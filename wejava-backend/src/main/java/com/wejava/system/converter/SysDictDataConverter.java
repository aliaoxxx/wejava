package com.wejava.system.converter;

import com.wejava.system.model.entity.SysDictData;
import com.wejava.system.model.form.SysDictDataForm;
import com.wejava.system.model.vo.SysDictDataVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SysDictDataConverter {

    SysDictDataVO toVO(SysDictData entity);

    List<SysDictDataVO> toVOList(List<SysDictData> entities);

    @Mapping(target = "dictCode", ignore = true)
    SysDictData toEntity(SysDictDataForm form);

    @Mapping(target = "dictCode", ignore = true)
    void update(SysDictDataForm form, @MappingTarget SysDictData entity);
}
