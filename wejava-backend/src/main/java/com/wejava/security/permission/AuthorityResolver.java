package com.wejava.security.permission;

import com.wejava.security.model.LoginSession;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * 把会话解析成 Spring Security 的权限集合。
 * <p>
 * 本类只做一件事：{@link LoginSession} → {@code Collection<GrantedAuthority>}。
 * 超管通配、perms 映射等语义全部在这里，{@code AuthUser} 不再知道这些细节。
 */
public final class AuthorityResolver {

    private AuthorityResolver() {}

    public static Collection<GrantedAuthority> resolve(LoginSession session) {
        if (session == null) {
            return List.of();
        }
        if (session.isSuperAdmin()) {
            return List.of(new SimpleGrantedAuthority(PermissionMatcher.ALL));
        }
        return session.getPerms().stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
