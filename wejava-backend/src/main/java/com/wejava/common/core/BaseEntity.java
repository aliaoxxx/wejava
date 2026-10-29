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
/**
 * 实体公共字段。
 * <p>
 * 逻辑删除约定：只有「用户 / 角色 / 部门」使用 {@code @TableLogic}（del_flag），
 * 因为这三类会被其他业务长期引用，需要保留历史；菜单、岗位、字典等属于配置类数据，
 * 删除前已有引用检查拦截，直接物理删除即可，不再引入 del_flag 增加查询负担。
 */
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
