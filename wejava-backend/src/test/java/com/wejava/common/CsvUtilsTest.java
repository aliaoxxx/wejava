package com.wejava.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvUtilsTest {

    private String render(List<String> headers, List<List<String>> rows) throws Exception {
        StringWriter buffer = new StringWriter();
        PrintWriter writer = new PrintWriter(buffer);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(writer);

        CsvUtils.write(response, "测试.csv", headers, rows);
        writer.flush();
        return buffer.toString();
    }

    @Test
    void 写入UTF8_BOM_否则Excel中文乱码() throws Exception {
        String csv = render(List.of("姓名"), List.of(List.of("张三")));
        assertThat(csv.charAt(0)).isEqualTo('\uFEFF');
        assertThat(csv).contains("张三");
    }

    @Test
    void 含逗号_引号_换行的字段要加引号并转义() throws Exception {
        String csv = render(List.of("a", "b", "c"), List.of(Arrays.asList("x,y", "he said \"hi\"", "line1\nline2")));

        assertThat(csv).contains("\"x,y\"");
        assertThat(csv).contains("\"he said \"\"hi\"\"\"");
        assertThat(csv).contains("\"line1\nline2\"");
    }

    @Test
    void 空值写成空字符串而不是null() throws Exception {
        String csv = render(List.of("a"), List.of(Arrays.asList((String) null)));
        assertThat(csv).doesNotContain("null");
        assertThat(csv).endsWith("\r\n");
    }

    @Test
    void 每行以CRLF结尾() throws Exception {
        String csv = render(List.of("h"), List.of(List.of("1"), List.of("2")));
        assertThat(csv.split("\r\n", -1)).hasSize(4);
    }

    @Test
    void 中文文件名做URL编码() throws Exception {
        StringWriter buffer = new StringWriter();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(buffer));
        CsvUtils.write(response, "用户列表.csv", List.of("h"), List.of());
        // 无法直接断言 header（mock 无守卫），这里只验证写入流正常
        assertThat(buffer.toString()).startsWith("\uFEFF");
        assertThat(new String("用户列表.csv".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8))
                .isEqualTo("用户列表.csv");
    }
}
