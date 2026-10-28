package com.wejava.system.model.query;

import com.wejava.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysRoleQuery extends PageQuery {

    private String roleName;

    private String roleKey;

    private Integer status;
}
