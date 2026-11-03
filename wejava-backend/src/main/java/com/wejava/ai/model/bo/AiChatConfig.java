package com.wejava.ai.model.bo;

/**
 * 对话链路实际使用的端点快照。
 *
 * <p>由 {@code AiConfigService.resolve(configId)} 组装：优先用请求指定的端点，
 * 未指定时取默认端点。密钥在这里已是明文，**不要**把它写进日志或响应。
 */
public record AiChatConfig(
        Long id,
        String provider,
        String configName,
        String model,
        String baseUrl,
        String apiKey,
        double temperature,
        int maxTokens) {}
