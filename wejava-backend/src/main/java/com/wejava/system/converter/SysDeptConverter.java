package com.wejava.system.converter;

import com.wejava.system.model.entity.SysDept;
import com.wejava.system.model.form.SysDeptForm;
import com.wejava.system.model.vo.SysDeptVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SysDeptConverter {

    @Mapping(target = "children", ignore = true)
    SysDeptVO toVO(SysDept entity);

    List<SysDeptVO> toVOList(List<SysDept> entities);

    @Mapping(target = "deptId", ignore = true)
    @Mapping(target = "ancestors", ignore = true)
    SysDept toEntity(SysDeptForm form);

    @Mapping(target = "deptId", ignore = true)
    @Mapping(target = "ancestors", ignore = true)
    void update(SysDeptForm form, @MappingTarget SysDept entity);
}
