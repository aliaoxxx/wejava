package com.wejava.system.model.query;

import com.wejava.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysConfigQuery extends PageQuery {

    private String configName;

    private String configKey;

    private Integer configType;
}
