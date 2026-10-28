package com.wejava.security.model;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Data;

/**
 * 登录会话快照。纯数据对象，存 Redis。
 * <p>
 * 刻意不实现 UserDetails：会话数据的生命周期（存 Redis、7 天）与
 * Spring Security 的认证契约（只在请求线程内存在）是两件事，混在一起会导致
 * 序列化注解、transient 字段、authorities 计算全挤在一个类里。
 */
@Data
public class LoginSession implements Serializable {

    private Long userId;
    private Long deptId;
    private String username;
    private String nickname;

    /** 角色标识，如 admin */
    private Set<String> roles = new LinkedHashSet<>();

    /** 权限字符，如 system:user:add */
    private Set<String> perms = new LinkedHashSet<>();

    /** 超级管理员：拥有 *:*:* */
    private boolean superAdmin;

    /** 当前 access token 的 jti */
    private String jti;

    /** 对应的 refresh token 的 jti，登出时一并清理 */
    private String refreshJti;
}
