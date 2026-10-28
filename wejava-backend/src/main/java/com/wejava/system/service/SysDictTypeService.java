package com.wejava.system.service;

import com.wejava.common.core.PageResult;
import com.wejava.system.model.form.SysDictTypeForm;
import com.wejava.system.model.query.SysDictTypeQuery;
import com.wejava.system.model.vo.SysDictTypeVO;
import java.util.List;

public interface SysDictTypeService {

    PageResult<SysDictTypeVO> page(SysDictTypeQuery query);

    SysDictTypeVO getById(Long dictId);

    Long create(SysDictTypeForm form);

    void update(SysDictTypeForm form);

    void remove(List<Long> dictIds);

    /** 下拉：全部启用中的字典类型 */
    List<SysDictTypeVO> optionSelect();
}
