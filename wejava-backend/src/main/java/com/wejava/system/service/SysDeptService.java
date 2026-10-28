package com.wejava.system.service;

import com.wejava.system.model.form.SysDeptForm;
import com.wejava.system.model.query.SysDeptQuery;
import com.wejava.system.model.vo.SysDeptVO;
import java.util.List;

/**
 * 部门管理（树形）。
 */
public interface SysDeptService {

    List<SysDeptVO> tree(SysDeptQuery query);

    SysDeptVO getById(Long deptId);

    Long create(SysDeptForm form);

    void update(SysDeptForm form);

    void remove(Long deptId);

    List<SysDeptVO> treeSelect();
}
