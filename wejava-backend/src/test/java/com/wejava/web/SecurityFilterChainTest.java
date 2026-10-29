package com.wejava.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wejava.common.interceptor.ConcurrencyInterceptor;
import com.wejava.security.jwt.JwtService;
import com.wejava.security.jwt.SessionStore;
import com.wejava.security.model.LoginSession;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 安全过滤链的端到端行为：白名单放行、无票据拦截、带票据放行。
 * <p>
 * SessionStore 用 mock 替换，避免测试依赖 Redis。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityFilterChainTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private SessionStore sessionStore;

    /** 并发控制拦截器依赖 Redis，测试里放行 */
    @MockitoBean
    private ConcurrencyInterceptor concurrencyInterceptor;

    @BeforeEach
    void setUp() throws Exception {
        when(concurrencyInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    private String tokenOf(LoginSession session) {
        String jti = "test-jti-" + session.getUserId();
        session.setJti(jti);
        when(sessionStore.getAccess(jti)).thenReturn(session);
        return jwtService.createAccessToken(session, jti);
    }

    @Test
    void 白名单接口无需登录即可访问() throws Exception {
        mockMvc.perform(get("/ping")).andExpect(status().isOk());
    }

    @Test
    void 未携带令牌访问受保护接口返回401() throws Exception {
        mockMvc.perform(get("/system/dept/list"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void 令牌无效时返回401() throws Exception {
        mockMvc.perform(get("/system/dept/list").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 会话已失效时返回401() throws Exception {
        LoginSession session = new LoginSession();
        session.setUserId(1L);
        session.setUsername("admin");
        String jti = "expired-jti";
        session.setJti(jti);
        // 不 mock SessionStore → getAccess 返回 null，模拟已登出
        String token = jwtService.createAccessToken(session, jti);

        mockMvc.perform(get("/system/dept/list").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 有效会话可以访问受保护接口() throws Exception {
        LoginSession session = new LoginSession();
        session.setUserId(1L);
        session.setUsername("admin");
        session.setSuperAdmin(true);
        session.setPerms(Set.of("*:*:*"));

        mockMvc.perform(get("/system/dept/list").header("Authorization", "Bearer " + tokenOf(session)))
                .andExpect(status().isOk());
    }
}
