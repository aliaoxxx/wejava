package com.wejava.security.jwt;

import com.wejava.common.exception.ErrorCode;
import com.wejava.security.model.AuthUser;
import com.wejava.security.model.LoginSession;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 解析 Bearer Token 并填充 SecurityContext。
 * <p>
 * 关键点：
 * 1. 校验失败时只清空上下文，不抛异常 —— 抛异常会绕过 ExceptionTranslationFilter 变成 500
 * 2. finally 里清上下文 —— Tomcat 线程复用，不清会串号
 * 3. 区分"过期"与"无效" —— 前端据此决定静默刷新还是跳登录页
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR = "AUTH_ERROR";
    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;
    private final SessionStore sessionStore;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER)) {
            String token = header.substring(BEARER.length()).trim();
            try {
                Jwt jwt = jwtService.decode(token);
                if (!JwtService.TYPE_ACCESS.equals(jwt.getClaimAsString(JwtService.CLAIM_TYPE))) {
                    throw new JwtException("token 类型不是 access");
                }
                LoginSession session = sessionStore.getAccess(jwt.getId());
                if (session == null) {
                    // 已登出 / 被踢 / 会话过期
                    request.setAttribute(AUTH_ERROR, ErrorCode.NOT_LOGIN);
                } else {
                    AuthUser authUser = new AuthUser(session);
                    var authentication =
                            new UsernamePasswordAuthenticationToken(authUser, null, authUser.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    sessionStore.touch(jwt.getId());
                }
            } catch (JwtValidationException e) {
                boolean expired = e.getErrors().stream()
                        .anyMatch(err -> err.getDescription() != null
                                && err.getDescription().toLowerCase().contains("expired"));
                request.setAttribute(AUTH_ERROR, expired ? ErrorCode.TOKEN_EXPIRED : ErrorCode.TOKEN_INVALID);
            } catch (JwtException | IllegalArgumentException e) {
                request.setAttribute(AUTH_ERROR, ErrorCode.TOKEN_INVALID);
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
