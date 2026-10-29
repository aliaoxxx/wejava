package com.wejava.common.util;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * Redis 计数器。
 * <p>
 * INCR 和 EXPIRE 拆成两条命令时，进程若在两者之间挂掉，key 会永远不过期，
 * 导致限流/锁定变成永久状态。这里用 Lua 把两步合成一次原子操作。
 */
@Component
@RequiredArgsConstructor
public class RedisCounter {

    private static final DefaultRedisScript<Long> INCR_WITH_TTL = new DefaultRedisScript<>(
            """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """,
            Long.class);

    private final StringRedisTemplate redis;

    /** 自增并确保 key 有 TTL（首次创建时设置），返回自增后的值 */
    public long increment(String key, Duration ttl) {
        Long value = redis.execute(INCR_WITH_TTL, List.of(key), String.valueOf(ttl.toSeconds()));
        return value == null ? 0L : value;
    }
}
