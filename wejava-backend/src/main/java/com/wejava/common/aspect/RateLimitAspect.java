package com.wejava.common.aspect;

import com.wejava.common.annotation.RateLimit;
import com.wejava.common.exception.RateLimitException;
import com.wejava.common.util.RedisCounter;
import com.wejava.common.web.IpUtils;
import com.wejava.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 限流切面：固定窗口计数。
 * key = 方法签名 + (用户ID 或 IP)，窗口单位由注解决定。
 */
@Slf4j
@Aspect
@Component
@Order(2)
@RequiredArgsConstructor
public class RateLimitAspect {

    private static final String KEY_PREFIX = "admin:rate:";

    private final RedisCounter redisCounter;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String redisKey = KEY_PREFIX + buildKey(joinPoint);
        // INCR + EXPIRE 在 Lua 里原子完成，避免两步之间进程退出导致 key 永不过期
        long current = redisCounter.increment(redisKey, Duration.ofSeconds(rateLimit.seconds()));

        if (current > rateLimit.count()) {
            String message =
                    rateLimit.message().isBlank() ? "请求过于频繁，请 " + rateLimit.seconds() + " 秒后再试" : rateLimit.message();
            throw new RateLimitException(message);
        }
        return joinPoint.proceed();
    }

    private String buildKey(ProceedingJoinPoint joinPoint) {
        String method = joinPoint.getSignature().toShortString();
        Long userId = SecurityUtils.currentUserId();
        String identity = userId != null ? "u" + userId : "ip" + clientIp();
        return method + ":" + identity;
    }

    private String clientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        return IpUtils.getIp(request);
    }
}
