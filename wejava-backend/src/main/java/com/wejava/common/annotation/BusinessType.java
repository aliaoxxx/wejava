package com.wejava.common.annotation;

import lombok.Getter;

/**
 * 操作类型，与 sys_oper_log.business_type 对应。
 */
@Getter
public enum BusinessType {
    OTHER(0, "其它"),
    INSERT(1, "新增"),
    UPDATE(2, "修改"),
    DELETE(3, "删除"),
    EXPORT(4, "导出"),
    IMPORT(5, "导入");

    private final int code;
    private final String label;

    BusinessType(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
