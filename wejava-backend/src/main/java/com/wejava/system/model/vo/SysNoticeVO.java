package com.wejava.system.model.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SysNoticeVO {

    private Long noticeId;
    private String noticeTitle;
    private Integer noticeType;
    private String noticeContent;
    private Integer status;
    private LocalDateTime createTime;
}
