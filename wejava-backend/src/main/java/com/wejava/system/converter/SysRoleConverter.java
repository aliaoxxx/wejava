package com.wejava.system.converter;

import com.wejava.system.model.entity.SysRole;
import com.wejava.system.model.form.SysRoleCreateForm;
import com.wejava.system.model.form.SysRoleUpdateForm;
import com.wejava.system.model.vo.SysRoleVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SysRoleConverter {

    @Mapping(target = "menuIds", ignore = true)
    SysRoleVO toVO(SysRole entity);

    List<SysRoleVO> toVOList(List<SysRole> entities);

    @Mapping(target = "roleId", ignore = true)
    @Mapping(target = "dataScope", ignore = true)
    SysRole toEntity(SysRoleCreateForm form);

    @Mapping(target = "roleId", ignore = true)
    @Mapping(target = "dataScope", ignore = true)
    void update(SysRoleUpdateForm form, @MappingTarget SysRole entity);
}
