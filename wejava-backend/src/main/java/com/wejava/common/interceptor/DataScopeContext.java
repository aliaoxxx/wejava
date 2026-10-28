package com.wejava.common.interceptor;

/**
 * 数据权限 SQL 片段的 ThreadLocal 载体。
 * 由 DataScopeAspect 写入，DataScopeInnerInterceptor 读取。
 */
public final class DataScopeContext {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private DataScopeContext() {}

    public static void set(String sqlFragment) {
        HOLDER.set(sqlFragment);
    }

    public static String get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
