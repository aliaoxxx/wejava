package com.wejava.system.controller;

import com.wejava.common.annotation.BusinessType;
import com.wejava.common.annotation.OperLog;
import com.wejava.common.core.PageResult;
import com.wejava.common.core.R;
import com.wejava.system.model.form.ChangeStatusForm;
import com.wejava.system.model.form.DataScopeForm;
import com.wejava.system.model.form.SysRoleCreateForm;
import com.wejava.system.model.form.SysRoleUpdateForm;
import com.wejava.system.model.query.SysRoleQuery;
import com.wejava.system.model.vo.RoleOptionVO;
import com.wejava.system.model.vo.SysRoleVO;
import com.wejava.system.service.SysRoleService;
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
@RequestMapping("/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    /** 下拉选项：登录即可访问 */
    @GetMapping("/option-select")
    public R<List<RoleOptionVO>> optionSelect() {
        return R.ok(roleService.optionSelect());
    }

    @PreAuthorize("@ps.has('system:role:list')")
    @GetMapping("/list")
    public R<PageResult<SysRoleVO>> list(SysRoleQuery query) {
        return R.ok(roleService.page(query));
    }

    @PreAuthorize("@ps.has('system:role:query')")
    @GetMapping("/{roleId}")
    public R<SysRoleVO> getById(@PathVariable Long roleId) {
        return R.ok(roleService.getById(roleId));
    }

    @OperLog(title = "角色管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ps.has('system:role:add')")
    @PostMapping
    public R<Long> create(@RequestBody @Valid SysRoleCreateForm form) {
        return R.ok(roleService.create(form));
    }

    @OperLog(title = "角色管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ps.has('system:role:edit')")
    @PutMapping
    public R<Void> update(@RequestBody @Valid SysRoleUpdateForm form) {
        roleService.update(form);
        return R.ok();
    }

    @OperLog(title = "角色管理", businessType = BusinessType.DELETE)
    @PreAuthorize("@ps.has('system:role:remove')")
    @DeleteMapping("/{roleIds}")
    public R<Void> remove(@PathVariable List<Long> roleIds) {
        roleService.remove(roleIds);
        return R.ok();
    }

    @PreAuthorize("@ps.has('system:role:edit')")
    @PutMapping("/change-status")
    public R<Void> changeStatus(@RequestBody @Valid ChangeStatusForm form) {
        roleService.changeStatus(form.getId(), form.getStatus());
        return R.ok();
    }

    @PreAuthorize("@ps.has('system:role:query')")
    @GetMapping("/{roleId}/dept-ids")
    public R<List<Long>> deptIds(@PathVariable Long roleId) {
        return R.ok(roleService.listDeptIds(roleId));
    }

    @PreAuthorize("@ps.has('system:role:dataScope')")
    @PutMapping("/data-scope")
    public R<Void> dataScope(@RequestBody @Valid DataScopeForm form) {
        roleService.updateDataScope(form);
        return R.ok();
    }
}
