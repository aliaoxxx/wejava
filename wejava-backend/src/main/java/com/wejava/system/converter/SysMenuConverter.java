package com.wejava.system.converter;

import com.wejava.system.model.entity.SysMenu;
import com.wejava.system.model.form.SysMenuForm;
import com.wejava.system.model.vo.SysMenuVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SysMenuConverter {

    @Mapping(target = "children", ignore = true)
    SysMenuVO toVO(SysMenu entity);

    List<SysMenuVO> toVOList(List<SysMenu> entities);

    @Mapping(target = "menuId", ignore = true)
    SysMenu toEntity(SysMenuForm form);

    @Mapping(target = "menuId", ignore = true)
    void update(SysMenuForm form, @MappingTarget SysMenu entity);
}
