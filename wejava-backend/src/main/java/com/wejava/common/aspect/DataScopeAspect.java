package com.wejava.common.aspect;

import com.wejava.common.annotation.DataScope;
import com.wejava.common.interceptor.DataScopeContext;
import com.wejava.security.SecurityUtils;
import com.wejava.security.model.LoginSession;
import com.wejava.system.mapper.SysRoleMapper;
import com.wejava.system.model.entity.SysRole;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 数据权限切面：把角色的 data_scope 翻译成 SQL 片段放进 ThreadLocal。
 * <p>
 * 多角色合并规则：取最宽松的一个（数字越小越宽松：1 全部 > 2 自定义 > 4 本部门及以下 > 3 本部门 > 5 仅本人）。
 */
@Slf4j
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class DataScopeAspect {

    /** create_by 存的是用户名，拼进 SQL 前必须过白名单 */
    private static final java.util.regex.Pattern SAFE_USERNAME =
            java.util.regex.Pattern.compile("^[a-zA-Z0-9_@.\\-]{1,64}$");

    private final SysRoleMapper roleMapper;

    @Around("@annotation(dataScope)")
    public Object around(ProceedingJoinPoint joinPoint, DataScope dataScope) throws Throwable {
        try {
            DataScopeContext.set(build(dataScope));
            return joinPoint.proceed();
        } finally {
            DataScopeContext.clear();
        }
    }

    private String build(DataScope annotation) {
        LoginSession session = SecurityUtils.currentSession();
        // 未登录、超级管理员：不限制
        if (session == null || session.isSuperAdmin()) {
            return null;
        }

        List<SysRole> roles = roleMapper.selectRolesByUserId(session.getUserId());
        if (roles.isEmpty()) {
            // 没有任何角色 → 查不到任何数据，绝不能退化成"不加条件"
            return "1 = 0";
        }

        int scope = roles.stream()
                .map(SysRole::getDataScope)
                .filter(StringUtils::hasText)
                .map(value -> {
                    try {
                        return Integer.parseInt(value.trim());
                    } catch (NumberFormatException e) {
                        return 5;
                    }
                })
                .min(Integer::compareTo)
                .orElse(5);

        return switch (scope) {
            case 1 -> null;
            case 2 -> {
                String roleIds = roles.stream()
                        .map(role -> String.valueOf(role.getRoleId()))
                        .collect(Collectors.joining(","));
                yield annotation.deptAlias() + ".dept_id IN (SELECT dept_id FROM sys_role_dept" + " WHERE role_id IN ("
                        + roleIds + "))";
            }
            case 3 -> deptEquals(annotation.deptAlias(), session.getDeptId());
            case 4 -> deptAndChildren(annotation.deptAlias(), session.getDeptId());
            default -> selfOnly(annotation, session);
        };
    }

    private String deptEquals(String alias, Long deptId) {
        if (deptId == null) {
            return "1 = 0";
        }
        return alias + ".dept_id = " + deptId;
    }

    private String deptAndChildren(String alias, Long deptId) {
        if (deptId == null) {
            return "1 = 0";
        }
        return alias + ".dept_id IN (SELECT dept_id FROM sys_dept" + " WHERE dept_id = " + deptId + " OR FIND_IN_SET("
                + deptId + ", ancestors))";
    }

    private String selfOnly(DataScope annotation, LoginSession session) {
        if (annotation.selfByCreateBy()) {
            String username = session.getUsername();
            // 这串内容会拼进 SQL，必须严格校验；不匹配说明数据异常，直接拒绝而不是放行
            if (username == null || !SAFE_USERNAME.matcher(username).matches()) {
                throw new IllegalStateException("用户名包含非法字符，无法生成数据权限条件");
            }
            return "create_by = '" + username + "'";
        }
        return annotation.userAlias() + ".user_id = " + Objects.requireNonNull(session.getUserId());
    }
}
