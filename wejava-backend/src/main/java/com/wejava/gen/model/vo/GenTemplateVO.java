package com.wejava.gen.model.vo;

import java.time.LocalDateTime;
import lombok.Data;

/** 模板列表项（不含内容，避免列表接口过大） */
@Data
public class GenTemplateVO {

    private String templateName;

    /** 是否被在线编辑覆盖 */
    private Boolean overridden;

    private Integer contentLength;

    private LocalDateTime updateTime;
}
