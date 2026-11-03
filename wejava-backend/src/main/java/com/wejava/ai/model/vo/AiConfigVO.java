package com.wejava.ai.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AiConfigVO {

    private Long aiConfigId;
    private String configName;
    private String provider;
    /** 服务商中文名，前端直接展示 */
    private String providerLabel;

    private String protocol;
    private String model;
    private String baseUrl;
    /** 掩码，永不下发明文 */
    private String apiKeyMasked;

    private Boolean apiKeyConfigured;
    private BigDecimal temperature;
    private Integer maxTokens;
    private Integer enabled;
    /** 是否聊天默认端点 */
    private Boolean isDefault;

    private Integer sort;
    private String remark;
    private String updateBy;
    private LocalDateTime updateTime;
}
