package com.wejava.system.service.impl;

import com.wejava.common.config.AsyncConfig;
import com.wejava.common.util.UserAgentUtils;
import com.wejava.system.mapper.SysLoginLogMapper;
import com.wejava.system.model.entity.SysLoginLog;
import com.wejava.system.service.LoginLogService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 登录日志落库。异步执行：写库失败或变慢都不影响登录响应。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl implements LoginLogService {

    private final SysLoginLogMapper loginLogMapper;

    @Override
    @Async(AsyncConfig.LOGIN_LOG_EXECUTOR)
    public void record(String username, String ip, String userAgent, boolean success, String msg) {
        try {
            SysLoginLog entity = new SysLoginLog();
            entity.setUsername(username);
            entity.setIpaddr(ip);
            entity.setBrowser(UserAgentUtils.parseBrowser(userAgent));
            entity.setOs(UserAgentUtils.parseOs(userAgent));
            entity.setStatus(success ? 0 : 1);
            entity.setMsg(msg);
            entity.setLoginTime(LocalDateTime.now());
            loginLogMapper.insert(entity);
        } catch (Exception e) {
            log.warn("写入登录日志失败: username={}, msg={}", username, e.getMessage());
        }
    }
}
