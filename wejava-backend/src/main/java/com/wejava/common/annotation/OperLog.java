package com.wejava.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在 Controller 方法上，由 OperLogAspect 异步记录操作日志。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperLog {

    /** 模块标题，如「用户管理」 */
    String title();

    BusinessType businessType() default BusinessType.OTHER;

    /** 是否记录请求参数（默认记录） */
    boolean saveParam() default true;
}
