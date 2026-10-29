package com.wejava.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 为每个请求生成链路 ID：写入日志 MDC、响应头，并随统一返回体返回。
 * 排查线上问题时，用户提供一个 ID 就能串起整条链路。
 * <p>
 * 请求头用通用的 {@code X-Request-Id}，上游网关/代理（如 Nginx）可以透传，便于跨服务串联。
 * 外部传入的值必须满足格式要求才会被采用，否则重新生成 —— 防止客户端伪造或灌入非法内容。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    public static final String HEADER = "X-Request-Id";

    /** 允许透传的链路 ID 格式：字母数字下划线中划线，8~64 位 */
    private static final Pattern ACCEPTED_TRACE_ID = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        MDC.put(TRACE_ID, traceId);
        response.setHeader(HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
        }
    }

    /** 上游传了合法 ID 就沿用（便于串联），否则生成新的 */
    private String resolveTraceId(HttpServletRequest request) {
        String incoming = request.getHeader(HEADER);
        if (incoming != null) {
            String trimmed = incoming.trim();
            if (ACCEPTED_TRACE_ID.matcher(trimmed).matches()) {
                return trimmed;
            }
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
