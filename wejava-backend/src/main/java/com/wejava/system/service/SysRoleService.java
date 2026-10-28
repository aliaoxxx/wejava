package com.wejava.system.service;

import com.wejava.common.core.PageResult;
import com.wejava.system.model.form.DataScopeForm;
import com.wejava.system.model.form.SysRoleCreateForm;
import com.wejava.system.model.form.SysRoleUpdateForm;
import com.wejava.system.model.query.SysRoleQuery;
import com.wejava.system.model.vo.RoleOptionVO;
import com.wejava.system.model.vo.SysRoleVO;
import java.util.List;

/**
 * 角色管理。
 */
public interface SysRoleService {

    List<RoleOptionVO> optionSelect();

    PageResult<SysRoleVO> page(SysRoleQuery query);

    SysRoleVO getById(Long roleId);

    Long create(SysRoleCreateForm form);

    void update(SysRoleUpdateForm form);

    void remove(List<Long> roleIds);

    void changeStatus(Long roleId, Integer status);

    void updateDataScope(DataScopeForm form);
}
