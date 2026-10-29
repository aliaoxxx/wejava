package com.wejava.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wejava.common.exception.BizException;
import com.wejava.system.mapper.SysUserMapper;
import com.wejava.system.model.entity.SysUser;
import com.wejava.system.model.form.SysUserCreateForm;
import com.wejava.system.model.query.SysUserQuery;
import com.wejava.system.model.vo.SysUserVO;
import com.wejava.system.service.SysUserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 用户服务：密码处理、唯一性校验、查询不泄露密码 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SysUserServiceImplTest {

    @Autowired
    private SysUserService userService;

    @Autowired
    private SysUserMapper userMapper;

    private SysUserCreateForm form(String username) {
        SysUserCreateForm form = new SysUserCreateForm();
        form.setUsername(username);
        form.setNickname("测试用户");
        form.setPassword("abc123456");
        return form;
    }

    @Test
    void 创建用户时密码以密文存储() {
        Long userId = userService.create(form("zhangsan"));

        SysUser saved = userMapper.selectById(userId);
        assertThat(saved.getPassword()).isNotEqualTo("abc123456");
        assertThat(saved.getPassword()).startsWith("{bcrypt}");
        // 同一明文每次加密结果不同（盐随机）
        Long another = userService.create(form("lisi"));
        assertThat(userMapper.selectById(another).getPassword()).isNotEqualTo(saved.getPassword());
    }

    @Test
    void 用户名重复时拒绝创建() {
        userService.create(form("wangwu"));

        assertThatThrownBy(() -> userService.create(form("wangwu"))).isInstanceOf(BizException.class);
    }

    @Test
    void 查询结果不包含密码字段() {
        userService.create(form("zhaoliu"));

        List<SysUserVO> records = userService.page(new SysUserQuery()).getList();

        assertThat(records).isNotEmpty();
        // SysUserVO 一旦被加上 password 字段，这个断言会失败
        assertThat(SysUserVO.class.getDeclaredFields())
                .noneMatch(field -> field.getName().equals("password"));
    }

    @Test
    void 按用户名模糊查询() {
        userService.create(form("sunjian"));
        userService.create(form("sundan"));

        SysUserQuery query = new SysUserQuery();
        query.setUsername("sun");
        assertThat(userService.page(query).getTotal()).isEqualTo(2);
    }

    @Test
    void 默认状态为正常_性别为未知() {
        Long userId = userService.create(form("zhouqi"));

        SysUser saved = userMapper.selectById(userId);
        assertThat(saved.getStatus()).isZero();
        assertThat(saved.getSex()).isEqualTo("2");
    }
}
