package com.wejava.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wejava.common.config.AsyncConfig;
import com.wejava.common.core.PageResult;
import com.wejava.system.converter.SysOperLogConverter;
import com.wejava.system.mapper.SysOperLogMapper;
import com.wejava.system.model.entity.SysOperLog;
import com.wejava.system.model.query.SysOperLogQuery;
import com.wejava.system.model.vo.SysOperLogVO;
import com.wejava.system.service.OperLogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperLogServiceImpl implements OperLogService {

    private final SysOperLogMapper operLogMapper;
    private final SysOperLogConverter converter;

    @Async(AsyncConfig.LOGIN_LOG_EXECUTOR)
    @Override
    public void record(SysOperLog operLog) {
        try {
            operLogMapper.insert(operLog);
        } catch (Exception e) {
            log.warn("写入操作日志失败: title={}, msg={}", operLog.getTitle(), e.getMessage());
        }
    }

    @Override
    public PageResult<SysOperLogVO> page(SysOperLogQuery query) {
        Page<SysOperLog> page = query.toPage();
        Page<SysOperLog> result = operLogMapper.selectPage(
                page,
                Wrappers.<SysOperLog>lambdaQuery()
                        .like(StringUtils.hasText(query.getTitle()), SysOperLog::getTitle, query.getTitle())
                        .like(StringUtils.hasText(query.getOperName()), SysOperLog::getOperName, query.getOperName())
                        .eq(query.getBusinessType() != null, SysOperLog::getBusinessType, query.getBusinessType())
                        .eq(query.getStatus() != null, SysOperLog::getStatus, query.getStatus())
                        .ge(query.getBeginTime() != null, SysOperLog::getOperTime, query.getBeginTime())
                        .le(query.getEndTime() != null, SysOperLog::getOperTime, query.getEndTime())
                        .orderByDesc(SysOperLog::getOperTime));
        return PageResult.of(result.convert(converter::toVO));
    }

    @Override
    public void remove(List<Long> operIds) {
        operLogMapper.deleteByIds(operIds);
    }

    @Override
    public void clean() {
        // BlockAttackInnerInterceptor 会拦截无 WHERE 的删除，这里给一个恒真条件
        operLogMapper.delete(Wrappers.<SysOperLog>lambdaQuery().ge(SysOperLog::getOperId, 0L));
    }
}
