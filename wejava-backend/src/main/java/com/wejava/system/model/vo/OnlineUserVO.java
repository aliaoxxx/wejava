package com.wejava.system.model.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OnlineUserVO {

    private String jti;
    private Long userId;
    private String username;
    private String nickname;
    private Long deptId;
    private LocalDateTime lastActiveTime;
}
