package com.wejava.system.converter;

import com.wejava.system.model.entity.SysNotice;
import com.wejava.system.model.form.SysNoticeForm;
import com.wejava.system.model.vo.SysNoticeVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SysNoticeConverter {

    SysNoticeVO toVO(SysNotice entity);

    List<SysNoticeVO> toVOList(List<SysNotice> entities);

    @Mapping(target = "noticeId", ignore = true)
    @Mapping(target = "targetIds", ignore = true)
    SysNotice toEntity(SysNoticeForm form);

    @Mapping(target = "noticeId", ignore = true)
    @Mapping(target = "targetIds", ignore = true)
    void update(SysNoticeForm form, @MappingTarget SysNotice entity);
}
