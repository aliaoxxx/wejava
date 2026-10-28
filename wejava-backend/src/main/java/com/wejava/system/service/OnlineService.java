package com.wejava.system.service;

import com.wejava.common.core.PageResult;
import com.wejava.system.model.vo.OnlineUserVO;

public interface OnlineService {

    PageResult<OnlineUserVO> page(long pageNum, long pageSize);

    void kick(String jti);
}
