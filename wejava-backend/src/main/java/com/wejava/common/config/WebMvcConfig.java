package com.wejava.common.config;

import com.wejava.common.interceptor.ConcurrencyInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ConcurrencyInterceptor concurrencyInterceptor;
    private final DateTimeConverters.LocalDateTimeConverter localDateTimeConverter;
    private final DateTimeConverters.LocalDateConverter localDateConverter;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // 查询参数接受 YYYY-MM-DD HH:mm:ss / YYYY-MM-DD，而不只是 ISO 格式
        registry.addConverter(localDateTimeConverter);
        registry.addConverter(localDateConverter);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(concurrencyInterceptor)
                .addPathPatterns("/**")
                // 登录、验证码、健康检查不参与并发限制
                .excludePathPatterns("/auth/login", "/auth/captcha", "/auth/refresh", "/ping", "/error");
    }
}
