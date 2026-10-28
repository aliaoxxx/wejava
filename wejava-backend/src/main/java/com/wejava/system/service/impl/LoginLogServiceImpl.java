package com.wejava.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wejava.common.config.AsyncConfig;
import com.wejava.common.core.PageResult;
import com.wejava.common.util.UserAgentUtils;
import com.wejava.system.converter.SysLoginLogConverter;
import com.wejava.system.mapper.SysLoginLogMapper;
import com.wejava.system.model.entity.SysLoginLog;
import com.wejava.system.model.query.SysLoginLogQuery;
import com.wejava.system.model.vo.SysLoginLogVO;
import com.wejava.system.service.LoginLogService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 登录日志落库。异步执行：写库失败或变慢都不影响登录响应。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl implements LoginLogService {

    private final SysLoginLogMapper loginLogMapper;
    private final SysLoginLogConverter converter;

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

    @Override
    public PageResult<SysLoginLogVO> page(SysLoginLogQuery query) {
        Page<SysLoginLog> page = query.toPage();
        Page<SysLoginLog> result = loginLogMapper.selectPage(
                page,
                Wrappers.<SysLoginLog>lambdaQuery()
                        .like(StringUtils.hasText(query.getUsername()), SysLoginLog::getUsername, query.getUsername())
                        .like(StringUtils.hasText(query.getIpaddr()), SysLoginLog::getIpaddr, query.getIpaddr())
                        .eq(query.getStatus() != null, SysLoginLog::getStatus, query.getStatus())
                        .ge(query.getBeginTime() != null, SysLoginLog::getLoginTime, query.getBeginTime())
                        .le(query.getEndTime() != null, SysLoginLog::getLoginTime, query.getEndTime())
                        .orderByDesc(SysLoginLog::getLoginTime));
        return PageResult.of(result.convert(converter::toVO));
    }

    @Override
    public void remove(List<Long> infoIds) {
        loginLogMapper.deleteByIds(infoIds);
    }

    @Override
    public void clean() {
        // BlockAttackInnerInterceptor 拦截无 WHERE 的删除，给一个恒真条件
        loginLogMapper.delete(Wrappers.<SysLoginLog>lambdaQuery().ge(SysLoginLog::getInfoId, 0L));
    }
}
