package com.wejava.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wejava.ai.mapper.SysAiConfigMapper;
import com.wejava.ai.model.entity.SysAiConfig;
import com.wejava.ai.model.form.AiConfigForm;
import com.wejava.ai.model.vo.AiConfigVO;
import com.wejava.ai.service.AiConfigService;
import com.wejava.common.util.AesUtils;
import com.wejava.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AiConfigServiceImpl implements AiConfigService {

    private final SysAiConfigMapper configMapper;
    private final AesUtils aesUtils;

    @Override
    public AiConfigVO get() {
        SysAiConfig entity = current();
        AiConfigVO vo = new AiConfigVO();
        if (entity == null) {
            return vo;
        }
        String plain = aesUtils.decrypt(entity.getApiKey());
        vo.setAiConfigId(entity.getAiConfigId());
        vo.setProvider(entity.getProvider());
        vo.setModel(entity.getModel());
        vo.setBaseUrl(entity.getBaseUrl());
        vo.setApiKeyMasked(aesUtils.mask(plain));
        vo.setApiKeyConfigured(StringUtils.hasText(plain));
        vo.setTemperature(entity.getTemperature());
        vo.setMaxTokens(entity.getMaxTokens());
        vo.setEnabled(entity.getEnabled());
        vo.setUpdateBy(entity.getUpdateBy());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(AiConfigForm form) {
        SysAiConfig entity = current();
        boolean create = entity == null;
        if (create) {
            entity = new SysAiConfig();
            entity.setProvider("deepseek");
        }

        entity.setModel(form.getModel());
        entity.setBaseUrl(form.getBaseUrl());
        entity.setTemperature(form.getTemperature() == null ? new BigDecimal("0.70") : form.getTemperature());
        entity.setMaxTokens(form.getMaxTokens() == null ? 4096 : form.getMaxTokens());
        entity.setEnabled(form.getEnabled() == null ? 1 : form.getEnabled());
        entity.setUpdateBy(SecurityUtils.currentUsername());
        entity.setUpdateTime(LocalDateTime.now());

        // 传空或不传表示保留原密钥；传了明文则加密保存
        if (StringUtils.hasText(form.getApiKey()) && !form.getApiKey().contains("****")) {
            entity.setApiKey(aesUtils.encrypt(form.getApiKey().trim()));
        }

        if (create) {
            configMapper.insert(entity);
        } else {
            configMapper.updateById(entity);
        }
    }

    @Override
    public String plainApiKey() {
        SysAiConfig entity = current();
        if (entity == null) {
            return null;
        }
        String plain = aesUtils.decrypt(entity.getApiKey());
        return StringUtils.hasText(plain) ? plain : null;
    }

    @Override
    public String model() {
        SysAiConfig entity = current();
        return entity == null ? "deepseek-chat" : entity.getModel();
    }

    @Override
    public String baseUrl() {
        SysAiConfig entity = current();
        return entity == null ? "https://api.deepseek.com" : entity.getBaseUrl();
    }

    @Override
    public Integer maxTokens() {
        SysAiConfig entity = current();
        return entity == null ? 4096 : entity.getMaxTokens();
    }

    @Override
    public double temperature() {
        SysAiConfig entity = current();
        return entity == null ? 0.7 : entity.getTemperature().doubleValue();
    }

    @Override
    public boolean enabled() {
        SysAiConfig entity = current();
        return entity == null || Integer.valueOf(1).equals(entity.getEnabled());
    }

    private SysAiConfig current() {
        return configMapper.selectOne(Wrappers.<SysAiConfig>lambdaQuery().last("LIMIT 1"));
    }
}
