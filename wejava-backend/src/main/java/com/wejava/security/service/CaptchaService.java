package com.wejava.security.service;

import com.wejava.security.model.Captcha;

/**
 * 验证码生命周期：生成并缓存答案、校验并一次性销毁。
 */
public interface CaptchaService {

    Captcha generate();

    void verify(String key, String input);
}
