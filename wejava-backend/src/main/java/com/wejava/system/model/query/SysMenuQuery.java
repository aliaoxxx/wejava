package com.wejava.system.model.query;

import lombok.Data;

/**
 * 菜单树查询条件。菜单量小，不分页。
 */
@Data
public class SysMenuQuery {

    private String menuName;

    private Integer status;
}
