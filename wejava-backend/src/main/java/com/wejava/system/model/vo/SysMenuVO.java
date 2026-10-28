package com.wejava.system.model.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class SysMenuVO {

    private Long menuId;
    private Long parentId;
    private String menuName;
    private Integer orderNum;
    private String path;
    private String component;
    private String query;
    private Integer isFrame;
    private Integer isCache;
    private String menuType;
    private Integer visible;
    private Integer status;
    private String perms;
    private String icon;
    private LocalDateTime createTime;
    private List<SysMenuVO> children;
}
