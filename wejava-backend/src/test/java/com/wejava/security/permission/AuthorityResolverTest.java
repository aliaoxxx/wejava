package com.wejava.security.permission;

import static org.assertj.core.api.Assertions.assertThat;

import com.wejava.security.model.LoginSession;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

/**
 * 会话 → 权限集合 的转换规则。
 */
class AuthorityResolverTest {

    @Test
    void superAdminResolvesToAllPermission() {
        LoginSession session = new LoginSession();
        session.setSuperAdmin(true);

        assertThat(AuthorityResolver.resolve(session))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(PermissionMatcher.ALL);
    }

    @Test
    void superAdminIgnoresConfiguredPerms() {
        LoginSession session = new LoginSession();
        session.setSuperAdmin(true);
        session.setPerms(new LinkedHashSet<>(Set.of("system:user:list")));

        assertThat(AuthorityResolver.resolve(session))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(PermissionMatcher.ALL);
    }

    @Test
    void normalUserResolvesToOwnPerms() {
        LoginSession session = new LoginSession();
        session.setPerms(new LinkedHashSet<>(Set.of("system:user:list", "system:user:add")));

        assertThat(AuthorityResolver.resolve(session))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("system:user:list", "system:user:add");
    }

    @Test
    void nullSessionResolvesToEmpty() {
        assertThat(AuthorityResolver.resolve(null)).isEmpty();
    }
}
