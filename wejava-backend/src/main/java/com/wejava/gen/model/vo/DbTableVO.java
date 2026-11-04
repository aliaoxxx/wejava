package com.wejava.gen.model.vo;

import lombok.Data;

/** 数据库中尚未导入的表 */
@Data
public class DbTableVO {

    private String tableName;

    private String tableComment;

    private Long rowCount;

    /** 是否已导入过配置，由服务层填充 */
    private Boolean imported;
}
