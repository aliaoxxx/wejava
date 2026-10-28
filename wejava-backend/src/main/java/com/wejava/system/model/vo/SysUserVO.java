package com.wejava.system.model.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 用户出参。不包含 password / delFlag / updateBy 等内部字段。
 */
@Data
public class SysUserVO {

    private Long userId;
    private Long deptId;
    private String deptName;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String sex;
    private Integer status;
    private String remark;
    private List<Long> roleIds;
    private LocalDateTime createTime;
}
