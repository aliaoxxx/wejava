package com.wejava.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletResponse;

/** 链路 ID 的透传与校验：只接受格式合法的外部值，其余一律重新生成 */
class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    private String run(String incoming) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(TraceIdFilter.HEADER)).thenReturn(incoming);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        String fromHeader = response.getHeader(TraceIdFilter.HEADER);
        assertThat(fromHeader).isNotBlank();
        // 请求结束后 MDC 必须清理干净，否则线程复用会串号
        assertThat(MDC.get(TraceIdFilter.TRACE_ID)).isNull();
        return fromHeader;
    }

    @Test
    void 上游传入合法ID时沿用() throws Exception {
        assertThat(run("gateway-abc_123")).isEqualTo("gateway-abc_123");
    }

    @Test
    void 未传ID时生成16位() throws Exception {
        assertThat(run(null)).hasSize(16).matches("[0-9a-f]{16}");
    }

    @Test
    void 含非法字符的ID不被采用() throws Exception {
        assertThat(run("<script>alert(1)</script>")).doesNotContain("<").hasSize(16);
    }

    @Test
    void 超长ID不被采用() throws Exception {
        assertThat(run("a".repeat(200))).hasSize(16);
    }

    @Test
    void 过短ID不被采用() throws Exception {
        assertThat(run("abc")).hasSize(16);
    }

    @Test
    void 前后空格会被裁剪() throws Exception {
        assertThat(run("  req-id-0001  ")).isEqualTo("req-id-0001");
    }

    @Test
    void 过滤器会继续执行后续链路() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(TraceIdFilter.HEADER)).thenReturn("trace-0001");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getHeader(TraceIdFilter.HEADER)).isEqualTo("trace-0001");
    }
}
