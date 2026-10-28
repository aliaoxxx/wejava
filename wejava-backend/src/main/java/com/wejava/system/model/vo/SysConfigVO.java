package com.wejava.system.model.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SysConfigVO {

    private Long configId;
    private String configName;
    private String configKey;
    private String configValue;
    private Integer configType;
    private String remark;
    private LocalDateTime createTime;
}
