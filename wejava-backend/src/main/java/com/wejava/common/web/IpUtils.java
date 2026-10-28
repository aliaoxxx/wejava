package com.wejava.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * 获取真实客户端 IP。经 Nginx 反代时取 X-Forwarded-For 的第一段。
 */
public final class IpUtils {

    private IpUtils() {}

    public static String getIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headers) {
            String value = request.getHeader(header);
            if (StringUtils.hasText(value) && !"unknown".equalsIgnoreCase(value)) {
                int idx = value.indexOf(',');
                return (idx > 0 ? value.substring(0, idx) : value).trim();
            }
        }
        return request.getRemoteAddr();
    }
}
