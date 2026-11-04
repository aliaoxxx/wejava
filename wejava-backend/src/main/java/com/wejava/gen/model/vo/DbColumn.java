package com.wejava.gen.model.vo;

import lombok.Data;

/** information_schema.columns 的投影，仅用于导入时推断 */
@Data
public class DbColumn {

    private String columnName;

    private String columnType;

    private String columnComment;

    private String isNullable;

    private String columnKey;

    private String extra;

    private String columnDefault;
}
