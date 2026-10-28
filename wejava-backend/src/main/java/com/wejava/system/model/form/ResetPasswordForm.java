package com.wejava.system.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重置密码（管理员操作）。
 */
@Data
public class ResetPasswordForm {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度 6~20")
    private String password;
}
