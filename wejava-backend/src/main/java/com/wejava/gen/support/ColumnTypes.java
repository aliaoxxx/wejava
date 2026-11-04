package com.wejava.gen.support;

import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 数据库类型 → Java / TS / 表单控件的推断。
 *
 * <p>推断只是「初值」，导入后可以在页面上逐列改，模板拿到的永远是最终配置。
 */
@Component
public class ColumnTypes {

    private static final Set<String> TEXT_COLUMNS = Set.of("remark", "description", "content", "summary");

    public static final String JAVA_STRING = "String";
    public static final String JAVA_LONG = "Long";
    public static final String JAVA_INTEGER = "Integer";
    public static final String JAVA_BOOLEAN = "Boolean";
    public static final String JAVA_BIG_DECIMAL = "BigDecimal";
    public static final String JAVA_DOUBLE = "Double";
    public static final String JAVA_LOCAL_DATE_TIME = "LocalDateTime";
    public static final String JAVA_LOCAL_DATE = "LocalDate";

    /** 去掉长度的裸类型：varchar(50) → varchar */
    public static String baseType(String columnType) {
        if (columnType == null) {
            return "";
        }
        int idx = columnType.indexOf('(');
        return (idx > 0 ? columnType.substring(0, idx) : columnType).trim().toLowerCase();
    }

    /** 取长度：varchar(50) → 50，取不到返回 0 */
    public static int length(String columnType) {
        if (columnType == null) {
            return 0;
        }
        int open = columnType.indexOf('(');
        int close = columnType.indexOf(')');
        if (open < 0 || close <= open) {
            return 0;
        }
        String inner = columnType.substring(open + 1, close);
        int comma = inner.indexOf(',');
        String len = (comma > 0 ? inner.substring(0, comma) : inner).trim();
        try {
            return Integer.parseInt(len);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String javaType(String columnType) {
        String base = baseType(columnType);
        return switch (base) {
            case "bigint" -> JAVA_LONG;
            case "int", "integer", "mediumint", "smallint", "tinyint" -> JAVA_INTEGER;
            case "bit", "boolean", "bool" -> JAVA_BOOLEAN;
            case "decimal", "numeric" -> JAVA_BIG_DECIMAL;
            case "double", "float", "real" -> JAVA_DOUBLE;
            case "datetime", "timestamp" -> JAVA_LOCAL_DATE_TIME;
            case "date" -> JAVA_LOCAL_DATE;
            default -> JAVA_STRING;
        };
    }

    public static String tsType(String javaType) {
        return switch (javaType) {
            case JAVA_LONG, JAVA_INTEGER, JAVA_BIG_DECIMAL, JAVA_DOUBLE -> "number";
            case JAVA_BOOLEAN -> "boolean";
            default -> "string";
        };
    }

    /** 表单控件推断 */
    public static String htmlType(String javaType, String columnName, String columnType) {
        String name = columnName == null ? "" : columnName.toLowerCase();
        if (JAVA_LOCAL_DATE_TIME.equals(javaType)) {
            return "datetime";
        }
        if (JAVA_LOCAL_DATE.equals(javaType)) {
            return "date";
        }
        if (JAVA_BOOLEAN.equals(javaType)
                || "status".equals(name)
                || "enabled".equals(name)
                || name.startsWith("is_")) {
            return "switch";
        }
        if (JAVA_STRING.equals(javaType)) {
            int len = length(columnType);
            if (TEXT_COLUMNS.contains(name) || len > 255 || "text".equals(baseType(columnType))) {
                return "textarea";
            }
            return "input";
        }
        return "number";
    }

    /** 查询方式推断：字符串名称类用 LIKE，时间用 BETWEEN，其余等值 */
    public static String queryType(String javaType, String columnName) {
        String name = columnName == null ? "" : columnName.toLowerCase();
        if (JAVA_LOCAL_DATE_TIME.equals(javaType) || JAVA_LOCAL_DATE.equals(javaType)) {
            return "BETWEEN";
        }
        if (JAVA_STRING.equals(javaType)
                && (name.contains("name") || name.endsWith("_code") || name.contains("title") || name.contains("no"))) {
            return "LIKE";
        }
        return "EQ";
    }

    /** BaseEntity 的时间/操作人字段：列表可见，表单不出现 */
    public static boolean isAuditColumn(String columnName) {
        return "create_by".equals(columnName)
                || "create_time".equals(columnName)
                || "update_by".equals(columnName)
                || "update_time".equals(columnName);
    }
}
