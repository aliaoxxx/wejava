package com.wejava.common.util;

import org.springframework.util.StringUtils;

/**
 * User-Agent 解析。纯函数，无状态，不依赖框架。
 */
public final class UserAgentUtils {

    private UserAgentUtils() {}

    public static String parseBrowser(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "Unknown";
        }
        if (userAgent.contains("Edg/")) {
            return "Edge";
        }
        if (userAgent.contains("Chrome/")) {
            return "Chrome";
        }
        if (userAgent.contains("Firefox/")) {
            return "Firefox";
        }
        if (userAgent.contains("Safari/")) {
            return "Safari";
        }
        if (userAgent.contains("curl/")) {
            return "curl";
        }
        return "Unknown";
    }

    public static String parseOs(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "Unknown";
        }
        if (userAgent.contains("Mac OS X") || userAgent.contains("Macintosh")) {
            return "macOS";
        }
        if (userAgent.contains("Windows")) {
            return "Windows";
        }
        if (userAgent.contains("Android")) {
            return "Android";
        }
        if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            return "iOS";
        }
        if (userAgent.contains("Linux")) {
            return "Linux";
        }
        return "Unknown";
    }
}
