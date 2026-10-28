package com.wejava.system.controller;

import com.wejava.common.annotation.BusinessType;
import com.wejava.common.annotation.OperLog;
import com.wejava.common.core.PageResult;
import com.wejava.common.core.R;
import com.wejava.system.model.form.SysConfigForm;
import com.wejava.system.model.query.SysConfigQuery;
import com.wejava.system.model.vo.SysConfigVO;
import com.wejava.system.service.SysConfigService;
import jakarta.validation.Valid;
import java.util.List;
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

@RestController
@RequestMapping("/system/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService configService;

    @PreAuthorize("@ps.has('system:config:list')")
    @GetMapping("/list")
    public R<PageResult<SysConfigVO>> list(SysConfigQuery query) {
        return R.ok(configService.page(query));
    }

    @PreAuthorize("@ps.has('system:config:query')")
    @GetMapping("/{configId}")
    public R<SysConfigVO> getById(@PathVariable Long configId) {
        return R.ok(configService.getById(configId));
    }

    @OperLog(title = "参数设置", businessType = BusinessType.INSERT)
    @PreAuthorize("@ps.has('system:config:add')")
    @PostMapping
    public R<Long> create(@RequestBody @Valid SysConfigForm form) {
        return R.ok(configService.create(form));
    }

    @OperLog(title = "参数设置", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ps.has('system:config:edit')")
    @PutMapping
    public R<Void> update(@RequestBody @Valid SysConfigForm form) {
        configService.update(form);
        return R.ok();
    }

    @OperLog(title = "参数设置", businessType = BusinessType.DELETE)
    @PreAuthorize("@ps.has('system:config:remove')")
    @DeleteMapping("/{configIds}")
    public R<Void> remove(@PathVariable List<Long> configIds) {
        configService.remove(configIds);
        return R.ok();
    }

    /** 按键名取参数值 */
    @GetMapping("/key/{configKey}")
    public R<String> getByKey(@PathVariable String configKey) {
        return R.ok(configService.getValueByKey(configKey));
    }
}
