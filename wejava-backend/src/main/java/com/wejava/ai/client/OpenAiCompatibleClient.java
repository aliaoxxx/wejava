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
 * OpenAI 兼容协议的模型客户端。
 *
 * <p>国产厂商（DeepSeek、智谱、通义、Kimi、豆包、千帆、混元、星火、硅基流动）都提供
 * {@code /chat/completions} 接口，因此这里按「协议」而不是按「厂商」实现一份即可，
 * 具体地址和模型来自 {@code sys_ai_config} 的配置行。
 *
 * <p>用 JDK 自带 HttpClient 逐行读取上游 SSE，不引入额外依赖。
 * 每读到一段文本回调一次；客户端断开时通过 cancelled 立即停止，避免继续消耗 token。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiCompatibleClient {

    private final JsonUtils jsonUtils;

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public record Usage(int promptTokens, int completionTokens) {}

    /**
     * 流式对话。
     *
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

        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint(baseUrl, "/chat/completions")))
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

    /**
     * 拉取模型列表（GET /models），不支持的厂商会抛异常，由上层提示用户手工填写。
     */
    @SuppressWarnings("unchecked")
    public List<String> listModels(String baseUrl, String apiKey) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint(baseUrl, "/models")))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(describe(response.statusCode(), response.body()));
            }
            Map<String, Object> body = jsonUtils.fromJson(response.body(), Map.class);
            Object data = body == null ? null : body.get("data");
            if (!(data instanceof List<?> list)) {
                return List.of();
            }
            List<String> models = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map && map.get("id") != null) {
                    models.add(map.get("id").toString());
                }
            }
            return models;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("拉取模型列表被中断", e);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("拉取模型列表失败：" + e.getMessage(), e);
        }
    }

    /**
     * 连通性测试：发一个只要 1 个 token 的请求，返回耗时毫秒。
     */
    public long ping(String baseUrl, String apiKey, String model) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("messages", List.of(Map.of("role", "user", "content", "ping")));
        payload.put("max_tokens", 1);
        payload.put("stream", false);

        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint(baseUrl, "/chat/completions")))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonUtils.toJson(payload), StandardCharsets.UTF_8))
                .build();
        long start = System.currentTimeMillis();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(describe(response.statusCode(), response.body()));
            }
            return System.currentTimeMillis() - start;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("测试连接被中断", e);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("连接失败：" + e.getMessage(), e);
        }
    }

    /**
     * 拼接接口地址：先去掉尾斜杠，避免 {@code .../v4/} + {@code /chat/completions} 拼出双斜杠。
     */
    static String endpoint(String baseUrl, String path) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + path;
    }

    /** 把上游状态码翻译成能看懂的提示，不透传原始报文 */
    private String describe(int status, String body) {
        String hint =
                switch (status) {
                    case 401 -> "密钥无效或已过期";
                    case 403 -> "密钥没有该模型的权限";
                    case 404 -> "地址或模型不存在，检查 Base URL 是否填到版本号";
                    case 429 -> "请求过于频繁或余额不足";
                    default -> "接口返回 " + status;
                };
        return hint + "（" + truncate(body) + "）";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String data) {
        try {
            return jsonUtils.fromJson(data, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

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
        // 推理模型的 reasoning_content（思考过程）本期不展示，只取正式回答
        Object content = deltaMap.get("content");
        return content == null ? null : content.toString();
    }

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
        if (text == null) {
            return "";
        }
        return text.length() > 200 ? text.substring(0, 200) : text;
    }

    /** 组装 OpenAI 兼容的消息数组 */
    public List<Map<String, String>> messages(List<SysAiMessageView> history) {
        List<Map<String, String>> list = new ArrayList<>();
        history.forEach(item -> list.add(Map.of("role", item.role(), "content", item.content())));
        return list;
    }
}
