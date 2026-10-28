package com.wejava.common.sse;

import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * SSE 连接票据。
 * <p>
 * EventSource 不能设置请求头，令牌若直接放在 URL 会进入 Nginx 日志和浏览器历史。
 * 这里改为一次性票据：30 秒有效、用后即删，泄露了也只是一串失效的随机值。
 */
@Service
@RequiredArgsConstructor
public class SseTicketService {

    private static final String KEY_PREFIX = "admin:sse:ticket:";
    private static final Duration TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redis;

    public String issue(Long userId) {
        String ticket = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(KEY_PREFIX + ticket, String.valueOf(userId), TTL);
        return ticket;
    }

    /** 一次性消费：拿到 userId 的同时删除票据 */
    public Long consume(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return null;
        }
        String key = KEY_PREFIX + ticket;
        String userId = redis.opsForValue().getAndDelete(key);
        if (userId == null) {
            return null;
        }
        try {
            return Long.valueOf(userId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
