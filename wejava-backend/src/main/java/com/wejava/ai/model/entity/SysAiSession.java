package com.wejava.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_ai_session")
public class SysAiSession {

    @TableId(value = "session_id", type = IdType.AUTO)
    private Long sessionId;

    private Long userId;

    private String title;

    private String model;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
