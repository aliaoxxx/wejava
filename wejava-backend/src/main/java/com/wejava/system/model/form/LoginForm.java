package com.wejava.system.model.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录表单。
 */
@Data
public class LoginForm {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String captchaKey;

    private String captchaCode;
}
