package com.wejava.gen.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 代码生成-字段配置 */
@Data
@TableName("gen_table_column")
public class GenTableColumn {

    @TableId(value = "column_id", type = IdType.AUTO)
    private Long columnId;

    private Long tableId;

    private String columnName;

    private String columnType;

    private String columnComment;

    private String javaType;

    private String javaField;

    private Integer isPk;

    private Integer isIncrement;

    private Integer isRequired;

    private Integer isInsert;

    private Integer isEdit;

    private Integer isList;

    private Integer isQuery;

    private String queryType;

    private String htmlType;

    private String dictType;

    private Integer sort;
}
