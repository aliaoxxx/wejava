package com.wejava.system.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 菜单新增/修改。menuId 为空表示新增。
 */
@Data
public class SysMenuForm {

    private Long menuId;

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称最长 50")
    private String menuName;

    private Long parentId;

    private Integer orderNum;

    @Size(max = 200, message = "路由地址最长 200")
    private String path;

    @Size(max = 255, message = "组件路径最长 255")
    private String component;

    private String query;

    private Integer isFrame;

    private Integer isCache;

    @NotBlank(message = "菜单类型不能为空")
    @Pattern(regexp = "^[MCF]$", message = "菜单类型只能是 M / C / F")
    private String menuType;

    private Integer visible;

    private Integer status;

    @Size(max = 100, message = "权限字符最长 100")
    private String perms;

    private String icon;
}
