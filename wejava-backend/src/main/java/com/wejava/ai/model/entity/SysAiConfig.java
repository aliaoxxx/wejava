package com.wejava.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_ai_config")
public class SysAiConfig {

    @TableId(value = "ai_config_id", type = IdType.AUTO)
    private Long aiConfigId;

    private String provider;

    private String model;

    private String baseUrl;

    /** 数据库中为 AES 密文 */
    private String apiKey;

    private BigDecimal temperature;

    private Integer maxTokens;

    private Integer enabled;

    private String updateBy;

    private LocalDateTime updateTime;
}
