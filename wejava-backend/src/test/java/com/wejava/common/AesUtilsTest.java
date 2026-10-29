package com.wejava.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class AesUtilsTest {

    private static final String SECRET = "wejava-test-ai-config-secret-32!!";

    @Test
    void 加密后能解密回原文() {
        AesUtils aes = new AesUtils(SECRET);
        String cipherText = aes.encrypt("unit-test-secret-value");

        assertThat(cipherText).isNotEqualTo("unit-test-secret-value");
        assertThat(aes.decrypt(cipherText)).isEqualTo("unit-test-secret-value");
    }

    @Test
    void 相同明文两次加密结果不同_因为IV随机() {
        AesUtils aes = new AesUtils(SECRET);
        assertThat(aes.encrypt("same")).isNotEqualTo(aes.encrypt("same"));
    }

    @Test
    void 密钥太短时启动即失败() {
        assertThatThrownBy(() -> new AesUtils("short")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 旧版本零填充密钥加密的数据仍然可以解密() throws Exception {
        // 按旧派生方式（截断 + 零填充）加密，模拟生产库里已有的历史数据
        byte[] raw = SECRET.getBytes(StandardCharsets.UTF_8);
        byte[] legacyKeyBytes = new byte[16];
        System.arraycopy(raw, 0, legacyKeyBytes, 0, Math.min(raw.length, 16));

        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(legacyKeyBytes, "AES"), new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal("legacy-api-key".getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        AesUtils aes = new AesUtils(SECRET);
        assertThat(aes.decrypt(Base64.getEncoder().encodeToString(combined))).isEqualTo("legacy-api-key");
    }

    @Test
    void 密文被篡改时返回null而不是抛异常() {
        AesUtils aes = new AesUtils(SECRET);
        String tampered = aes.encrypt("secret-value");
        String broken = tampered.substring(0, tampered.length() - 4) + "AAAA";

        assertThat(aes.decrypt(broken)).isNull();
    }

    @Test
    void 掩码只保留首尾() {
        AesUtils aes = new AesUtils(SECRET);
        assertThat(aes.mask("unit-test-key-abcdefghijklmnop")).startsWith("unit-").endsWith("mnop");
        assertThat(aes.mask("short")).isEqualTo("****");
    }
}
