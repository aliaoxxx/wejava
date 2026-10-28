package com.wejava.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wejava.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice")
public class SysNotice extends BaseEntity {

    @TableId(value = "notice_id", type = IdType.AUTO)
    private Long noticeId;

    private String noticeTitle;

    private Integer noticeType;

    private String noticeContent;

    private Integer status;

    /** 1 全员 2 按角色 3 按部门 4 指定用户 */
    private Integer targetType;

    /** 接收对象 ID，逗号分隔 */
    private String targetIds;
}
