package com.wejava.common.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 敏感配置的对称加密（AES-GCM）。
 * <p>
 * 密钥来自环境变量 AI_CONFIG_SECRET，未配置时使用开发默认值。
 * 密文格式：base64(iv[12] + ciphertext)，每次加密随机 IV。
 */
@Component
public class AesUtils {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public AesUtils(@Value("${wejava.ai.config-secret:wejava-dev-ai-config-secret-32b!!}") String secret) {
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = new byte[16];
        System.arraycopy(raw, 0, keyBytes, 0, Math.min(raw.length, 16));
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plain) {
        if (!StringUtils.hasText(plain)) {
            return plain;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            byte[] result = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, result, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, result, IV_LENGTH, encrypted.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new IllegalStateException("加密失败", e);
        }
    }

    public String decrypt(String cipherText) {
        if (!StringUtils.hasText(cipherText)) {
            return cipherText;
        }
        try {
            byte[] all = Base64.getDecoder().decode(cipherText);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH);
            byte[] encrypted = new byte[all.length - IV_LENGTH];
            System.arraycopy(all, IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 解密失败通常意味着密钥变了，返回空让上层走「未配置」分支
            return null;
        }
    }

    /** 展示用掩码：abcd…wxyz */
    public String mask(String plain) {
        if (!StringUtils.hasText(plain)) {
            return "";
        }
        if (plain.length() <= 8) {
            return "****";
        }
        return plain.substring(0, 5) + "****" + plain.substring(plain.length() - 4);
    }
}
