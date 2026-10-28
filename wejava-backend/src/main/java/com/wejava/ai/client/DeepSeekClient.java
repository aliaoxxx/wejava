package com.wejava.ai.client;

import com.wejava.common.util.JsonUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * DeepSeek 客户端（OpenAI 兼容协议）。
 * <p>
 * 用 JDK 自带 HttpClient 逐行读取上游 SSE，不引入额外依赖。
 * 每读到一段文本回调一次；客户端断开时通过 cancelled 立即停止，避免继续消耗 token。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeepSeekClient {

    private final JsonUtils jsonUtils;

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public record Usage(int promptTokens, int completionTokens) {}

    /**
     * @param messages  完整上下文（含历史）
     * @param onDelta   每段增量文本
     * @param cancelled 客户端是否已断开
     * @param onUsage   结束时回传用量（上游未返回时为 0）
     */
    public void stream(
            String baseUrl,
            String apiKey,
            String model,
            List<Map<String, String>> messages,
            Consumer<String> onDelta,
            BooleanSupplier cancelled,
            Consumer<Usage> onUsage) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("messages", messages);
        payload.put("stream", true);
        payload.put("stream_options", Map.of("include_usage", true));

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofMinutes(5))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(jsonUtils.toJson(payload), StandardCharsets.UTF_8))
                .build();

        int promptTokens = 0;
        int completionTokens = 0;
        try {
            HttpResponse<java.io.InputStream> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                String body = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new IllegalStateException("模型接口返回 " + response.statusCode() + ": " + truncate(body));
            }

            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (cancelled.getAsBoolean()) {
                        log.info("客户端已断开，停止读取上游流");
                        break;
                    }
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    Map<String, Object> chunk = parse(data);
                    String delta = extractDelta(chunk);
                    if (delta != null && !delta.isEmpty()) {
                        onDelta.accept(delta);
                    }
                    Usage usage = extractUsage(chunk);
                    if (usage != null) {
                        promptTokens = usage.promptTokens();
                        completionTokens = usage.completionTokens();
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage() == null ? "调用模型失败" : e.getMessage(), e);
        } finally {
            onUsage.accept(new Usage(promptTokens, completionTokens));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String data) {
        try {
            return jsonUtils.fromJson(data, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private String extractDelta(Map<String, Object> chunk) {
        Object choices = chunk.get("choices");
        if (!(choices instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> choice)) {
            return null;
        }
        Object delta = choice.get("delta");
        if (!(delta instanceof Map<?, ?> deltaMap)) {
            return null;
        }
        Object content = deltaMap.get("content");
        return content == null ? null : content.toString();
    }

    @SuppressWarnings("unchecked")
    private Usage extractUsage(Map<String, Object> chunk) {
        Object usage = chunk.get("usage");
        if (!(usage instanceof Map<?, ?> map)) {
            return null;
        }
        Object prompt = map.get("prompt_tokens");
        Object completion = map.get("completion_tokens");
        if (prompt == null && completion == null) {
            return null;
        }
        return new Usage(
                prompt == null ? 0 : Integer.parseInt(prompt.toString()),
                completion == null ? 0 : Integer.parseInt(completion.toString()));
    }

    private String truncate(String text) {
        return text.length() > 200 ? text.substring(0, 200) : text;
    }

    /** 组装 OpenAI 兼容的消息数组 */
    public List<Map<String, String>> messages(List<SysAiMessageView> history) {
        List<Map<String, String>> list = new ArrayList<>();
        history.forEach(item -> list.add(Map.of("role", item.role(), "content", item.content())));
        return list;
    }
}
