package com.wejava.system.converter;

import com.wejava.system.model.entity.SysUser;
import com.wejava.system.model.form.SysUserCreateForm;
import com.wejava.system.model.form.SysUserUpdateForm;
import com.wejava.system.model.vo.SysUserVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * 用户对象转换。编译期生成实现，无反射开销。
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SysUserConverter {

    /** deptName 由 SQL 关联查询填充，实体里没有 */
    @Mapping(target = "deptName", ignore = true)
    @Mapping(target = "roleIds", ignore = true)
    SysUserVO toVO(SysUser entity);

    List<SysUserVO> toVOList(List<SysUser> entities);

    SysUser toEntity(SysUserCreateForm form);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "password", ignore = true)
    void update(SysUserUpdateForm form, @MappingTarget SysUser entity);
}
