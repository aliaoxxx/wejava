package com.wejava.system.service.impl;

import com.wejava.common.exception.BizException;
import com.wejava.common.exception.ErrorCode;
import com.wejava.security.config.SecurityProperties;
import com.wejava.security.jwt.LoginAttemptStore;
import com.wejava.security.model.AuthUser;
import com.wejava.security.model.LoginSession;
import com.wejava.security.service.CaptchaService;
import com.wejava.system.model.form.LoginForm;
import com.wejava.system.model.vo.LoginVO;
import com.wejava.system.service.LoginLogService;
import com.wejava.system.service.LoginService;
import com.wejava.system.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * 登录编排：验证码 → 锁定检查 → 密码认证 → 签发令牌 → 记日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final AuthenticationManager authenticationManager;
    private final CaptchaService captchaService;
    private final LoginAttemptStore loginAttemptStore;
    private final SecurityProperties properties;
    private final LoginLogService loginLogService;
    private final TokenService tokenService;

    @Override
    public LoginVO login(LoginForm form, String ip, String userAgent) {
        captchaService.verify(form.getCaptchaKey(), form.getCaptchaCode());

        if (loginAttemptStore.isLocked(form.getUsername())) {
            throw new BizException(ErrorCode.ACCOUNT_LOCKED);
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(form.getUsername(), form.getPassword()));
        } catch (DisabledException e) {
            loginLogService.record(form.getUsername(), ip, userAgent, false, "账号已停用");
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        } catch (BadCredentialsException e) {
            long failures = loginAttemptStore.incrFail(form.getUsername());
            if (failures >= properties.getMaxRetry()) {
                loginAttemptStore.lock(form.getUsername());
                log.warn("账号 {} 连续失败 {} 次，已锁定 {} 分钟", form.getUsername(), failures, properties.getLockMinutes());
            }
            loginLogService.record(form.getUsername(), ip, userAgent, false, "用户名或密码错误");
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }

        loginAttemptStore.clearFail(form.getUsername());

        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        LoginSession session = null;
        if (authUser != null) {
            session = authUser.getSession();
        }
        LoginVO vo = tokenService.issue(session);

        if (session != null) {
            loginLogService.record(session.getUsername(), ip, userAgent, true, "登录成功");
        }
        if (session != null) {
            log.info("登录成功 userId={}, username={}", session.getUserId(), session.getUsername());
        }
        return vo;
    }
}
