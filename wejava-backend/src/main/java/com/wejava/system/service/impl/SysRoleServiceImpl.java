package com.wejava.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wejava.common.core.PageResult;
import com.wejava.common.exception.BizException;
import com.wejava.common.exception.ErrorCode;
import com.wejava.system.converter.SysRoleConverter;
import com.wejava.system.mapper.SysRoleMapper;
import com.wejava.system.model.entity.SysRole;
import com.wejava.system.model.form.DataScopeForm;
import com.wejava.system.model.form.SysRoleCreateForm;
import com.wejava.system.model.form.SysRoleUpdateForm;
import com.wejava.system.model.query.SysRoleQuery;
import com.wejava.system.model.vo.RoleOptionVO;
import com.wejava.system.model.vo.SysRoleVO;
import com.wejava.system.service.SysRoleService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    /** 内置超级管理员角色，不可删除、不可停用、不可改标识 */
    private static final long ADMIN_ROLE_ID = 1L;

    private final SysRoleMapper roleMapper;
    private final SysRoleConverter converter;

    @Override
    public List<RoleOptionVO> optionSelect() {
        return roleMapper.selectOptions();
    }

    @Override
    public PageResult<SysRoleVO> page(SysRoleQuery query) {
        Page<SysRole> page = query.toPage();
        Page<SysRole> result = roleMapper.selectPage(
                page,
                Wrappers.<SysRole>lambdaQuery()
                        .like(StringUtils.hasText(query.getRoleName()), SysRole::getRoleName, query.getRoleName())
                        .like(StringUtils.hasText(query.getRoleKey()), SysRole::getRoleKey, query.getRoleKey())
                        .eq(query.getStatus() != null, SysRole::getStatus, query.getStatus())
                        .orderByAsc(SysRole::getRoleSort));
        return PageResult.of(result.convert(converter::toVO));
    }

    @Override
    public SysRoleVO getById(Long roleId) {
        SysRoleVO vo = converter.toVO(requireRole(roleId));
        vo.setMenuIds(roleMapper.selectMenuIdsByRoleId(roleId));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysRoleCreateForm form) {
        if (roleKeyExists(form.getRoleKey(), null)) {
            throw new BizException(ErrorCode.DATA_ALREADY_EXISTS);
        }
        SysRole entity = converter.toEntity(form);
        entity.setRoleId(null);
        entity.setDataScope("5");
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        if (entity.getRoleSort() == null) {
            entity.setRoleSort(0);
        }
        roleMapper.insert(entity);
        replaceMenus(entity.getRoleId(), form.getMenuIds());
        return entity.getRoleId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysRoleUpdateForm form) {
        SysRole entity = requireRole(form.getRoleId());
        if (Objects.equals(form.getRoleId(), ADMIN_ROLE_ID)) {
            if (!Objects.equals(entity.getRoleKey(), form.getRoleKey())) {
                throw new BizException(ErrorCode.CANNOT_OPERATE_ADMIN);
            }
            if (Integer.valueOf(1).equals(form.getStatus())) {
                throw new BizException(ErrorCode.CANNOT_OPERATE_ADMIN);
            }
        } else if (roleKeyExists(form.getRoleKey(), form.getRoleId())) {
            throw new BizException(ErrorCode.DATA_ALREADY_EXISTS);
        }

        converter.update(form, entity);
        roleMapper.updateById(entity);

        if (form.getMenuIds() != null) {
            replaceMenus(form.getRoleId(), form.getMenuIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(List<Long> roleIds) {
        for (Long roleId : roleIds) {
            if (roleId == null) {
                continue;
            }
            if (roleId == ADMIN_ROLE_ID) {
                throw new BizException(ErrorCode.CANNOT_OPERATE_ADMIN);
            }
            if (roleMapper.countUsersByRoleId(roleId) > 0) {
                throw new BizException(ErrorCode.ROLE_HAS_USER);
            }
            roleMapper.softDeleteRole(roleId);
            roleMapper.deleteRoleMenus(roleId);
            roleMapper.deleteRoleDepts(roleId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long roleId, Integer status) {
        requireRole(roleId);
        if (roleId == ADMIN_ROLE_ID) {
            throw new BizException(ErrorCode.CANNOT_OPERATE_ADMIN);
        }
        SysRole update = new SysRole();
        update.setRoleId(roleId);
        update.setStatus(status);
        roleMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDataScope(DataScopeForm form) {
        requireRole(form.getRoleId());
        if (Objects.equals(form.getRoleId(), ADMIN_ROLE_ID)) {
            throw new BizException(ErrorCode.CANNOT_OPERATE_ADMIN);
        }
        if ("2".equals(form.getDataScope())
                && (form.getDeptIds() == null || form.getDeptIds().isEmpty())) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "自定义数据权限必须选择部门");
        }

        SysRole update = new SysRole();
        update.setRoleId(form.getRoleId());
        update.setDataScope(form.getDataScope());
        roleMapper.updateById(update);

        roleMapper.deleteRoleDepts(form.getRoleId());
        if ("2".equals(form.getDataScope())) {
            roleMapper.insertRoleDepts(form.getRoleId(), form.getDeptIds());
        }
    }

    @Override
    public List<Long> listDeptIds(Long roleId) {
        return roleMapper.selectDeptIdsByRoleId(roleId);
    }

    private SysRole requireRole(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return role;
    }

    private boolean roleKeyExists(String roleKey, Long excludeRoleId) {
        return roleMapper.exists(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleKey, roleKey)
                .ne(excludeRoleId != null, SysRole::getRoleId, excludeRoleId));
    }

    private void replaceMenus(Long roleId, List<Long> menuIds) {
        roleMapper.deleteRoleMenus(roleId);
        if (menuIds != null && !menuIds.isEmpty()) {
            roleMapper.insertRoleMenus(roleId, menuIds);
        }
    }
}
