package com.wejava.security.jwt;

import com.wejava.security.config.SecurityProperties;
import com.wejava.security.model.LoginSession;
import com.wejava.security.model.OnlineSession;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 会话存储：登录态、在线列表、按用户踢人。
 * <p>
 * 只负责「会话」这一件事，防爆破计数在 LoginAttemptStore。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionStore {

    private static final String KEY_ACCESS = "admin:auth:token:";
    private static final String KEY_REFRESH = "admin:auth:refresh:";
    private static final String KEY_USER = "admin:auth:user:";
    private static final String KEY_ONLINE = "admin:auth:online";

    /** 在线时间戳最多每分钟更新一次，避免每个请求都写 Redis */
    private static final long TOUCH_INTERVAL_MS = 60_000L;

    private static final int TOUCH_CACHE_LIMIT = 10_000;

    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;
    private final SecurityProperties properties;

    /** 本实例内的最后活跃时间缓存，用于节流；跨实例不一致对"在线列表"无影响 */
    private final Map<String, Long> lastTouch = new ConcurrentHashMap<>();

    public void saveAccess(String jti, LoginSession session) {
        Duration ttl = properties.getAccessTtl();
        redis.opsForValue().set(KEY_ACCESS + jti, jsonMapper.writeValueAsString(session), ttl);
        redis.opsForSet().add(KEY_USER + session.getUserId(), jti);
        redis.expire(KEY_USER + session.getUserId(), ttl);
        redis.opsForZSet().add(KEY_ONLINE, jti, Instant.now().toEpochMilli());
        lastTouch.put(jti, System.currentTimeMillis());
    }

    /**
     * 读取会话。反序列化失败按"无会话"处理，让请求走 401 而不是 500。
     */
    public LoginSession getAccess(String jti) {
        String json = redis.opsForValue().get(KEY_ACCESS + jti);
        if (json == null) {
            return null;
        }
        try {
            return jsonMapper.readValue(json, LoginSession.class);
        } catch (RuntimeException e) {
            log.warn("会话反序列化失败，按失效处理 jti={}", jti, e);
            return null;
        }
    }

    public void saveRefresh(String refreshJti, Long userId, String accessJti) {
        redis.opsForValue().set(KEY_REFRESH + refreshJti, userId + ":" + accessJti, properties.getRefreshTtl());
    }

    /** @return [userId, accessJti]，不存在或格式异常返回 null */
    public String[] getRefresh(String refreshJti) {
        String value = redis.opsForValue().get(KEY_REFRESH + refreshJti);
        if (value == null) {
            return null;
        }
        int idx = value.indexOf(':');
        if (idx <= 0) {
            return null;
        }
        return new String[] {value.substring(0, idx), value.substring(idx + 1)};
    }

    public void removeRefresh(String refreshJti) {
        redis.delete(KEY_REFRESH + refreshJti);
    }

    public void removeAccess(String jti) {
        redis.delete(KEY_ACCESS + jti);
        redis.opsForZSet().remove(KEY_ONLINE, jti);
        lastTouch.remove(jti);
    }

    /**
     * 刷新在线时间。本地缓存 60 秒内的重复调用直接跳过，
     * 把「每请求一次写」降为「每会话每分钟一次写」。
     */
    public void touch(String jti) {
        if (jti == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Long previous = lastTouch.get(jti);
        if (previous != null && now - previous < TOUCH_INTERVAL_MS) {
            return;
        }
        if (lastTouch.size() > TOUCH_CACHE_LIMIT) {
            lastTouch.clear();
        }
        lastTouch.put(jti, now);
        redis.opsForZSet().add(KEY_ONLINE, jti, now);
    }

    /** 分页取在线会话（按最后活跃时间倒序） */
    public List<OnlineSession> listOnline(long offset, long limit) {
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> tuples =
                redis.opsForZSet().reverseRangeWithScores(KEY_ONLINE, offset, offset + limit - 1);
        List<OnlineSession> result = new java.util.ArrayList<>();
        if (tuples == null) {
            return result;
        }
        for (var tuple : tuples) {
            String jti = tuple.getValue();
            if (jti == null) {
                continue;
            }
            LoginSession session = getAccess(jti);
            if (session == null) {
                // 令牌已过期，顺手清掉在线表里的僵尸记录
                redis.opsForZSet().remove(KEY_ONLINE, jti);
                continue;
            }
            long lastActive = tuple.getScore() == null ? 0L : tuple.getScore().longValue();
            result.add(new OnlineSession(jti, session, lastActive));
        }
        return result;
    }

    public long countOnline() {
        Long size = redis.opsForZSet().zCard(KEY_ONLINE);
        return size == null ? 0L : size;
    }

    /** 强制某个会话下线 */
    public void kickSession(String jti) {
        LoginSession session = getAccess(jti);
        if (session != null && session.getRefreshJti() != null) {
            redis.delete(KEY_REFRESH + session.getRefreshJti());
        }
        removeAccess(jti);
    }

    /**
     * 踢掉某个用户的全部会话。
     * 必须先读会话拿到 refreshJti，否则只删了 access，refresh token 仍能换出新 token。
     */
    public void kickUser(Long userId) {
        Set<String> jtis = redis.opsForSet().members(KEY_USER + userId);
        if (jtis != null) {
            for (String jti : jtis) {
                LoginSession session = getAccess(jti);
                if (session != null && session.getRefreshJti() != null) {
                    redis.delete(KEY_REFRESH + session.getRefreshJti());
                }
                redis.delete(KEY_ACCESS + jti);
                redis.opsForZSet().remove(KEY_ONLINE, jti);
                lastTouch.remove(jti);
            }
        }
        redis.delete(KEY_USER + userId);
        log.info("已踢下线 userId={}, 会话数={}", userId, jtis == null ? 0 : jtis.size());
    }
}
