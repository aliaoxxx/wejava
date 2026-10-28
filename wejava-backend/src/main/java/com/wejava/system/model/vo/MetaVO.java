package com.wejava.system.model.vo;

import lombok.Data;

/**
 * 前端路由 meta。
 */
@Data
public class MetaVO {

    private String title;

    private String icon;

    private Boolean hidden;

    private Boolean cache;
}
