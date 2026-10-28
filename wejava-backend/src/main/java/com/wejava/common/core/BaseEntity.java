package com.wejava.common.core;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 实体基类：只放审计字段，主键由各实体自己声明（表的主键名不统一：user_id / role_id / menu_id）。
 * <p>
 * 审计字段由 AuditMetaObjectHandler 自动填充，Service 里不要再手写。
 */
@Data
public abstract class BaseEntity implements Serializable {

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
