package com.wejava.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
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

    /** 密钥最小长度，太短时派生出来的密钥强度不足，启动即拒绝 */
    private static final int MIN_SECRET_LENGTH = 16;

    private final SecretKeySpec key;
    /** 旧版本用「截断 + 零填充」生成的密钥，仅供解密历史数据，新数据一律用 key */
    private final SecretKeySpec legacyKey;

    private final SecureRandom random = new SecureRandom();

    public AesUtils(@Value("${wejava.ai.config-secret:wejava-dev-ai-config-secret-32b!!}") String secret) {
        if (secret == null || secret.trim().length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("wejava.ai.config-secret 至少需要 " + MIN_SECRET_LENGTH + " 个字符（当前长度: "
                    + (secret == null ? 0 : secret.trim().length()) + "）");
        }
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        this.key = new SecretKeySpec(Arrays.copyOf(sha256(raw), 16), "AES");

        byte[] legacy = new byte[16];
        System.arraycopy(raw, 0, legacy, 0, Math.min(raw.length, 16));
        this.legacyKey = new SecretKeySpec(legacy, "AES");
    }

    /** 用 SHA-256 把任意长度的口令派生成定长密钥，避免短口令产生零填充 */
    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JVM 不支持 SHA-256", e);
        }
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
        byte[] iv;
        byte[] encrypted;
        try {
            byte[] all = Base64.getDecoder().decode(cipherText);
            if (all.length <= IV_LENGTH) {
                return null;
            }
            iv = new byte[IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH);
            encrypted = new byte[all.length - IV_LENGTH];
            System.arraycopy(all, IV_LENGTH, encrypted, 0, encrypted.length);
        } catch (IllegalArgumentException e) {
            // 不是合法的 base64，按「解不开」处理
            return null;
        }

        try {
            return doDecrypt(key, iv, encrypted);
        } catch (Exception e) {
            try {
                // 兼容旧版本加密的数据（密钥派生方式已升级）
                return doDecrypt(legacyKey, iv, encrypted);
            } catch (Exception ignored) {
                // 两把密钥都解不开，说明密钥真的变了，返回空让上层走「未配置」分支
                return null;
            }
        }
    }

    private String doDecrypt(SecretKeySpec secretKey, byte[] iv, byte[] encrypted) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    /** 展示用掩码：保留首 5 位和末 4 位，中间用 **** 代替 */
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
