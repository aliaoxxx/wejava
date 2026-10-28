package com.wejava.system.model.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class SysDeptVO {

    private Long deptId;
    private Long parentId;
    private String ancestors;
    private String deptName;
    private Integer orderNum;
    private String leader;
    private String phone;
    private String email;
    private Integer status;
    private LocalDateTime createTime;
    private List<SysDeptVO> children;
}
