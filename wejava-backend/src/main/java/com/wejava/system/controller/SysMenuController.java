package com.wejava.system.controller;

import com.wejava.common.annotation.BusinessType;
import com.wejava.common.annotation.OperLog;
import com.wejava.common.core.R;
import com.wejava.system.model.form.SysMenuForm;
import com.wejava.system.model.query.SysMenuQuery;
import com.wejava.system.model.vo.RoleMenuTreeVO;
import com.wejava.system.model.vo.SysMenuVO;
import com.wejava.system.service.SysMenuService;
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
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    @PreAuthorize("@ps.has('system:menu:list')")
    @GetMapping("/list")
    public R<List<SysMenuVO>> list(SysMenuQuery query) {
        return R.ok(menuService.tree(query));
    }

    @PreAuthorize("@ps.has('system:menu:query')")
    @GetMapping("/{menuId}")
    public R<SysMenuVO> getById(@PathVariable Long menuId) {
        return R.ok(menuService.getById(menuId));
    }

    @OperLog(title = "菜单管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ps.has('system:menu:add')")
    @PostMapping
    public R<Long> create(@RequestBody @Valid SysMenuForm form) {
        return R.ok(menuService.create(form));
    }

    @OperLog(title = "菜单管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ps.has('system:menu:edit')")
    @PutMapping
    public R<Void> update(@RequestBody @Valid SysMenuForm form) {
        menuService.update(form);
        return R.ok();
    }

    @OperLog(title = "菜单管理", businessType = BusinessType.DELETE)
    @PreAuthorize("@ps.has('system:menu:remove')")
    @DeleteMapping("/{menuId}")
    public R<Void> remove(@PathVariable Long menuId) {
        menuService.remove(menuId);
        return R.ok();
    }

    /** 上级菜单选择器：登录即可 */
    @GetMapping("/tree-select")
    public R<List<SysMenuVO>> treeSelect() {
        return R.ok(menuService.treeSelect());
    }

    /** 角色分配菜单用的树：登录即可（角色页面会用） */
    @PreAuthorize("@ps.has('system:role:query')")
    @GetMapping("/role-tree/{roleId}")
    public R<RoleMenuTreeVO> roleTree(@PathVariable Long roleId) {
        return R.ok(menuService.roleMenuTree(roleId));
    }
}
