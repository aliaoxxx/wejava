package com.wejava.security.model;

import com.wejava.security.permission.AuthorityResolver;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security 的 principal。只在请求线程内存在，不落 Redis、不参与序列化。
 * <p>
 * 字段说明：
 * - session：会话快照
 * - password：仅登录认证阶段有值，认证完成后不再使用
 * - status：账号状态，用于 isEnabled()
 */
public class AuthUser implements UserDetails {

    private final transient LoginSession session;
    private final transient String password;
    private final transient Integer status;

    /** 从 Redis 会话还原时使用：此时不需要密码，账号状态视为正常 */
    public AuthUser(LoginSession session) {
        this(session, null, 0);
    }

    /** 登录认证时使用 */
    public AuthUser(LoginSession session, String password, Integer status) {
        this.session = session;
        this.password = password;
        this.status = status;
    }

    public LoginSession getSession() {
        return session;
    }

    public Integer getStatus() {
        return status;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return AuthorityResolver.resolve(session);
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return session == null ? null : session.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == null || status == 0;
    }
}
