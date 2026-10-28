package com.wejava.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.wejava.common.interceptor.DataScopeInnerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    /**
     * 生产环境固定 MYSQL；测试环境用 H2，可通过 mybatis-plus.db-type 覆盖。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(@Value("${mybatis-plus.db-type:MYSQL}") String dbType) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 数据权限必须在分页之前：这样 count 语句也会带上权限条件
        interceptor.addInnerInterceptor(new DataScopeInnerInterceptor());
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.getDbType(dbType)));
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }
}
