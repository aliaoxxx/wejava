package com.wejava.system.service;

import com.wejava.security.model.LoginSession;
import com.wejava.system.model.vo.LoginVO;

/**
 * 令牌生命周期：签发、刷新（含轮换）、登出。
 */
public interface TokenService {

    LoginVO issue(LoginSession session);

    LoginVO refresh(String refreshToken);

    void logout(LoginSession session);
}
