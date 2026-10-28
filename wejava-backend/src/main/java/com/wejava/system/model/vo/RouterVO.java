package com.wejava.system.model.vo;

import java.util.List;
import lombok.Data;

/**
 * 前端动态路由节点。
 */
@Data
public class RouterVO {

    private String name;

    private String path;

    private String component;

    private String redirect;

    private MetaVO meta;

    private List<RouterVO> children;
}
