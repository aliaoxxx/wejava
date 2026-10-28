package com.wejava.system.model.query;

import lombok.Data;

/** 部门树查询，不分页 */
@Data
public class SysDeptQuery {

    private String deptName;

    private Integer status;
}
