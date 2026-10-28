package com.wejava;

import static org.assertj.core.api.Assertions.assertThat;

import com.wejava.system.converter.SysUserConverter;
import com.wejava.system.model.entity.SysUser;
import com.wejava.system.model.vo.SysUserVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 验证 MapStruct 在 Boot 4 + JDK 17 + Lombok 下的注解处理链路是否正常。
 */
@SpringBootTest
@ActiveProfiles("test")
class ConverterTest {

    @Autowired
    private SysUserConverter converter;

    @Test
    void entityToVoShouldDropSensitiveFields() {
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setUsername("admin");
        user.setNickname("超级管理员");
        user.setPassword("{bcrypt}xxx");
        user.setStatus(0);

        SysUserVO vo = converter.toVO(user);

        assertThat(vo.getUserId()).isEqualTo(1L);
        assertThat(vo.getUsername()).isEqualTo("admin");
        assertThat(vo.getNickname()).isEqualTo("超级管理员");
        assertThat(vo.getStatus()).isZero();
    }
}
