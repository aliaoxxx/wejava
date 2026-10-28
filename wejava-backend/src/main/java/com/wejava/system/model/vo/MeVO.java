package com.wejava.system.model.vo;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MeVO {

    private Long userId;
    private String username;
    private String nickname;
    private Long deptId;
    private boolean superAdmin;
    private Set<String> roles;
    private Set<String> perms;
}
