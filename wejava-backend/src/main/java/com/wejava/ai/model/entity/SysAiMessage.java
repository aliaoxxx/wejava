package com.wejava.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_ai_message")
public class SysAiMessage {

    @TableId(value = "message_id", type = IdType.AUTO)
    private Long messageId;

    private Long sessionId;

    /** user / assistant */
    private String role;

    private String content;

    private Integer promptTokens;

    private Integer completionTokens;

    private Long durationMs;

    /** 生成该消息的端点与模型，便于排查是哪个服务商答的 */
    private Long configId;

    private String model;

    private LocalDateTime createTime;
}
