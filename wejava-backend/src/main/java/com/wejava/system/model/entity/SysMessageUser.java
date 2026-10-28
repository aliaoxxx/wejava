package com.wejava.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 收件箱：一条消息对应一个接收人，记录已读状态。
 */
@Data
@TableName("sys_message_user")
public class SysMessageUser {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long messageId;

    private Long userId;

    private Integer readFlag;

    private LocalDateTime readTime;

    private LocalDateTime createTime;
}
