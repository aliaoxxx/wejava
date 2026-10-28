package com.wejava.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wejava.system.model.entity.SysRole;
import com.wejava.system.model.vo.RoleOptionVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /** 当前用户拥有的角色（数据权限计算用） */
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);

    /** 下拉选项：只取启用中的角色 */
    List<RoleOptionVO> selectOptions();

    /** 角色已分配的菜单ID */
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    /** 角色的自定义数据权限部门ID */
    List<Long> selectDeptIdsByRoleId(@Param("roleId") Long roleId);

    /** 角色是否已分配给用户 */
    int countUsersByRoleId(@Param("roleId") Long roleId);

    void deleteRoleMenus(@Param("roleId") Long roleId);

    void insertRoleMenus(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);

    void deleteRoleDepts(@Param("roleId") Long roleId);

    void insertRoleDepts(@Param("roleId") Long roleId, @Param("deptIds") List<Long> deptIds);

    /** 逻辑删除并改名，避免 uk_role_key 冲突 */
    int softDeleteRole(@Param("roleId") Long roleId);
}
