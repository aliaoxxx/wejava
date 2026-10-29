package com.wejava.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * 获取真实客户端 IP。
 * <p>
 * 取值顺序有讲究，别用「X-Forwarded-For 第一段」：
 * <ul>
 *   <li>X-Real-IP：由本项目 Nginx 用 {@code $remote_addr} 覆盖写入，客户端无法伪造，优先使用</li>
 *   <li>X-Forwarded-For：代理是「追加」语义，只有<b>最后一段</b>是直连代理看到的地址；
 *       前面的段可能是客户端自己填的，取第一段等于允许伪造</li>
 * </ul>
 */
public final class IpUtils {

    private static final String HEADER_REAL_IP = "X-Real-IP";
    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
    private static final String UNKNOWN = "unknown";

    private IpUtils() {}

    public static String getIp(HttpServletRequest request) {
        String realIp = request.getHeader(HEADER_REAL_IP);
        if (isUsable(realIp)) {
            return realIp.trim();
        }

        String forwarded = request.getHeader(HEADER_FORWARDED_FOR);
        if (isUsable(forwarded)) {
            String[] parts = forwarded.split(",");
            for (int i = parts.length - 1; i >= 0; i--) {
                String candidate = parts[i].trim();
                if (isUsable(candidate)) {
                    return candidate;
                }
            }
        }
        return request.getRemoteAddr();
    }

    private static boolean isUsable(String value) {
        return StringUtils.hasText(value) && !UNKNOWN.equalsIgnoreCase(value.trim());
    }
}
