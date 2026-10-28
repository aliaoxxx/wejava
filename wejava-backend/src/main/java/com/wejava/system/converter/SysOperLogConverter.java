package com.wejava.system.converter;

import com.wejava.system.model.entity.SysOperLog;
import com.wejava.system.model.vo.SysOperLogVO;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SysOperLogConverter {

    SysOperLogVO toVO(SysOperLog entity);

    List<SysOperLogVO> toVOList(List<SysOperLog> entities);
}
