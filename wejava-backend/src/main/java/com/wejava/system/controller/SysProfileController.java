package com.wejava.system.controller;

import com.wejava.common.annotation.BusinessType;
import com.wejava.common.annotation.OperLog;
import com.wejava.common.core.R;
import com.wejava.security.SecurityUtils;
import com.wejava.system.model.form.ProfilePasswordForm;
import com.wejava.system.model.form.ProfileUpdateForm;
import com.wejava.system.model.vo.SysUserVO;
import com.wejava.system.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人中心。所有接口只作用于当前登录用户，不需要额外权限点。
 */
@RestController
@RequestMapping("/system/profile")
@RequiredArgsConstructor
public class SysProfileController {

    private final SysUserService userService;

    @GetMapping
    public R<SysUserVO> detail() {
        return R.ok(userService.getById(SecurityUtils.currentUserId()));
    }

    @OperLog(title = "个人中心", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> update(@RequestBody @Valid ProfileUpdateForm form) {
        userService.updateOwnProfile(form);
        return R.ok();
    }

    @OperLog(title = "个人中心", businessType = BusinessType.UPDATE)
    @PutMapping("/password")
    public R<Void> changePassword(@RequestBody @Valid ProfilePasswordForm form) {
        userService.changeOwnPassword(form);
        return R.ok();
    }
}
