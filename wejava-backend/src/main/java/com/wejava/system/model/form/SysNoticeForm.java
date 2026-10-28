package com.wejava.system.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SysNoticeForm {

    private Long noticeId;

    @NotBlank(message = "公告标题不能为空")
    @Size(max = 50, message = "公告标题最长 50")
    private String noticeTitle;

    private Integer noticeType;

    private String noticeContent;

    private Integer status;
}
