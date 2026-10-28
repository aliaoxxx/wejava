package com.wejava.security.permission;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 方法级权限判断。注册为 "ps"，用法：
 * <pre>@PreAuthorize("@ps.has('system:user:add')")</pre>
 * <p>
 * 本类只负责「从上下文取 Authentication」，匹配规则全部委托给 PermissionMatcher。
 */
@Component("ps")
public class PermissionService {

    public boolean has(String perm) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (PermissionMatcher.matches(authority.getAuthority(), perm)) {
                return true;
            }
        }
        return false;
    }
}
