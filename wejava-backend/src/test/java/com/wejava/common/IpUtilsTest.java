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
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(remoteAddr);
        return request;
    }

    @Test
    void 多层代理时取第一段() {
        assertThat(IpUtils.getIp(requestWith("1.1.1.1, 2.2.2.2, 3.3.3.3", null, "9.9.9.9")))
                .isEqualTo("1.1.1.1");
    }

    @Test
    void 单层代理去掉首尾空格() {
        assertThat(IpUtils.getIp(requestWith("  1.1.1.1  ", null, "9.9.9.9"))).isEqualTo("1.1.1.1");
    }

    @Test
    void 没有转发头时回退到远程地址() {
        assertThat(IpUtils.getIp(requestWith(null, null, "9.9.9.9"))).isEqualTo("9.9.9.9");
    }

    @Test
    void unknown_视为无效并继续找下一个头() {
        assertThat(IpUtils.getIp(requestWith("unknown", "2.2.2.2", "9.9.9.9"))).isEqualTo("2.2.2.2");
    }
}
