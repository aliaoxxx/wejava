package com.wejava.security.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 认证相关配置，前缀 wejava.auth。
 */
@Data
@Component
@ConfigurationProperties(prefix = "wejava.auth")
public class SecurityProperties {

    /** 签发者，会写入 token 的 iss 并参与校验 */
    private String issuer = "wejava-admin";

    /** 签名密钥，HS256 要求至少 32 字节 */
    private String secret;

    /** access token 有效期 */
    private Duration accessTtl = Duration.ofMinutes(30);

    /** refresh token 有效期 */
    private Duration refreshTtl = Duration.ofDays(7);

    /** 是否开启图形验证码 */
    private boolean captchaEnabled = true;

    /** 连续登录失败多少次锁定 */
    private int maxRetry = 5;

    /** 锁定时长（分钟） */
    private int lockMinutes = 15;

    /** 是否单点登录：true 时新登录会踢掉旧会话 */
    private boolean singleLogin = false;

    /** 时钟偏移容差，多实例部署时用 */
    private Duration clockSkew = Duration.ofSeconds(60);
}
