package com.wejava.common.config;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 启动时的密钥体检：生产环境若仍在使用开发默认密钥，直接拒绝启动。
 * <p>
 * 默认密钥写在代码里等于没有加密——JWT 可被伪造、AI 密钥可被解密，
 * 这类问题一旦上线几乎无法补救，所以在启动阶段就把门关死。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupSecretValidator implements ApplicationRunner {

    private static final String DEFAULT_JWT_SECRET = "wejava-dev-secret-key-please-change-in-production";
    private static final String DEFAULT_AI_SECRET = "wejava-dev-ai-config-secret-32b!!";

    private final Environment environment;

    @Value("${wejava.auth.secret:}")
    private String jwtSecret;

    @Value("${wejava.ai.config-secret:}")
    private String aiConfigSecret;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (!isProd()) {
            log.warn("当前非生产环境，跳过默认密钥校验");
            return;
        }

        boolean jwtDefault = isBlankOr(jwtSecret, DEFAULT_JWT_SECRET);
        boolean aiDefault = isBlankOr(aiConfigSecret, DEFAULT_AI_SECRET);
        if (jwtDefault || aiDefault) {
            StringBuilder sb = new StringBuilder("生产环境禁止使用默认密钥，请通过环境变量配置：");
            if (jwtDefault) {
                sb.append("\n  - JWT_SECRET（当前为开发默认值）");
            }
            if (aiDefault) {
                sb.append("\n  - AI_CONFIG_SECRET（当前为开发默认值）");
            }
            throw new IllegalStateException(sb.toString());
        }
        log.info("密钥校验通过");
    }

    private boolean isProd() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    private boolean isBlankOr(String value, String defaultValue) {
        return value == null || value.isBlank() || defaultValue.equals(value);
    }
}
