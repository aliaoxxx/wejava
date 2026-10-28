package com.wejava.system.converter;

import com.wejava.system.model.entity.SysPost;
import com.wejava.system.model.form.SysPostForm;
import com.wejava.system.model.vo.SysPostVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SysPostConverter {

    SysPostVO toVO(SysPost entity);

    List<SysPostVO> toVOList(List<SysPost> entities);

    @Mapping(target = "postId", ignore = true)
    SysPost toEntity(SysPostForm form);

    @Mapping(target = "postId", ignore = true)
    void update(SysPostForm form, @MappingTarget SysPost entity);
}
