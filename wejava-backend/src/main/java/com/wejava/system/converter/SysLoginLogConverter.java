package com.wejava.system.converter;

import com.wejava.system.model.entity.SysLoginLog;
import com.wejava.system.model.vo.SysLoginLogVO;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SysLoginLogConverter {

    SysLoginLogVO toVO(SysLoginLog entity);

    List<SysLoginLogVO> toVOList(List<SysLoginLog> entities);
}
