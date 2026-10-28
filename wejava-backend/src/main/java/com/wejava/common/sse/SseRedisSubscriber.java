package com.wejava.common.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import tools.jackson.databind.json.JsonMapper;

/**
 * 订阅其他实例发出的 SSE 事件。
 * <p>
 * 本实例投递时也会收到自己的广播，但 deliverLocal 对「没有本地连接」的情况直接返回，
 * 所以不会重复推送。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SseRedisSubscriber {

    private final SseHub sseHub;
    private final JsonMapper jsonMapper;

    @Bean
    public RedisMessageListenerContainer sseListenerContainer(RedisConnectionFactory factory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(
                (message, pattern) -> {
                    try {
                        String channel = new String(message.getChannel());
                        Long userId = Long.valueOf(channel.substring(SseHub.CHANNEL_PREFIX.length()));
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> payload =
                                jsonMapper.readValue(new String(message.getBody()), java.util.Map.class);
                        // 自己发出的广播已经本地推送过了，跳过避免重复
                        if (sseHub.getInstanceId().equals(payload.get("from"))) {
                            return;
                        }
                        sseHub.deliverLocal(userId, (String) payload.get("event"), payload.get("data"));
                    } catch (Exception e) {
                        log.warn("SSE 消息解析失败: {}", e.getMessage());
                    }
                },
                new PatternTopic(SseHub.CHANNEL_PREFIX + "*"));
        return container;
    }
}
