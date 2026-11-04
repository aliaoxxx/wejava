package com.wejava.gen.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 字段配置提交 */
@Data
public class GenTableColumnForm {

    private Long columnId;

    @NotBlank(message = "列名不能为空")
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

    @Size(max = 20, message = "查询方式最长 20")
    private String queryType;

    @Size(max = 20, message = "控件类型最长 20")
    private String htmlType;

    @Size(max = 100, message = "字典编码最长 100")
    private String dictType;

    private Integer sort;
}
