package com.wejava.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限。标注在 Service 的查询方法上，按当前用户角色的 data_scope 自动改写 SQL。
 * <p>
 * 注意：只有 MyBatis-Plus 生成的 SQL 会被自动改写；手写 XML 需要在 SQL 里手动拼条件。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {

    /** 部门字段所在的表别名 */
    String deptAlias() default "d";

    /** 用户字段所在的表别名 */
    String userAlias() default "u";

    /** true 表示"仅本人"时按 create_by 过滤（适用于没有 user_id 的表） */
    boolean selfByCreateBy() default false;
}
