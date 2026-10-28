package com.wejava;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
@MapperScan("com.wejava.**.mapper")
public class WeJavaApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(WeJavaApplication.class, args);
    }

    /** war 部署到外部 Tomcat 时的入口 */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(WeJavaApplication.class);
    }
}
