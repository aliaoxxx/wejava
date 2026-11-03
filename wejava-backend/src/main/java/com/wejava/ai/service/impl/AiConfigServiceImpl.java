package com.wejava.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wejava.ai.client.OpenAiCompatibleClient;
import com.wejava.ai.mapper.SysAiConfigMapper;
import com.wejava.ai.model.bo.AiChatConfig;
import com.wejava.ai.model.entity.SysAiConfig;
import com.wejava.ai.model.form.AiConfigForm;
import com.wejava.ai.model.vo.AiConfigVO;
import com.wejava.ai.model.vo.AiProviderVO;
import com.wejava.ai.provider.AiProvider;
import com.wejava.ai.service.AiConfigService;
import com.wejava.common.exception.BizException;
import com.wejava.common.exception.ErrorCode;
import com.wejava.common.util.AesUtils;
import com.wejava.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AiConfigServiceImpl implements AiConfigService {

    private static final BigDecimal DEFAULT_TEMPERATURE = new BigDecimal("0.70");
    private static final int DEFAULT_MAX_TOKENS = 4096;

    private final SysAiConfigMapper configMapper;
    private final AesUtils aesUtils;
    private final OpenAiCompatibleClient client;

    @Override
    public List<AiConfigVO> list() {
        List<SysAiConfig> rows = configMapper.selectList(Wrappers.<SysAiConfig>lambdaQuery()
                .orderByDesc(SysAiConfig::getIsDefault)
                .orderByAsc(SysAiConfig::getSort)
                .orderByAsc(SysAiConfig::getAiConfigId));
        return rows.stream().map(this::toVO).toList();
    }

    @Override
    public AiConfigVO get(Long id) {
        return toVO(requireById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AiConfigForm form) {
        SysAiConfig entity = new SysAiConfig();
        applyForm(entity, form, true);
        entity.setUpdateBy(SecurityUtils.currentUsername());
        entity.setUpdateTime(LocalDateTime.now());

        // 第一条自动成为默认，否则聊天会没有可用端点
        boolean first = configMapper.selectCount(null) == 0;
        boolean makeDefault = first || Boolean.TRUE.equals(form.getIsDefault());
        entity.setIsDefault(makeDefault ? 1 : 0);

        configMapper.insert(entity);
        if (makeDefault) {
            clearDefault(entity.getAiConfigId());
        }
        return entity.getAiConfigId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AiConfigForm form) {
        SysAiConfig entity = requireById(id);
        // 不允许把唯一的默认端点取消掉，否则聊天无端点可用
        if (Integer.valueOf(1).equals(entity.getIsDefault()) && Boolean.FALSE.equals(form.getIsDefault())) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "默认端点不能取消，请先把其他端点设为默认");
        }
        applyForm(entity, form, false);
        entity.setUpdateBy(SecurityUtils.currentUsername());
        entity.setUpdateTime(LocalDateTime.now());
        if (Boolean.TRUE.equals(form.getIsDefault())) {
            entity.setIsDefault(1);
        }
        configMapper.updateById(entity);
        if (Boolean.TRUE.equals(form.getIsDefault())) {
            clearDefault(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysAiConfig entity = requireById(id);
        if (Integer.valueOf(1).equals(entity.getIsDefault())) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "默认端点不能删除，请先切换默认端点");
        }
        if (configMapper.selectCount(null) <= 1) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "至少要保留一个端点");
        }
        configMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        SysAiConfig entity = requireById(id);
        entity.setIsDefault(1);
        entity.setUpdateBy(SecurityUtils.currentUsername());
        entity.setUpdateTime(LocalDateTime.now());
        configMapper.updateById(entity);
        clearDefault(id);
    }

    @Override
    public List<AiProviderVO> providers() {
        return AiProvider.all().stream().map(AiProviderVO::of).toList();
    }

    @Override
    public List<String> listModels(Long id) {
        SysAiConfig entity = requireById(id);
        String apiKey = plainApiKey(entity);
        if (!StringUtils.hasText(apiKey)) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "该端点还没有配置 API Key");
        }
        return client.listModels(entity.getBaseUrl(), apiKey);
    }

    @Override
    public long test(Long id) {
        SysAiConfig entity = requireById(id);
        String apiKey = plainApiKey(entity);
        if (!StringUtils.hasText(apiKey)) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "该端点还没有配置 API Key");
        }
        return client.ping(entity.getBaseUrl(), apiKey, entity.getModel());
    }

    @Override
    public AiChatConfig resolve(Long configId) {
        SysAiConfig entity;
        if (configId != null) {
            entity = configMapper.selectById(configId);
        } else {
            entity = configMapper.selectOne(Wrappers.<SysAiConfig>lambdaQuery()
                    .eq(SysAiConfig::getIsDefault, 1)
                    .last("LIMIT 1"));
            if (entity == null) {
                // 兜底：没有标记默认时取第一条启用的
                entity = configMapper.selectOne(Wrappers.<SysAiConfig>lambdaQuery()
                        .eq(SysAiConfig::getEnabled, 1)
                        .orderByAsc(SysAiConfig::getAiConfigId)
                        .last("LIMIT 1"));
            }
        }
        if (entity == null || !Integer.valueOf(1).equals(entity.getEnabled())) {
            return null;
        }
        return new AiChatConfig(
                entity.getAiConfigId(),
                entity.getProvider(),
                entity.getConfigName(),
                entity.getModel(),
                entity.getBaseUrl(),
                plainApiKey(entity),
                entity.getTemperature() == null
                        ? DEFAULT_TEMPERATURE.doubleValue()
                        : entity.getTemperature().doubleValue(),
                entity.getMaxTokens() == null ? DEFAULT_MAX_TOKENS : entity.getMaxTokens());
    }

    /** 解密密钥，未配置时返回 null */
    private String plainApiKey(SysAiConfig entity) {
        String plain = aesUtils.decrypt(entity.getApiKey());
        return StringUtils.hasText(plain) ? plain : null;
    }

    private void applyForm(SysAiConfig entity, AiConfigForm form, boolean creating) {
        String provider =
                StringUtils.hasText(form.getProvider()) ? form.getProvider().trim() : "custom";
        entity.setProvider(provider);
        if (creating || StringUtils.hasText(form.getConfigName())) {
            entity.setConfigName(
                    StringUtils.hasText(form.getConfigName())
                            ? form.getConfigName().trim()
                            : defaultName(provider, form.getModel()));
        }
        entity.setProtocol(StringUtils.hasText(entity.getProtocol()) ? entity.getProtocol() : "openai");
        entity.setModel(form.getModel().trim());
        entity.setBaseUrl(normalizeBaseUrl(form.getBaseUrl()));
        entity.setTemperature(form.getTemperature() == null ? DEFAULT_TEMPERATURE : form.getTemperature());
        entity.setMaxTokens(form.getMaxTokens() == null ? DEFAULT_MAX_TOKENS : form.getMaxTokens());
        entity.setEnabled(form.getEnabled() == null ? 1 : form.getEnabled());
        entity.setRemark(form.getRemark());

        // 传空或掩码表示保留原密钥；传了明文则加密保存
        if (StringUtils.hasText(form.getApiKey()) && !form.getApiKey().contains("****")) {
            entity.setApiKey(aesUtils.encrypt(form.getApiKey().trim()));
        }
    }

    private String defaultName(String provider, String model) {
        String label = AiProvider.of(provider).map(AiProvider::getLabel).orElse(provider);
        return label + " " + model;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String url = baseUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * 把除 {@code keepId} 之外的端点取消默认标记。
     *
     * <p>项目里注册了 {@code BlockAttackInnerInterceptor}（禁止全表更新），
     * 批量 set 会被拦下，这里改成按主键逐条更新 —— 端点数量个位数，开销可忽略。
     */
    private void clearDefault(Long keepId) {
        List<SysAiConfig> others = configMapper.selectList(Wrappers.<SysAiConfig>lambdaQuery()
                .eq(SysAiConfig::getIsDefault, 1)
                .ne(SysAiConfig::getAiConfigId, keepId));
        for (SysAiConfig other : others) {
            other.setIsDefault(0);
            configMapper.updateById(other);
        }
    }

    private SysAiConfig requireById(Long id) {
        SysAiConfig entity = configMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "AI 端点不存在");
        }
        return entity;
    }

    private AiConfigVO toVO(SysAiConfig entity) {
        AiConfigVO vo = new AiConfigVO();
        vo.setAiConfigId(entity.getAiConfigId());
        vo.setConfigName(entity.getConfigName());
        vo.setProvider(entity.getProvider());
        vo.setProviderLabel(
                AiProvider.of(entity.getProvider()).map(AiProvider::getLabel).orElse(entity.getProvider()));
        vo.setProtocol(entity.getProtocol());
        vo.setModel(entity.getModel());
        vo.setBaseUrl(entity.getBaseUrl());
        String plain = plainApiKey(entity);
        vo.setApiKeyMasked(plain == null ? null : aesUtils.mask(plain));
        vo.setApiKeyConfigured(plain != null);
        vo.setTemperature(entity.getTemperature());
        vo.setMaxTokens(entity.getMaxTokens());
        vo.setEnabled(entity.getEnabled());
        vo.setIsDefault(Integer.valueOf(1).equals(entity.getIsDefault()));
        vo.setSort(entity.getSort());
        vo.setRemark(entity.getRemark());
        vo.setUpdateBy(entity.getUpdateBy());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
