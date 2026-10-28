package com.wejava.system.model.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MessageVO {

    private Long messageId;
    private String title;
    private String content;
    private Integer msgType;
    private Integer level;
    private String link;
    private Integer readFlag;
    private LocalDateTime readTime;
    private LocalDateTime createTime;
}
