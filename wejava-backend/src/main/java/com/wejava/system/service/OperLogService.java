package com.wejava.system.service;

import com.wejava.common.core.PageResult;
import com.wejava.system.model.entity.SysOperLog;
import com.wejava.system.model.query.SysOperLogQuery;
import com.wejava.system.model.vo.SysOperLogVO;
import java.util.List;

public interface OperLogService {

    /** 异步落库，不阻塞业务 */
    void record(SysOperLog operLog);

    PageResult<SysOperLogVO> page(SysOperLogQuery query);

    void remove(List<Long> operIds);

    void clean();
}
