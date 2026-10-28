package com.wejava.security.handler;

import com.wejava.common.core.R;
import com.wejava.common.exception.ErrorCode;
import com.wejava.security.jwt.JwtAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 401 响应。区分"过期"（1005，前端静默刷新）和"失效"（1006，前端跳登录页）。
 */
@Component
@RequiredArgsConstructor
public class RestAuthEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        Object attribute = request.getAttribute(JwtAuthFilter.AUTH_ERROR);
        ErrorCode code = attribute instanceof ErrorCode errorCode ? errorCode : ErrorCode.NOT_LOGIN;

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(jsonMapper.writeValueAsString(R.fail(code)));
    }
}
