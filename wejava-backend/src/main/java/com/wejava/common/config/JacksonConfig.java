package com.wejava.common.config;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * Jackson 3 配置（注意：包名是 tools.jackson，不是 com.fasterxml）。
 * <p>
 * 两件事必须做：
 * 1. Long → String：雪花 ID 19 位超过 JS 安全整数范围，直接给前端会精度丢失
 * 2. LocalDateTime 统一格式：避免前端拿到 ISO 的 "T" 分隔
 */
@Configuration
public class JacksonConfig {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Bean
    public JsonMapperBuilderCustomizer wejavaJsonCustomizer() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        return builder -> {
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
            module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
            builder.addModule(module);
        };
    }
}
