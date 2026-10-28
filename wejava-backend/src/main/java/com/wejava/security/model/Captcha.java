package com.wejava.security.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 验证码出参。独立成文件：一个文件只放一个类型。
 */
@Data
@AllArgsConstructor
public class Captcha {

    private String captchaKey;

    /** data:image/png;base64,... */
    private String captchaImage;
}
