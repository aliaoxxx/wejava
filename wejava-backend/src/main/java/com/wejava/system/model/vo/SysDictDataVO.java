package com.wejava.system.model.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SysDictDataVO {

    private Long dictCode;
    private Integer dictSort;
    private String dictLabel;
    private String dictValue;
    private String dictType;
    private String cssClass;
    private String listClass;
    private Integer isDefault;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}
