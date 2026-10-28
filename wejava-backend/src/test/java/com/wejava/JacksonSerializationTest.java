package com.wejava;

import static org.assertj.core.api.Assertions.assertThat;

import com.wejava.common.config.JacksonConfig;
import java.time.LocalDateTime;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;

/**
 * 验证设计文档里最关键的两个序列化约定：
 * Long → String（防前端精度丢失）、LocalDateTime → yyyy-MM-dd HH:mm:ss
 */
@SpringBootTest
@ActiveProfiles("test")
class JacksonSerializationTest {

    @Autowired
    private JsonMapper jsonMapper;

    @Data
    static class Payload {
        private Long userId;
        private LocalDateTime createTime;
    }

    @Test
    void longShouldSerializeAsString() {
        Payload payload = new Payload();
        payload.setUserId(1234567890123456789L);

        String json = jsonMapper.writeValueAsString(payload);

        assertThat(json).contains("\"userId\":\"1234567890123456789\"");
    }

    @Test
    void localDateTimeShouldUseConfiguredPattern() {
        Payload payload = new Payload();
        payload.setCreateTime(LocalDateTime.of(2026, 10, 28, 9, 30, 0));

        String json = jsonMapper.writeValueAsString(payload);

        assertThat(json).contains("\"createTime\":\"" + "2026-10-28 09:30:00" + "\"");
        assertThat(JacksonConfig.DATE_TIME_PATTERN).isEqualTo("yyyy-MM-dd HH:mm:ss");
    }

    @Test
    void localDateTimeShouldDeserializeFromConfiguredPattern() {
        Payload payload = jsonMapper.readValue("{\"createTime\":\"2026-10-28 09:30:00\"}", Payload.class);

        assertThat(payload.getCreateTime()).isEqualTo(LocalDateTime.of(2026, 10, 28, 9, 30, 0));
    }
}
