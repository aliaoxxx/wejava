package com.wejava.ai.controller;

import com.wejava.ai.model.form.AiConfigForm;
import com.wejava.ai.model.vo.AiConfigVO;
import com.wejava.ai.model.vo.AiProviderVO;
import com.wejava.ai.service.AiConfigService;
import com.wejava.common.annotation.BusinessType;
import com.wejava.common.annotation.OperLog;
import com.wejava.common.annotation.RateLimit;
import com.wejava.common.core.R;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 端点管理。密钥属于敏感数据，仅授权管理员可读，且只返回掩码。
 *
 * <p>模型列表与连通性测试都由服务端发起，密钥不会下发到浏览器。
 */
@RestController
@RequestMapping("/system/ai-config")
@RequiredArgsConstructor
public class AiConfigController {

    private final AiConfigService configService;

    @PreAuthorize("@ps.has('system:ai:config')")
    @GetMapping("/list")
    public R<List<AiConfigVO>> list() {
        return R.ok(configService.list());
    }

    @PreAuthorize("@ps.has('system:ai:config')")
    @GetMapping("/providers")
    public R<List<AiProviderVO>> providers() {
        return R.ok(configService.providers());
    }

    @PreAuthorize("@ps.has('system:ai:config')")
    @GetMapping("/{id}")
    public R<AiConfigVO> get(@PathVariable Long id) {
        return R.ok(configService.get(id));
    }

    @PreAuthorize("@ps.has('system:ai:config')")
    @OperLog(title = "AI 端点", businessType = BusinessType.INSERT, saveParam = false)
    @PostMapping
    public R<Long> create(@RequestBody @Valid AiConfigForm form) {
        return R.ok(configService.create(form));
    }

    @PreAuthorize("@ps.has('system:ai:config')")
    @OperLog(title = "AI 端点", businessType = BusinessType.UPDATE, saveParam = false)
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid AiConfigForm form) {
        configService.update(id, form);
        return R.ok();
    }

    @PreAuthorize("@ps.has('system:ai:config')")
    @OperLog(title = "AI 端点", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        configService.delete(id);
        return R.ok();
    }

    @PreAuthorize("@ps.has('system:ai:config')")
    @OperLog(title = "AI 端点", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/default")
    public R<Void> setDefault(@PathVariable Long id) {
        configService.setDefault(id);
        return R.ok();
    }

    @PreAuthorize("@ps.has('system:ai:config')")
    @RateLimit(count = 5, seconds = 60)
    @GetMapping("/{id}/models")
    public R<List<String>> models(@PathVariable Long id) {
        return R.ok(configService.listModels(id));
    }

    @PreAuthorize("@ps.has('system:ai:config')")
    @RateLimit(count = 5, seconds = 60)
    @PostMapping("/{id}/test")
    public R<Map<String, Object>> test(@PathVariable Long id) {
        long cost = configService.test(id);
        return R.ok(Map.of("ok", true, "costMs", cost));
    }
}
