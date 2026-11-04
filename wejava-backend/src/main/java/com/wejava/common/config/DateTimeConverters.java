package com.wejava.common.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * 查询参数里的时间字符串转换器。
 *
 * <p>Spring 默认只认 ISO 格式（2026-01-15T00:00:00），而前端日期选择器发的是
 * {@code YYYY-MM-DD HH:mm:ss}，直接用会报 400。这里同时接受几种常见写法，
 * 避免每个 Query 对象都要加 {@code @DateTimeFormat}。
 */
@Component
public class DateTimeConverters {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 2026-01-15 10:30:00 / 2026-01-15T10:30:00 / 2026-01-15 */
    public static LocalDateTime parseDateTime(String value) {
        String text = value.trim();
        List<DateTimeFormatter> candidates = List.of(DATE_TIME, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        for (DateTimeFormatter formatter : candidates) {
            try {
                return LocalDateTime.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
                // 继续尝试下一种格式
            }
        }
        try {
            return LocalDate.parse(text, DATE).atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("时间格式不正确：" + value);
        }
    }

    /** 2026-01-15 / 2026-01-15 10:30:00（取日期部分） */
    public static LocalDate parseDate(String value) {
        String text = value.trim();
        if (text.length() > 10) {
            text = text.substring(0, 10);
        }
        try {
            return LocalDate.parse(text, DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("日期格式不正确：" + value);
        }
    }

    /** 注册到 Spring MVC，查询参数（Query 对象字段）自动生效 */
    @Component
    public static class LocalDateTimeConverter implements Converter<String, LocalDateTime> {
        @Override
        public LocalDateTime convert(String source) {
            return source == null || source.isBlank() ? null : parseDateTime(source);
        }
    }

    @Component
    public static class LocalDateConverter implements Converter<String, LocalDate> {
        @Override
        public LocalDate convert(String source) {
            return source == null || source.isBlank() ? null : parseDate(source);
        }
    }
}
