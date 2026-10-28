package com.wejava.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wejava.common.exception.BizException;
import com.wejava.common.exception.ErrorCode;
import com.wejava.security.model.AuthUser;
import com.wejava.security.model.LoginSession;
import com.wejava.system.mapper.SysUserMapper;
import com.wejava.system.model.entity.SysUser;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 登录时装配用户、角色、权限。整个流程只查 3 次库，避免 N+1。
 * <p>
 * 放在 system 而不是 security：它依赖 SysUserMapper，属于业务侧。
 */
@Service
@RequiredArgsConstructor
public class LoginUserDetailsService implements UserDetailsService {

    public static final String SUPER_ADMIN_ROLE = "admin";

    private final SysUserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        return build(user);
    }

    /**
     * 刷新 Token 时按 userId 重建身份。用户被删除或停用时抛业务异常，不让刷新继续。
     */
    public AuthUser loadByUserId(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_LOGIN);
        }
        return build(user);
    }

    private AuthUser build(SysUser user) {
        List<String> roleKeys = userMapper.selectRoleKeysByUserId(user.getUserId());
        List<String> perms = userMapper.selectPermsByUserId(user.getUserId());

        LoginSession session = new LoginSession();
        session.setUserId(user.getUserId());
        session.setDeptId(user.getDeptId());
        session.setUsername(user.getUsername());
        session.setNickname(user.getNickname());
        session.setSuperAdmin(roleKeys.contains(SUPER_ADMIN_ROLE));
        session.setRoles(new LinkedHashSet<>(roleKeys));
        session.setPerms(new LinkedHashSet<>(perms));

        return new AuthUser(session, user.getPassword(), user.getStatus());
    }
}
