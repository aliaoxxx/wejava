package com.wejava.system.converter;

import com.wejava.system.model.entity.SysDictType;
import com.wejava.system.model.form.SysDictTypeForm;
import com.wejava.system.model.vo.SysDictTypeVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SysDictTypeConverter {

    SysDictTypeVO toVO(SysDictType entity);

    List<SysDictTypeVO> toVOList(List<SysDictType> entities);

    @Mapping(target = "dictId", ignore = true)
    SysDictType toEntity(SysDictTypeForm form);

    @Mapping(target = "dictId", ignore = true)
    void update(SysDictTypeForm form, @MappingTarget SysDictType entity);
}
