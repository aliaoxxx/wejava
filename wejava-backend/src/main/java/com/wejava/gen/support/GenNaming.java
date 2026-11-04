package com.wejava.gen.support;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 表名/列名推导规则。
 *
 * <p>{@code system_notice} → 模块 {@code system}、业务 {@code notice}、
 * 类名 {@code SystemNotice}、变量 {@code notice}。
 * 没有下划线的表（如 {@code notice}）视为「无模块前缀」，模块名取 {@code system}。
 */
public final class GenNaming {

    /** 无前缀时的默认模块 */
    private static final String DEFAULT_MODULE = "system";

    private GenNaming() {}

    /** MySQL 列名 → Java 属性名：user_name → userName */
    public static String toCamel(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String[] parts = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return sb.toString();
    }

    /** Java 属性名 → 类名：notice → Notice */
    public static String capitalize(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /** MySQL 列名 → 表名推导：sys_user → SysUser */
    public static String toClassName(String tableName) {
        return capitalize(toCamel(tableName));
    }

    /** 模块名：取第一个下划线段，否则用默认模块 */
    public static String moduleName(String tableName) {
        int idx = tableName.indexOf('_');
        if (idx <= 0) {
            return DEFAULT_MODULE;
        }
        return tableName.substring(0, idx);
    }

    /** 业务名：去掉模块前缀后的驼峰，notice / user_role → userRole */
    public static String businessName(String tableName) {
        int idx = tableName.indexOf('_');
        if (idx <= 0) {
            return toCamel(tableName);
        }
        String rest = tableName.substring(idx + 1);
        return rest.isEmpty() ? toCamel(tableName) : toCamel(rest);
    }

    /** 列名兜底展示名：user_name → User Name（没有列注释时用） */
    public static String camelToLabel(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(capitalize(part));
        }
        return sb.toString();
    }

    /** 表名 → 默认功能名：去掉 sys_ 前缀后用中文描述（无注释时的兜底） */
    public static String defaultFunctionName(String tableName, String tableComment) {
        if (tableComment != null && !tableComment.isBlank()) {
            return tableComment.trim();
        }
        String[] parts = tableName.split("_");
        return Arrays.stream(parts).map(GenNaming::capitalize).collect(Collectors.joining(" "));
    }
}
