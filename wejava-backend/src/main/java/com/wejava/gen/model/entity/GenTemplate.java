package com.wejava.gen.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 代码生成-模板覆盖（有记录表示在线编辑过） */
@Data
@TableName("gen_template")
public class GenTemplate {

    @TableId(value = "template_id", type = IdType.AUTO)
    private Long templateId;

    private String templateName;

    private String content;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
