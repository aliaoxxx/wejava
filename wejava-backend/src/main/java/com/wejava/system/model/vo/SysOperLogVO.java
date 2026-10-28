package com.wejava.system.model.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SysOperLogVO {

    private Long operId;
    private String title;
    private Integer businessType;
    private String method;
    private String requestMethod;
    private String operName;
    private String operUrl;
    private String operIp;
    private String operParam;
    private String jsonResult;
    private Integer status;
    private String errorMsg;
    private Long costTime;
    private LocalDateTime operTime;
}
