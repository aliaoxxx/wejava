package com.wejava.system.model.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新 Token 表单。
 */
@Data
public class RefreshForm {

    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
