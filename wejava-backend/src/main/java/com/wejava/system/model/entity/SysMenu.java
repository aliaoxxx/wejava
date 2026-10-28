package com.wejava.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wejava.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    @TableId(value = "menu_id", type = IdType.ASSIGN_ID)
    private Long menuId;

    private String menuName;

    private Long parentId;

    private Integer orderNum;

    private String path;

    private String component;

    private String query;

    private Integer isFrame;

    private Integer isCache;

    /** M 目录 / C 菜单 / F 按钮 */
    private String menuType;

    private Integer visible;

    private Integer status;

    private String perms;

    private String icon;
}
