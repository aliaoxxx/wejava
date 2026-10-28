package com.wejava.system.service;

import com.wejava.security.model.LoginSession;
import com.wejava.system.model.form.SysMenuForm;
import com.wejava.system.model.query.SysMenuQuery;
import com.wejava.system.model.vo.RoleMenuTreeVO;
import com.wejava.system.model.vo.RouterVO;
import com.wejava.system.model.vo.SysMenuVO;
import java.util.List;

/**
 * 菜单管理与动态路由。
 */
public interface SysMenuService {

    /** 当前用户可见的前端路由树 */
    List<RouterVO> routers(LoginSession session);

    /** 管理页用的完整菜单树 */
    List<SysMenuVO> tree(SysMenuQuery query);

    SysMenuVO getById(Long menuId);

    Long create(SysMenuForm form);

    void update(SysMenuForm form);

    void remove(Long menuId);

    /** 上级菜单选择器：只含目录与菜单，不含按钮 */
    List<SysMenuVO> treeSelect();

    /** 角色分配菜单：完整树 + 已选中ID */
    RoleMenuTreeVO roleMenuTree(Long roleId);
}
