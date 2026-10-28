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
        boolean needQuote = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needQuote ? "\"" + escaped + "\"" : escaped;
    }
}
