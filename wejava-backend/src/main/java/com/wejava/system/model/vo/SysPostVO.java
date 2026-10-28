package com.wejava.system.model.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SysPostVO {

    private Long postId;
    private String postCode;
    private String postName;
    private Integer postSort;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}
