package com.wejava.ai.controller;

import com.wejava.ai.model.form.AiConfigForm;
import com.wejava.ai.model.vo.AiConfigVO;
import com.wejava.ai.service.AiConfigService;
import com.wejava.common.annotation.BusinessType;
import com.wejava.common.annotation.OperLog;
import com.wejava.common.core.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 配置。密钥属于敏感数据，仅授权管理员可读，且只返回掩码。
 */
@RestController
@RequestMapping("/system/ai-config")
@RequiredArgsConstructor
public class AiConfigController {

    private final AiConfigService configService;

    @PreAuthorize("@ps.has('system:ai:config')")
    @GetMapping
    public R<AiConfigVO> get() {
        return R.ok(configService.get());
    }

    @PreAuthorize("@ps.has('system:ai:config')")
    @OperLog(title = "AI 配置", businessType = BusinessType.UPDATE, saveParam = false)
    @PutMapping
    public R<Void> update(@RequestBody @Valid AiConfigForm form) {
        configService.update(form);
        return R.ok();
    }
}
