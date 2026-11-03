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

    /** 显示名称，如「智谱 GLM」 */
    private String configName;

    private String provider;

    /** 协议，当前仅兼容 OpenAI 的 chat/completions */
    private String protocol;

    private String model;

    private String baseUrl;

    /** 数据库中为 AES 密文 */
    private String apiKey;

    private BigDecimal temperature;

    private Integer maxTokens;

    private Integer enabled;

    /** 聊天默认使用的端点（全表唯一） */
    private Integer isDefault;

    private Integer sort;

    private String remark;

    private String updateBy;

    private LocalDateTime updateTime;
}
