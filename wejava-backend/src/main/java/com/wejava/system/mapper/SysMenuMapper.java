package com.wejava.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wejava.system.model.entity.SysMenu;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /** 超级管理员：全部目录与菜单 */
    List<SysMenu> selectAllRouters();

    /** 普通用户：按角色关联的目录与菜单 */
    List<SysMenu> selectRoutersByUserId(@Param("userId") Long userId);

    /** 菜单是否已被角色引用 */
    int countRoleRef(@Param("menuId") Long menuId);
}
