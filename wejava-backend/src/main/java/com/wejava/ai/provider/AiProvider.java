package com.wejava.ai.provider;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 国产模型服务商预设。
 *
 * <p>只是「表单初值」——选中后自动填充 base_url，运行时永远以库里的配置为准。
 * 因此模型名不在这里硬编码：厂商模型迭代很快（智谱已到 glm-5.x、Kimi 已到 K3.x），
 * 写死就会过时，改为让用户填写或从厂商的 {@code GET /models} 拉取。
 *
 * <p>base_url 一律填到「版本号」为止，客户端再拼 {@code /chat/completions}。
 */
@Getter
@RequiredArgsConstructor
public enum AiProvider {
    DEEPSEEK("deepseek", "DeepSeek", "https://api.deepseek.com", "https://platform.deepseek.com/api_keys"),
    ZHIPU(
            "zhipu",
            "智谱 GLM",
            "https://open.bigmodel.cn/api/paas/v4",
            "https://bigmodel.cn/usercenter/proj-mgmt/apikeys"),
    QWEN("qwen", "通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1", "https://bailian.console.aliyun.com/"),
    KIMI("kimi", "Kimi", "https://api.moonshot.cn/v1", "https://platform.moonshot.cn/console/api-keys"),
    DOUBAO("doubao", "火山方舟（豆包）", "https://ark.cn-beijing.volces.com/api/v3", "https://console.volcengine.com/ark"),
    QIANFAN(
            "qianfan",
            "千帆（文心）",
            "https://qianfan.baidubce.com/v2",
            "https://console.bce.baidu.com/qianfan/ais/console/applicationConsole/application"),
    HUNYUAN(
            "hunyuan",
            "混元",
            "https://api.hunyuan.cloud.tencent.com/v1",
            "https://console.cloud.tencent.com/hunyuan/api-key"),
    SPARK("spark", "讯飞星火", "https://spark-api-open.xf-yun.com/v1", "https://console.xfyun.cn/services/bmx1"),
    SILICONFLOW("siliconflow", "硅基流动", "https://api.siliconflow.cn/v1", "https://cloud.siliconflow.cn/account/ak"),
    CUSTOM("custom", "自定义", "", null);

    /** 与 sys_ai_config.provider 对应 */
    private final String key;

    private final String label;

    /** 预设的接口地址，自定义为空 */
    private final String baseUrl;

    /** 申请密钥的控制台地址，前端展示成链接 */
    private final String consoleUrl;

    public static List<AiProvider> all() {
        return Arrays.asList(values());
    }

    public static Optional<AiProvider> of(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return all().stream().filter(p -> p.key.equalsIgnoreCase(key.trim())).findFirst();
    }
}
