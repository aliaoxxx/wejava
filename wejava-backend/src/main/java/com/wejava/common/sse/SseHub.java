package com.wejava.common.sse;

import com.wejava.common.util.JsonUtils;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 连接中心。
 * <p>
 * 本地维护 userId -> emitter 列表；跨实例通过 Redis Pub/Sub 转发：
 * 任何实例投递消息都先 PUBLISH，各实例收到后只推给本地在线的连接。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseHub {

    public static final String CHANNEL_PREFIX = "admin:sse:user:";

    /** 单个用户最多允许的连接数，防止前端异常重连堆积 */
    private static final int MAX_CONNECTIONS_PER_USER = 3;

    /** 30 分钟断开一次，让客户端走重连流程，顺带释放资源 */
    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

    /** 实例标识：广播时带上，订阅回调据此跳过自己发出的消息，避免重复推送 */
    private final String instanceId = java.util.UUID.randomUUID().toString();

    private final Map<Long, List<SseEmitter>> connections = new ConcurrentHashMap<>();
    private final StringRedisTemplate redis;
    private final JsonUtils jsonUtils;

    public SseEmitter connect(Long userId) {
        List<SseEmitter> list = connections.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>());
        if (list.size() >= MAX_CONNECTIONS_PER_USER) {
            SseEmitter oldest = list.remove(0);
            oldest.complete();
        }

        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        list.add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> {
            remove(userId, emitter);
            emitter.complete();
        });
        emitter.onError(e -> remove(userId, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name(SseEventType.CONNECTED)
                    .data(
                            Map.of("userId", String.valueOf(userId)),
                            org.springframework.http.MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            remove(userId, emitter);
        }
        return emitter;
    }

    /** 推送给指定用户（跨实例）：先本地推，再广播给其他实例 */
    public void sendToUser(Long userId, String event, Object data) {
        deliverLocal(userId, event, data);
        try {
            redis.convertAndSend(
                    CHANNEL_PREFIX + userId,
                    jsonUtils.toJson(Map.of(
                            "from", instanceId,
                            "event", event,
                            "data", data)));
        } catch (Exception e) {
            log.warn("SSE 广播失败 userId={}: {}", userId, e.getMessage());
        }
    }

    /** 批量推送，如公告投递给多个收件人 */
    public void sendToUsers(List<Long> userIds, String event, Object data) {
        if (userIds == null) {
            return;
        }
        userIds.forEach(userId -> sendToUser(userId, event, data));
    }

    /** 只推给本实例上已建立的连接（Redis 订阅回调调用） */
    public void deliverLocal(Long userId, String event, Object data) {
        List<SseEmitter> list = connections.get(userId);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(
                        SseEmitter.event().name(event).data(data, org.springframework.http.MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                // 客户端已断开，移除即可
                remove(userId, emitter);
            }
        }
    }

    /**
     * 心跳：每 15 秒发一行注释，防止 Nginx / 网关掐掉空闲连接。
     * SSE 协议里以冒号开头的行是注释，客户端会忽略。
     */
    @Scheduled(fixedDelay = 15_000L)
    public void heartbeat() {
        connections.forEach((userId, list) -> {
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    remove(userId, emitter);
                }
            }
        });
    }

    public String getInstanceId() {
        return instanceId;
    }

    public int onlineConnections() {
        return connections.values().stream().mapToInt(List::size).sum();
    }

    private void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> list = connections.get(userId);
        if (list == null) {
            return;
        }
        list.remove(emitter);
        if (list.isEmpty()) {
            connections.remove(userId);
        }
    }
}
