package com.wejava.ai.model.form;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class AiConfigForm {

    @NotBlank(message = "模型不能为空")
    @Size(max = 64, message = "模型名最长 64")
    private String model;

    @NotBlank(message = "Base URL 不能为空")
    @Size(max = 200, message = "Base URL 最长 200")
    private String baseUrl;

    /** 传空或掩码表示保持原值 */
    private String apiKey;

    @DecimalMin(value = "0.0", message = "温度不能小于 0")
    @DecimalMax(value = "2.0", message = "温度不能大于 2")
    private BigDecimal temperature;

    @Min(value = 1, message = "最大输出不能小于 1")
    @Max(value = 32768, message = "最大输出不能超过 32768")
    private Integer maxTokens;

    private Integer enabled;
}
