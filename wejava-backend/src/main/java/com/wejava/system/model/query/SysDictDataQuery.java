package com.wejava.system.model.query;

import com.wejava.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysDictDataQuery extends PageQuery {

    private String dictLabel;

    private String dictType;

    private Integer status;
}
