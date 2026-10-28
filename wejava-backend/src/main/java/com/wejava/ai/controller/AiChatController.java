package com.wejava.ai.controller;

import com.wejava.ai.model.form.AiChatForm;
import com.wejava.ai.model.vo.AiMessageVO;
import com.wejava.ai.service.AiChatService;
import com.wejava.common.annotation.RateLimit;
import com.wejava.common.core.R;
import com.wejava.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话。POST + SSE：请求体带提问，响应是 text/event-stream。
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @RateLimit(count = 10, seconds = 60, message = "AI 对话过于频繁，请稍后再试")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody @Valid AiChatForm form) {
        return aiChatService.stream(SecurityUtils.currentUserId(), form);
    }

    @GetMapping("/session/{sessionId}/messages")
    public R<List<AiMessageVO>> history(@PathVariable Long sessionId) {
        return R.ok(aiChatService.history(SecurityUtils.currentUserId(), sessionId));
    }
}
