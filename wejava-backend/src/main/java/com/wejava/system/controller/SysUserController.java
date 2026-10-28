package com.wejava.system.controller;

import com.wejava.common.annotation.BusinessType;
import com.wejava.common.annotation.OperLog;
import com.wejava.common.core.PageResult;
import com.wejava.common.core.R;
import com.wejava.common.util.CsvUtils;
import com.wejava.system.model.form.ChangeStatusForm;
import com.wejava.system.model.form.ResetPasswordForm;
import com.wejava.system.model.form.SysUserCreateForm;
import com.wejava.system.model.form.SysUserUpdateForm;
import com.wejava.system.model.query.SysUserQuery;
import com.wejava.system.model.vo.SysUserVO;
import com.wejava.system.service.SysUserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
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
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @PreAuthorize("@ps.has('system:user:list')")
    @GetMapping("/list")
    public R<PageResult<SysUserVO>> list(SysUserQuery query) {
        return R.ok(userService.page(query));
    }

    @PreAuthorize("@ps.has('system:user:query')")
    @GetMapping("/{userId}")
    public R<SysUserVO> getById(@PathVariable Long userId) {
        return R.ok(userService.getById(userId));
    }

    @OperLog(title = "用户管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ps.has('system:user:add')")
    @PostMapping
    public R<Long> create(@RequestBody @Valid SysUserCreateForm form) {
        return R.ok(userService.create(form));
    }

    @OperLog(title = "用户管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ps.has('system:user:edit')")
    @PutMapping
    public R<Void> update(@RequestBody @Valid SysUserUpdateForm form) {
        userService.update(form);
        return R.ok();
    }

    @OperLog(title = "用户管理", businessType = BusinessType.DELETE)
    @PreAuthorize("@ps.has('system:user:remove')")
    @DeleteMapping("/{userIds}")
    public R<Void> remove(@PathVariable List<Long> userIds) {
        userService.remove(userIds);
        return R.ok();
    }

    @PreAuthorize("@ps.has('system:user:resetPwd')")
    @PutMapping("/reset-pwd")
    public R<Void> resetPassword(@RequestBody @Valid ResetPasswordForm form) {
        userService.resetPassword(form.getUserId(), form.getPassword());
        return R.ok();
    }

    @PreAuthorize("@ps.has('system:user:export')")
    @OperLog(title = "用户管理", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public void export(SysUserQuery query, HttpServletResponse response) throws IOException {
        List<SysUserVO> users = userService.exportList(query);
        List<List<String>> rows = new java.util.ArrayList<>();
        for (SysUserVO user : users) {
            rows.add(java.util.Arrays.asList(
                    String.valueOf(user.getUserId()),
                    user.getUsername(),
                    user.getNickname(),
                    user.getDeptName(),
                    user.getPhone(),
                    user.getStatus() != null && user.getStatus() == 0 ? "正常" : "停用",
                    user.getCreateTime() == null
                            ? ""
                            : user.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        }
        CsvUtils.write(
                response, "用户列表.csv", java.util.Arrays.asList("用户ID", "用户名", "昵称", "部门", "手机号", "状态", "创建时间"), rows);
    }

    @PreAuthorize("@ps.has('system:user:edit')")
    @PutMapping("/change-status")
    public R<Void> changeStatus(@RequestBody @Valid ChangeStatusForm form) {
        userService.changeStatus(form.getId(), form.getStatus());
        return R.ok();
    }
}
