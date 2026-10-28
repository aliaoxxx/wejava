package com.wejava.security;

import com.wejava.security.model.AuthUser;
import com.wejava.security.model.LoginSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 从 SecurityContext 取当前登录人。匿名访问时返回 null。
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static LoginSession currentSession() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof AuthUser authUser) {
            return authUser.getSession();
        }
        return null;
    }

    public static Long currentUserId() {
        LoginSession session = currentSession();
        return session == null ? null : session.getUserId();
    }

    public static String currentUsername() {
        LoginSession session = currentSession();
        return session == null ? null : session.getUsername();
    }
}
