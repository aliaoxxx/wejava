package com.wejava.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wejava.ai.client.OpenAiCompatibleClient;
import com.wejava.ai.client.SysAiMessageView;
import com.wejava.ai.mapper.SysAiMessageMapper;
import com.wejava.ai.mapper.SysAiSessionMapper;
import com.wejava.ai.model.bo.AiChatConfig;
import com.wejava.ai.model.entity.SysAiMessage;
import com.wejava.ai.model.entity.SysAiSession;
import com.wejava.ai.model.form.AiChatForm;
import com.wejava.ai.model.vo.AiMessageVO;
import com.wejava.ai.service.AiChatService;
import com.wejava.ai.service.AiConfigService;
import com.wejava.common.config.AsyncConfig;
import com.wejava.common.exception.BizException;
import com.wejava.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    private static final int MAX_HISTORY = 20;
    private static final long SSE_TIMEOUT_MS = 5 * 60 * 1000L;

    private final SysAiSessionMapper sessionMapper;
    private final SysAiMessageMapper messageMapper;
    private final AiConfigService aiConfigService;
    private final OpenAiCompatibleClient client;
    private final ExecutorService aiExecutor;

    @Value("${DEEPSEEK_API_KEY:}")
    private String apiKeyFromEnv;

    public AiChatServiceImpl(
            SysAiSessionMapper sessionMapper,
            SysAiMessageMapper messageMapper,
            AiConfigService aiConfigService,
            OpenAiCompatibleClient client,
            @Qualifier(AsyncConfig.AI_EXECUTOR) ExecutorService aiExecutor) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.aiConfigService = aiConfigService;
        this.client = client;
        this.aiExecutor = aiExecutor;
    }

    @Override
    public SseEmitter stream(Long userId, AiChatForm form) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        emitter.onCompletion(() -> cancelled.set(true));
        emitter.onTimeout(() -> cancelled.set(true));
        emitter.onError(e -> cancelled.set(true));

        aiExecutor.execute(() -> doStream(userId, form, emitter, cancelled));
        return emitter;
    }

    private void doStream(Long userId, AiChatForm form, SseEmitter emitter, AtomicBoolean cancelled) {
        AiChatConfig config = aiConfigService.resolve(form.getConfigId());
        if (config == null) {
            sendError(emitter, "没有可用的 AI 端点，请在「AI 配置」中新增并启用一个端点");
            return;
        }

        String apiKey = config.apiKey();
        if (apiKey == null) {
            // 兼容老部署：允许用环境变量兜底
            apiKey = apiKeyFromEnv != null && !apiKeyFromEnv.isBlank() ? apiKeyFromEnv.trim() : null;
        }
        if (apiKey == null) {
            sendError(emitter, "该端点还没有配置 API Key，请在「AI 配置」中填写");
            return;
        }

        SysAiSession session = ensureSession(userId, form, config.model());
        saveMessage(session.getSessionId(), "user", form.getContent(), null, null, null, config.id(), config.model());

        List<SysAiMessageView> history = loadHistory(session.getSessionId());
        StringBuilder answer = new StringBuilder();
        long start = System.currentTimeMillis();
        int[] usage = {0, 0};

        try {
            client.stream(
                    config.baseUrl(),
                    apiKey,
                    config.model(),
                    history.stream()
                            .map(item -> Map.of("role", item.role(), "content", item.content()))
                            .toList(),
                    delta -> {
                        answer.append(delta);
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("delta")
                                    .data(Map.of("text", delta), org.springframework.http.MediaType.APPLICATION_JSON));
                        } catch (Exception e) {
                            cancelled.set(true);
                        }
                    },
                    cancelled::get,
                    u -> {
                        usage[0] = u.promptTokens();
                        usage[1] = u.completionTokens();
                    });

            long cost = System.currentTimeMillis() - start;
            saveMessage(
                    session.getSessionId(),
                    "assistant",
                    answer.toString(),
                    usage[0],
                    usage[1],
                    cost,
                    config.id(),
                    config.model());

            emitter.send(SseEmitter.event()
                    .name("done")
                    .data(
                            Map.of(
                                    "sessionId",
                                    String.valueOf(session.getSessionId()),
                                    "promptTokens",
                                    usage[0],
                                    "completionTokens",
                                    usage[1],
                                    "durationMs",
                                    cost),
                            org.springframework.http.MediaType.APPLICATION_JSON));
            emitter.complete();
        } catch (Exception e) {
            log.warn("AI 对话失败: {}", e.getMessage());
            sendError(emitter, e.getMessage());
        }
    }

    @Override
    public List<AiMessageVO> history(Long userId, Long sessionId) {
        SysAiSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return messageMapper
                .selectList(Wrappers.<SysAiMessage>lambdaQuery()
                        .eq(SysAiMessage::getSessionId, sessionId)
                        .orderByAsc(SysAiMessage::getMessageId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    private SysAiSession ensureSession(Long userId, AiChatForm form, String model) {
        if (form.getSessionId() != null) {
            SysAiSession exist = sessionMapper.selectById(form.getSessionId());
            if (exist == null || !exist.getUserId().equals(userId)) {
                throw new BizException(ErrorCode.DATA_NOT_FOUND);
            }
            exist.setUpdateTime(LocalDateTime.now());
            sessionMapper.updateById(exist);
            return exist;
        }
        SysAiSession session = new SysAiSession();
        session.setUserId(userId);
        session.setTitle(form.getContent().length() > 30 ? form.getContent().substring(0, 30) : form.getContent());
        session.setModel(model);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.insert(session);
        return session;
    }

    private List<SysAiMessageView> loadHistory(Long sessionId) {
        List<SysAiMessage> messages = messageMapper.selectList(Wrappers.<SysAiMessage>lambdaQuery()
                .eq(SysAiMessage::getSessionId, sessionId)
                .orderByDesc(SysAiMessage::getMessageId)
                .last("LIMIT " + MAX_HISTORY));
        List<SysAiMessageView> list = new ArrayList<>();
        for (int i = messages.size() - 1; i >= 0; i--) {
            SysAiMessage item = messages.get(i);
            list.add(new SysAiMessageView(item.getRole(), item.getContent()));
        }
        return list;
    }

    private void saveMessage(
            Long sessionId,
            String role,
            String content,
            Integer promptTokens,
            Integer completionTokens,
            Long durationMs,
            Long configId,
            String model) {
        SysAiMessage message = new SysAiMessage();
        message.setConfigId(configId);
        message.setModel(model);
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setPromptTokens(promptTokens);
        message.setCompletionTokens(completionTokens);
        message.setDurationMs(durationMs);
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);
    }

    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(
                            Map.of("msg", message == null ? "对话失败" : message),
                            org.springframework.http.MediaType.APPLICATION_JSON));
        } catch (Exception ignored) {
            // 连接已断开，忽略
        }
        emitter.complete();
    }

    private AiMessageVO toVO(SysAiMessage entity) {
        AiMessageVO vo = new AiMessageVO();
        vo.setMessageId(entity.getMessageId());
        vo.setSessionId(entity.getSessionId());
        vo.setRole(entity.getRole());
        vo.setContent(entity.getContent());
        vo.setPromptTokens(entity.getPromptTokens());
        vo.setCompletionTokens(entity.getCompletionTokens());
        vo.setDurationMs(entity.getDurationMs());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
