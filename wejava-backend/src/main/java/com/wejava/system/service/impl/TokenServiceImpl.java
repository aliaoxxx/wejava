package com.wejava.system.service.impl;

import com.wejava.common.exception.BizException;
import com.wejava.common.exception.ErrorCode;
import com.wejava.security.config.SecurityProperties;
import com.wejava.security.jwt.JwtService;
import com.wejava.security.jwt.SessionStore;
import com.wejava.security.model.AuthUser;
import com.wejava.security.model.LoginSession;
import com.wejava.system.model.vo.LoginVO;
import com.wejava.system.service.LoginUserDetailsService;
import com.wejava.system.service.TokenService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

/**
 * 令牌生命周期：签发、刷新（含轮换）、登出。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final JwtService jwtService;
    private final SessionStore sessionStore;
    private final SecurityProperties properties;
    private final LoginUserDetailsService userDetailsService;

    @Override
    public LoginVO issue(LoginSession session) {
        if (properties.isSingleLogin()) {
            sessionStore.kickUser(session.getUserId());
        }

        String accessJti = UUID.randomUUID().toString().replace("-", "");
        String refreshJti = UUID.randomUUID().toString().replace("-", "");
        session.setJti(accessJti);
        session.setRefreshJti(refreshJti);

        String accessToken = jwtService.createAccessToken(session, accessJti);
        String refreshToken = jwtService.createRefreshToken(session.getUserId(), refreshJti);

        sessionStore.saveAccess(accessJti, session);
        sessionStore.saveRefresh(refreshJti, session.getUserId(), accessJti);

        return new LoginVO(accessToken, refreshToken, properties.getAccessTtl().toSeconds(), "Bearer");
    }

    @Override
    public LoginVO refresh(String refreshToken) {
        Jwt jwt;
        try {
            jwt = jwtService.decode(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BizException(ErrorCode.NOT_LOGIN);
        }
        if (!JwtService.TYPE_REFRESH.equals(jwt.getClaimAsString(JwtService.CLAIM_TYPE))) {
            throw new BizException(ErrorCode.NOT_LOGIN);
        }

        String refreshJti = jwt.getId();
        String[] pair = sessionStore.getRefresh(refreshJti);
        if (pair == null) {
            // 已被使用过（rotation）或已过期
            throw new BizException(ErrorCode.NOT_LOGIN);
        }

        Long userId;
        try {
            userId = Long.valueOf(pair[0]);
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.NOT_LOGIN);
        }
        String oldAccessJti = pair[1];

        LoginSession session = sessionStore.getAccess(oldAccessJti);
        if (session == null) {
            // 会话已过期，按 userId 重建；此时必须校验账号是否仍可用
            AuthUser authUser = userDetailsService.loadByUserId(userId);
            if (!authUser.isEnabled()) {
                sessionStore.kickUser(userId);
                throw new BizException(ErrorCode.ACCOUNT_DISABLED);
            }
            session = authUser.getSession();
        }

        // rotation：旧的立即作废
        sessionStore.removeRefresh(refreshJti);
        sessionStore.removeAccess(oldAccessJti);

        return issue(session);
    }

    @Override
    public void logout(LoginSession session) {
        if (session == null) {
            return;
        }
        sessionStore.removeAccess(session.getJti());
        if (session.getRefreshJti() != null) {
            sessionStore.removeRefresh(session.getRefreshJti());
        }
        log.info("登出 userId={}", session.getUserId());
    }
}
