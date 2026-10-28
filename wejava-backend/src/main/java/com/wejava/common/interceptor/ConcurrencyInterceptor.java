package com.wejava.common.interceptor;

import com.wejava.common.core.R;
import com.wejava.common.exception.ErrorCode;
import com.wejava.common.web.IpUtils;
import com.wejava.security.SecurityUtils;
import com.wejava.security.model.LoginSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.json.JsonMapper;

/**
 * 同一 IP + 同一用户的并发请求数限制。
 * <p>
 * 用 Redis 计数器实现：请求进入 +1，结束 -1，key 带 60 秒兜底过期
 * （防止进程崩溃导致计数只增不减）。
 * 未登录请求不做限制——登录接口由防爆破机制保护。
 */
@Component
@RequiredArgsConstructor
public class ConcurrencyInterceptor implements HandlerInterceptor {

    private static final String KEY_PREFIX = "admin:concurrency:";
    private static final String ATTR_KEY = "CONCURRENCY_KEY";
    private static final Duration FALLBACK_TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redis;
    private final ConcurrencyLimitResolver limitResolver;
    private final JsonMapper jsonMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws java.io.IOException {
        LoginSession session = SecurityUtils.currentSession();
        if (session == null) {
            return true;
        }

        int limit = limitResolver.getLimit();
        if (limit <= 0) {
            return true;
        }

        String key = KEY_PREFIX + session.getUserId() + ":" + IpUtils.getIp(request);
        Long current = redis.opsForValue().increment(key);
        if (current != null && current == 1L) {
            redis.expire(key, FALLBACK_TTL);
        }

        if (current != null && current > limit) {
            redis.opsForValue().decrement(key);
            // 直接写 429，不走全局异常（限流属于协议层拒绝，客户端/网关需要看到状态码）
            response.setStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Retry-After", "1");
            response.getWriter()
                    .write(jsonMapper.writeValueAsString(
                            R.fail(ErrorCode.TOO_MANY_REQUESTS.getCode(), "操作过于频繁（同一账号最多 " + limit + " 个并发请求），请稍后重试")));
            return false;
        }

        request.setAttribute(ATTR_KEY, key);
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object key = request.getAttribute(ATTR_KEY);
        if (key instanceof String value && !value.isEmpty()) {
            redis.opsForValue().decrement(value);
        }
    }
}
