package com.wejava.system.model.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色分配菜单时用：完整菜单树 + 已选中的菜单ID。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleMenuTreeVO {

    private List<SysMenuVO> menus;

    private List<Long> checkedKeys;
}
