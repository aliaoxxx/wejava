package com.wejava.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档配置。
 *
 * <p>只新增配置，不改任何 Controller 的方法签名和返回结构 —— 文档内容全部由 springdoc
 * 扫描现有注解生成。
 *
 * <p>生产环境通过 {@code springdoc.api-docs.enabled=false} 关闭（见 application-prod.yml），
 * 此时本配置类不生效，Swagger UI 与 /v3/api-docs 均返回 404。
 */
@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true", matchIfMissing = true)
public class OpenApiConfig {

    /** Swagger UI 右上角 Authorize 按钮使用的安全方案名 */
    private static final String BEARER_SCHEME = "bearerAuth";

    /** 各 profile 的上下文路径不同（/dev/api、/prod/api、/api），文档里的 Server 跟着走 */
    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Bean
    public OpenAPI wejavaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WeJavaAdmin API")
                        .version("1.0.0")
                        .description("后台管理框架接口文档。除登录、验证码、刷新外，所有接口都需要 Access Token。"
                                + "点击右上角 Authorize 填入 accessToken 后即可调试。")
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                // 用相对地址：Swagger UI 的 Try it out 会打到当前站点，不受端口/域名影响
                .servers(List.of(new Server()
                        .url(contextPath.isEmpty() ? "/" : contextPath)
                        .description("当前站点")))
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("请求头 Authorization: Bearer <accessToken>")))
                // 全局生效：每个接口都会带上这把锁，登录/验证码接口单独用 @SecurityRequirements 去掉
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
