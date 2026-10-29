package com.wejava.common.util;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * CSV 导出。相比 Excel 少了 POI 依赖（约 10MB），Excel/WPS 都能直接打开。
 * <p>
 * 关键点：写入 UTF-8 BOM，否则 Excel 打开中文会乱码。
 */
public final class CsvUtils {

    private CsvUtils() {}

    public static void write(
            HttpServletResponse response, String filename, List<String> headers, List<List<String>> rows)
            throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(
                "Content-Disposition",
                "attachment;filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));

        PrintWriter writer = response.getWriter();
        writer.write('\uFEFF');
        writer.write(line(headers));
        for (List<String> row : rows) {
            writer.write(line(row));
        }
        writer.flush();
    }

    private static String line(List<String> cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(cells.get(i)));
        }
        return sb.append("\r\n").toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        // 先中和公式注入，再按 CSV 规则转义
        String safe = neutralize(value);
        boolean needQuote = safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r");
        String escaped = safe.replace("\"", "\"\"");
        return needQuote ? "\"" + escaped + "\"" : escaped;
    }

    /**
     * 防止 Excel 公式注入：以 = + - @ 或制表符/回车开头的单元格会被 Excel 当公式执行。
     * 在前面加一个单引号，Excel 会按文本处理；普通值不受影响。
     */
    private static String neutralize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
            return "'" + value;
        }
        return value;
    }
}
