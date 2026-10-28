package com.wejava.system.converter;

import com.wejava.system.model.entity.SysConfig;
import com.wejava.system.model.form.SysConfigForm;
import com.wejava.system.model.vo.SysConfigVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SysConfigConverter {

    SysConfigVO toVO(SysConfig entity);

    List<SysConfigVO> toVOList(List<SysConfig> entities);

    @Mapping(target = "configId", ignore = true)
    SysConfig toEntity(SysConfigForm form);

    @Mapping(target = "configId", ignore = true)
    void update(SysConfigForm form, @MappingTarget SysConfig entity);
}
