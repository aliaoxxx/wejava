package com.wejava.system.service;

import com.wejava.common.core.PageResult;
import com.wejava.system.model.form.SysNoticeForm;
import com.wejava.system.model.query.SysNoticeQuery;
import com.wejava.system.model.vo.SysNoticeVO;
import java.util.List;

public interface SysNoticeService {

    PageResult<SysNoticeVO> page(SysNoticeQuery query);

    SysNoticeVO getById(Long noticeId);

    Long create(SysNoticeForm form);

    void update(SysNoticeForm form);

    void remove(List<Long> noticeIds);
}
