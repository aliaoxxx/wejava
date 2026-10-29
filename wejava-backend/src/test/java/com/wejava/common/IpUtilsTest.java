package com.wejava.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

class IpUtilsTest {

    private HttpServletRequest requestWith(String xff, String xRealIp, String remoteAddr) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(xff);
        when(request.getHeader("X-Real-IP")).thenReturn(xRealIp);
        when(request.getRemoteAddr()).thenReturn(remoteAddr);
        return request;
    }

    @Test
    void 优先使用代理覆盖写入的XRealIP() {
        assertThat(IpUtils.getIp(requestWith("1.1.1.1", "2.2.2.2", "9.9.9.9"))).isEqualTo("2.2.2.2");
    }

    @Test
    void 多层转发取最后一段_前面的段可被客户端伪造() {
        assertThat(IpUtils.getIp(requestWith("6.6.6.6, 1.1.1.1, 2.2.2.2", null, "9.9.9.9")))
                .isEqualTo("2.2.2.2");
    }

    @Test
    void 单层转发去掉首尾空格() {
        assertThat(IpUtils.getIp(requestWith("  1.1.1.1  ", null, "9.9.9.9"))).isEqualTo("1.1.1.1");
    }

    @Test
    void 没有转发头时回退到远程地址() {
        assertThat(IpUtils.getIp(requestWith(null, null, "9.9.9.9"))).isEqualTo("9.9.9.9");
    }

    @Test
    void unknown视为无效并继续回退() {
        assertThat(IpUtils.getIp(requestWith("unknown", null, "9.9.9.9"))).isEqualTo("9.9.9.9");
    }

    @Test
    void 末尾是unknown时跳过继续向前找() {
        assertThat(IpUtils.getIp(requestWith("1.1.1.1, unknown", null, "9.9.9.9")))
                .isEqualTo("1.1.1.1");
    }
}
