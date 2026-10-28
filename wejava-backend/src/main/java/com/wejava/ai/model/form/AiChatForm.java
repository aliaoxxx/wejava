package com.wejava.ai.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiChatForm {

    /** 为空表示新建会话 */
    private Long sessionId;

    @NotBlank(message = "提问内容不能为空")
    @Size(max = 4000, message = "单次提问最长 4000 字")
    private String content;
}
