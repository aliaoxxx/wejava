package com.wejava.system.service;

import com.wejava.system.model.form.LoginForm;
import com.wejava.system.model.vo.LoginVO;

/**
 * 登录编排：验证码 → 锁定检查 → 密码认证 → 签发令牌 → 记日志。
 */
public interface LoginService {

    LoginVO login(LoginForm form, String ip, String userAgent);
}
