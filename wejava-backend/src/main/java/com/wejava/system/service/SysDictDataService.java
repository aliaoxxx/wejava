package com.wejava.system.service;

import com.wejava.common.core.PageResult;
import com.wejava.system.model.form.SysDictDataForm;
import com.wejava.system.model.query.SysDictDataQuery;
import com.wejava.system.model.vo.SysDictDataVO;
import java.util.List;

public interface SysDictDataService {

    PageResult<SysDictDataVO> page(SysDictDataQuery query);

    SysDictDataVO getById(Long dictCode);

    Long create(SysDictDataForm form);

    void update(SysDictDataForm form);

    void remove(List<Long> dictCodes);

    /** 按类型取字典数据，前端下拉直接用 */
    List<SysDictDataVO> listByType(String dictType);
}
