package com.wejava.gen.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 代码生成-表配置 */
@Data
@TableName("gen_table")
public class GenTable {

    @TableId(value = "table_id", type = IdType.AUTO)
    private Long tableId;

    private String tableName;

    private String tableComment;

    /** 实体类名，如 SystemNotice */
    private String className;

    /** 模块名，如 system */
    private String moduleName;

    /** 业务名，如 notice */
    private String businessName;

    /** 功能名，用于菜单与页面标题 */
    private String functionName;

    private String packageName;

    private String author;

    private Long parentMenuId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
