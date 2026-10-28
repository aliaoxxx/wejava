package com.wejava.system.model.query;

import com.wejava.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysPostQuery extends PageQuery {

    private String postCode;

    private String postName;

    private Integer status;
}
