package com.wejava.gen.model.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 表配置详情 */
@Data
public class GenTableVO {

    private Long tableId;
    private String tableName;
    private String tableComment;
    private String className;
    private String moduleName;
    private String businessName;
    private String functionName;
    private String packageName;
    private String author;
    private Long parentMenuId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<GenTableColumnVO> columns;
}
