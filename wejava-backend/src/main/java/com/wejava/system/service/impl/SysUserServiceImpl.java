package com.wejava.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wejava.common.annotation.DataScope;
import com.wejava.common.core.PageResult;
import com.wejava.common.exception.BizException;
import com.wejava.common.exception.ErrorCode;
import com.wejava.security.SecurityUtils;
import com.wejava.security.jwt.SessionStore;
import com.wejava.system.converter.SysUserConverter;
import com.wejava.system.mapper.SysDeptMapper;
import com.wejava.system.mapper.SysUserMapper;
import com.wejava.system.model.entity.SysDept;
import com.wejava.system.model.entity.SysUser;
import com.wejava.system.model.form.ProfilePasswordForm;
import com.wejava.system.model.form.ProfileUpdateForm;
import com.wejava.system.model.form.SysUserCreateForm;
import com.wejava.system.model.form.SysUserUpdateForm;
import com.wejava.system.model.query.SysUserQuery;
import com.wejava.system.model.vo.SysUserVO;
import com.wejava.system.service.SysUserService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户管理实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    /** 系统内置超级管理员，不可删除、不可停用 */
    private static final long SUPER_ADMIN_ID = 1L;

    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final SysUserConverter converter;
    private final PasswordEncoder passwordEncoder;
    private final SessionStore sessionStore;

    @Override
    @DataScope(deptAlias = "u", userAlias = "u")
    public PageResult<SysUserVO> page(SysUserQuery query) {
        Page<SysUserVO> page = query.toPage();
        IPage<SysUserVO> result = userMapper.selectUserPage(page, query);
        return PageResult.of(result);
    }

    @Override
    @DataScope(deptAlias = "u", userAlias = "u")
    public List<SysUserVO> exportList(SysUserQuery query) {
        Page<SysUserVO> page = new Page<>(1, 10000);
        return userMapper.selectUserPage(page, query).getRecords();
    }

    @Override
    public SysUserVO getById(Long userId) {
        SysUser user = requireUser(userId);
        SysUserVO vo = converter.toVO(user);
        vo.setRoleIds(userMapper.selectRoleIdsByUserId(userId));
        if (user.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(user.getDeptId());
            vo.setDeptName(dept == null ? null : dept.getDeptName());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysUserCreateForm form) {
        if (userMapper.exists(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, form.getUsername()))) {
            throw new BizException(ErrorCode.DATA_ALREADY_EXISTS);
        }
        SysUser entity = converter.toEntity(form);
        entity.setUserId(null);
        entity.setPassword(passwordEncoder.encode(form.getPassword()));
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        if (entity.getSex() == null) {
            entity.setSex("2");
        }
        userMapper.insert(entity);
        replaceRoles(entity.getUserId(), form.getRoleIds());
        return entity.getUserId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysUserUpdateForm form) {
        if (Objects.equals(form.getUserId(), SUPER_ADMIN_ID)
                && Integer.valueOf(1).equals(form.getStatus())) {
            throw new BizException(ErrorCode.CANNOT_OPERATE_ADMIN);
        }
        SysUser entity = requireUser(form.getUserId());
        boolean disabling = Integer.valueOf(1).equals(form.getStatus())
                && !Integer.valueOf(1).equals(entity.getStatus());

        converter.update(form, entity);
        userMapper.updateById(entity);

        if (form.getRoleIds() != null) {
            replaceRoles(form.getUserId(), form.getRoleIds());
        }
        if (disabling) {
            sessionStore.kickUser(form.getUserId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(List<Long> userIds) {
        Long currentUserId = SecurityUtils.currentUserId();
        for (Long userId : userIds) {
            if (userId == null) {
                continue;
            }
            if (userId == SUPER_ADMIN_ID) {
                throw new BizException(ErrorCode.CANNOT_OPERATE_ADMIN);
            }
            if (userId.equals(currentUserId)) {
                throw new BizException(ErrorCode.CANNOT_OPERATE_SELF);
            }
            if (userMapper.selectById(userId) == null) {
                continue;
            }
            userMapper.softDeleteUser(userId);
            userMapper.deleteUserRoles(userId);
            sessionStore.kickUser(userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, String password) {
        SysUser entity = requireUser(userId);
        if (Objects.equals(userId, SUPER_ADMIN_ID) && !Objects.equals(SecurityUtils.currentUserId(), SUPER_ADMIN_ID)) {
            throw new BizException(ErrorCode.CANNOT_OPERATE_ADMIN);
        }
        SysUser update = new SysUser();
        update.setUserId(userId);
        update.setPassword(passwordEncoder.encode(password));
        update.setPwdUpdateTime(LocalDateTime.now());
        userMapper.updateById(update);
        // 改密后强制下线
        sessionStore.kickUser(userId);
        log.info("重置密码 userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long userId, Integer status) {
        requireUser(userId);
        if (userId == SUPER_ADMIN_ID) {
            throw new BizException(ErrorCode.CANNOT_OPERATE_ADMIN);
        }
        SysUser update = new SysUser();
        update.setUserId(userId);
        update.setStatus(status);
        userMapper.updateById(update);
        if (Integer.valueOf(1).equals(status)) {
            sessionStore.kickUser(userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeOwnPassword(ProfilePasswordForm form) {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        SysUser user = requireUser(userId);
        if (!passwordEncoder.matches(form.getOldPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "原密码不正确");
        }
        if (form.getOldPassword().equals(form.getNewPassword())) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "新密码不能与原密码相同");
        }

        SysUser update = new SysUser();
        update.setUserId(userId);
        update.setPassword(passwordEncoder.encode(form.getNewPassword()));
        update.setPwdUpdateTime(LocalDateTime.now());
        userMapper.updateById(update);

        // 改密后所有会话失效，需要重新登录
        sessionStore.kickUser(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOwnProfile(ProfileUpdateForm form) {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        requireUser(userId);

        SysUser update = new SysUser();
        update.setUserId(userId);
        update.setNickname(form.getNickname());
        update.setPhone(form.getPhone());
        update.setEmail(form.getEmail());
        userMapper.updateById(update);
    }

    private SysUser requireUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return user;
    }

    private void replaceRoles(Long userId, List<Long> roleIds) {
        userMapper.deleteUserRoles(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            userMapper.insertUserRoles(userId, roleIds);
        }
    }
}
