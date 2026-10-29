package com.wejava.security.jwt;

import com.wejava.common.util.RedisCounter;
import com.wejava.security.config.SecurityProperties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 登录失败计数与锁定。只负责防爆破这一件事，不碰会话。
 */
@Component
@RequiredArgsConstructor
public class LoginAttemptStore {

    private static final String KEY_FAIL = "admin:auth:fail:";
    private static final String KEY_LOCK = "admin:auth:lock:";

    private final StringRedisTemplate redis;
    private final RedisCounter redisCounter;
    private final SecurityProperties properties;

    public long incrFail(String username) {
        // 自增和设置过期原子完成，避免计数永久累积
        return redisCounter.increment(KEY_FAIL + username, Duration.ofMinutes(properties.getLockMinutes()));
    }

    public int getFail(String username) {
        String value = redis.opsForValue().get(KEY_FAIL + username);
        return value == null ? 0 : Integer.parseInt(value);
    }

    public void clearFail(String username) {
        redis.delete(KEY_FAIL + username);
        redis.delete(KEY_LOCK + username);
    }

    public boolean isLocked(String username) {
        return Boolean.TRUE.equals(redis.hasKey(KEY_LOCK + username));
    }

    public void lock(String username) {
        redis.opsForValue().set(KEY_LOCK + username, "1", Duration.ofMinutes(properties.getLockMinutes()));
    }
}
