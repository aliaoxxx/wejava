package com.wejava.ai.model.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AiMessageVO {

    private Long messageId;
    private Long sessionId;
    private String role;
    private String content;
    private Integer promptTokens;
    private Integer completionTokens;
    private Long durationMs;
    private LocalDateTime createTime;
}
