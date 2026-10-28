package com.wejava.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_message")
public class SysMessage {

    @TableId(value = "message_id", type = IdType.AUTO)
    private Long messageId;

    private String title;

    private String content;

    /** 1 公告 2 系统 3 个人 */
    private Integer msgType;

    /** 0 普通 1 重要 2 紧急 */
    private Integer level;

    private String link;

    /** 来源业务 ID，如公告 ID */
    private Long sourceId;

    private String createBy;

    private LocalDateTime createTime;
}
