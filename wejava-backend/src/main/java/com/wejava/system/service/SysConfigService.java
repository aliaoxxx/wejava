package com.wejava.system.service;

import com.wejava.common.core.PageResult;
import com.wejava.system.model.form.SysConfigForm;
import com.wejava.system.model.query.SysConfigQuery;
import com.wejava.system.model.vo.SysConfigVO;
import java.util.List;

public interface SysConfigService {

    PageResult<SysConfigVO> page(SysConfigQuery query);

    SysConfigVO getById(Long configId);

    Long create(SysConfigForm form);

    void update(SysConfigForm form);

    void remove(List<Long> configIds);

    String getValueByKey(String configKey);
}
