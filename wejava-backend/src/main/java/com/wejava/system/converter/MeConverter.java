package com.wejava.system.converter;

import com.wejava.security.model.LoginSession;
import com.wejava.system.model.vo.MeVO;
import org.mapstruct.Mapper;

/**
 * 会话 → 当前用户信息。纯映射，不查库。
 */
@Mapper(componentModel = "spring")
public interface MeConverter {

    MeVO toVO(LoginSession session);
}
