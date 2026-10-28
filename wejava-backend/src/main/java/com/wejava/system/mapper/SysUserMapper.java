package com.wejava.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wejava.system.model.entity.SysUser;
import com.wejava.system.model.query.SysUserQuery;
import com.wejava.system.model.vo.SysUserVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户 Mapper。跨表查询写在 XML 里（resources/mapper/system/SysUserMapper.xml）。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 某个角色下的启用用户 */
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);

    /** 部门（含子部门）下的启用用户，deptId 用 FIND_IN_SET 匹配 ancestors */
    List<Long> selectUserIdsByDeptId(@Param("deptId") Long deptId);

    /** 分页查询（关联部门名称） */
    IPage<SysUserVO> selectUserPage(IPage<SysUserVO> page, @Param("q") SysUserQuery query);

    /** 用户拥有的角色ID */
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /** 用户的权限字符（登录时装配） */
    List<String> selectPermsByUserId(@Param("userId") Long userId);

    /** 用户的角色标识（登录时装配） */
    List<String> selectRoleKeysByUserId(@Param("userId") Long userId);

    /** 逻辑删除并改名，避免唯一索引冲突 */
    int softDeleteUser(@Param("userId") Long userId);

    void deleteUserRoles(@Param("userId") Long userId);

    void insertUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);
}
