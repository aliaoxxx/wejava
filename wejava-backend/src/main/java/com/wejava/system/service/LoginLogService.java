package com.wejava.system.service;

import com.wejava.common.core.PageResult;
import com.wejava.system.model.query.SysLoginLogQuery;
import com.wejava.system.model.vo.SysLoginLogVO;
import java.util.List;

/**
 * 登录日志：异步落库 + 查询 / 清理。
 */
public interface LoginLogService {

    void record(String username, String ip, String userAgent, boolean success, String msg);

    PageResult<SysLoginLogVO> page(SysLoginLogQuery query);

    void remove(List<Long> infoIds);

    void clean();
}
