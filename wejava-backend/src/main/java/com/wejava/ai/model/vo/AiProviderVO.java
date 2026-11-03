package com.wejava.ai.model.vo;

import com.wejava.ai.provider.AiProvider;

/**
 * 厂商预设的对外结构。
 *
 * <p>直接用枚举返回会被 Jackson 序列化成常量名（DEEPSEEK / ZHIPU…），
 * 前端要的是业务 key 和可读名称，所以单独包一层。
 */
public record AiProviderVO(String key, String label, String baseUrl, String consoleUrl) {

    public static AiProviderVO of(AiProvider provider) {
        return new AiProviderVO(
                provider.getKey(), provider.getLabel(), provider.getBaseUrl(), provider.getConsoleUrl());
    }
}
