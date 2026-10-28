package com.wejava.system.model.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class SysRoleVO {

    private Long roleId;
    private String roleName;
    private String roleKey;
    private Integer roleSort;
    private String dataScope;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private List<Long> menuIds;
}
