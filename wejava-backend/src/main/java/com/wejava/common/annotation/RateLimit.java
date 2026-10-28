package com.wejava.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口级限流：在 timeWindow 秒内最多允许 count 次调用。
 * <p>
 * 与「并发数限制」互补：
 * - 并发数保护线程池/连接池（同一时刻的请求数）
 * - QPS 限制防刷（单位时间的请求数）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 窗口内允许的最大次数 */
    int count() default 10;

    /** 窗口长度（秒） */
    int seconds() default 1;

    /** 自定义提示语 */
    String message() default "";
}
