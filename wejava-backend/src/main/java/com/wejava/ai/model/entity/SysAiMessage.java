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

    private LocalDateTime createTime;
}
