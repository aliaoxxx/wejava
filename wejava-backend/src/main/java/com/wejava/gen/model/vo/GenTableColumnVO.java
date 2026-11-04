package com.wejava.gen.model.vo;

import lombok.Data;

/** 字段配置（前端编辑用） */
@Data
public class GenTableColumnVO {

    private Long columnId;
    private Long tableId;
    private String columnName;
    private String columnType;
    private String columnComment;
    private String javaType;
    private String javaField;
    private Boolean isPk;
    private Boolean isIncrement;
    private Boolean isRequired;
    private Boolean isInsert;
    private Boolean isEdit;
    private Boolean isList;
    private Boolean isQuery;
    private String queryType;
    private String htmlType;
    private String dictType;
    private Integer sort;
}
