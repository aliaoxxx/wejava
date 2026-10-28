package com.wejava.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wejava.common.exception.BizException;
import com.wejava.common.exception.ErrorCode;
import com.wejava.system.mapper.SysUserMapper;
import com.wejava.system.model.entity.SysUser;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 把「接收范围」解析成具体用户 ID 列表。
 * 后台发消息和公告定向投递共用这一份逻辑。
 */
@Component
@RequiredArgsConstructor
public class MessageTargetResolver {

    public static final int TARGET_ALL = 1;
    public static final int TARGET_ROLE = 2;
    public static final int TARGET_DEPT = 3;
    public static final int TARGET_USER = 4;

    private final SysUserMapper userMapper;

    public List<Long> resolve(int targetType, List<Long> targetIds) {
        if (targetType == TARGET_ALL) {
            return userMapper
                    .selectList(Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getStatus, 0)
                            .select(SysUser::getUserId))
                    .stream()
                    .map(SysUser::getUserId)
                    .toList();
        }

        if (targetIds == null || targetIds.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "请选择接收对象");
        }

        Set<Long> result = new LinkedHashSet<>();
        switch (targetType) {
            case TARGET_ROLE -> targetIds.forEach(roleId -> result.addAll(userMapper.selectUserIdsByRoleId(roleId)));
            case TARGET_DEPT -> targetIds.forEach(deptId -> result.addAll(userMapper.selectUserIdsByDeptId(deptId)));
            case TARGET_USER -> result.addAll(targetIds);
            default -> throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "接收范围不合法");
        }
        return new ArrayList<>(result);
    }
}
