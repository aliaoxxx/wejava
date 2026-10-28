package com.wejava.security.service.impl;

import com.wejava.common.exception.BizException;
import com.wejava.common.exception.ErrorCode;
import com.wejava.security.config.SecurityProperties;
import com.wejava.security.model.Captcha;
import com.wejava.security.service.CaptchaRenderer;
import com.wejava.security.service.CaptchaService;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 验证码编排：生成答案、存 Redis、校验。绘图交给 CaptchaRenderer。
 */
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private static final String KEY = "admin:auth:captcha:";
    private static final Duration TTL = Duration.ofMinutes(2);
    /** 去掉 0/O/1/I/l 等易混淆字符 */
    private static final char[] CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();

    private static final int LENGTH = 4;

    private final StringRedisTemplate redis;
    private final SecurityProperties properties;
    private final CaptchaRenderer renderer;
    private final SecureRandom random = new SecureRandom();

    @Override
    public Captcha generate() {
        String key = UUID.randomUUID().toString().replace("-", "");
        char[] code = new char[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            code[i] = CHARS[random.nextInt(CHARS.length)];
        }
        String text = new String(code);
        redis.opsForValue().set(KEY + key, text, TTL);
        return new Captcha(key, renderer.render(text));
    }

    @Override
    public void verify(String key, String input) {
        if (!properties.isCaptchaEnabled()) {
            return;
        }
        if (!StringUtils.hasText(key) || !StringUtils.hasText(input)) {
            throw new BizException(ErrorCode.CAPTCHA_ERROR);
        }
        String redisKey = KEY + key;
        String expected = redis.opsForValue().get(redisKey);
        // 一次性使用：无论对错都删，防止爆破
        redis.delete(redisKey);
        if (expected == null || !expected.equalsIgnoreCase(input.trim())) {
            throw new BizException(ErrorCode.CAPTCHA_ERROR);
        }
    }
}
