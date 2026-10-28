package com.wejava;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wejava.system.mapper.SysUserMapper;
import com.wejava.system.model.entity.SysUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * M1-1 风险验证：Boot 4.1.2 + MyBatis-Plus 3.5.18 + jsqlparser 能否正常分页。
 */
@SpringBootTest
@ActiveProfiles("test")
class PaginationTest {

    @Autowired
    private SysUserMapper userMapper;

    @Test
    void paginationWorks() {
        for (int i = 0; i < 25; i++) {
            SysUser user = new SysUser();
            user.setUsername("user" + i);
            user.setNickname("nick" + i);
            user.setStatus(0);
            userMapper.insert(user);
        }

        Page<SysUser> page = new Page<>(2, 10);
        Page<SysUser> result = userMapper.selectPage(page, null);

        assertThat(result.getTotal()).isEqualTo(25);
        assertThat(result.getRecords()).hasSize(10);
        assertThat(result.getCurrent()).isEqualTo(2);
    }
}
