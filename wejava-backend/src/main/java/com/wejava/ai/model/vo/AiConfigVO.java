package com.wejava.ai.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AiConfigVO {

    private Long aiConfigId;
    private String provider;
    private String model;
    private String baseUrl;
    /** 掩码，永不下发明文 */
    private String apiKeyMasked;

    private Boolean apiKeyConfigured;
    private BigDecimal temperature;
    private Integer maxTokens;
    private Integer enabled;
    private String updateBy;
    private LocalDateTime updateTime;
}
