[English](en/README.md) | 中文

# WeJavaAdmin 设计文档

一个基于 **Spring Boot 4.1 + Spring Security 7 + MyBatis-Plus + Vue 3** 的后台管理框架。
认证方案为 **自建 JWT + Redis 会话白名单**（不使用 OAuth2 授权服务器）。

---

## 文档索引

| 文档 | 内容 | English |
|---|---|---|
| [01-数据库设计](01-数据库设计.md) | 20 张表完整 DDL、索引、数据权限字段、初始化数据（含站内消息） | [01-database-design](en/01-database-design.md) |
| [02-认证与权限设计](02-认证与权限设计.md) | 登录时序、Token 结构、Redis 规范、过滤器链、RBAC、数据权限 | [02-auth-and-permissions](en/02-auth-and-permissions.md) |
| [03-接口设计](03-接口设计.md) | 统一返回体、错误码、全部接口清单、权限字符表 | [03-api-design](en/03-api-design.md) |
| [04-前端设计](04-前端设计.md) | 目录结构、动态路由、axios 刷新队列、权限指令 | [04-frontend-design](en/04-frontend-design.md) |
| [05-开发规范](05-开发规范.md) | 分层、命名、DTO/VO/Converter、分页、事务、MyBatis-Plus 与 XML 规范 | [05-development-conventions](en/05-development-conventions.md) |
| [06-前端配置驱动设计](06-前端配置驱动设计.md) | CRUD Schema、useCrud、三层策略、落地顺序 | [06-schema-driven-crud](en/06-schema-driven-crud.md) |
| [07-部署方案](07-部署方案.md) | WAR + Nginx 部署、路径前缀、发版回滚 | [07-deployment](en/07-deployment.md) |
| [避坑清单](避坑清单.md) | 上线前逐条核对的风险项 | [pitfalls](en/pitfalls.md) |
| [演示脚本](demo-script.md) | 60 秒录屏分镜：打开 → 登录 → 新增用户 → 看监控 | [demo-script](en/demo-script.md) |

---

## 一、技术栈

| 层 | 选型 | 版本 | 说明 |
|---|---|---|---|
| 运行时 | JDK | 17 | Boot 4 最低要求；虚拟线程需 21，当前未启用 |
| 框架 | Spring Boot | 4.1.2 | |
| 安全 | Spring Security | 7.1.2 | 仅认证 + 授权，不含 authorization-server |
| ORM | MyBatis-Plus | 3.5.18 | `mybatis-plus-spring-boot4-starter` + `mybatis-plus-jsqlparser` |
| 数据库 | MySQL | 8.4+ | 8.0 已 EOL；本机在 9.6 上验证 |
| 迁移 | Flyway | 随 Boot 管理 | 需显式引入 `spring-boot-starter-flyway` |
| 缓存 | Redis | 7.0+ | 会话、验证码、限流、锁 |
| 连接池 | HikariCP | 随 Boot 管理 | 不使用 Druid |
| 接口文档 | springdoc-openapi | 3.x | 仅 dev 开启，生产关闭 |
| 前端 | Vue + Vite + TS | 3.5 / 6 / 5.7 | |
| UI | Element Plus | 2.9 | |
| 状态 | Pinia | 2.3 | |

---

## 二、整体架构

```
┌─────────────────────────────────────────────────────────┐
│                     浏览器（Vue 3 SPA）                   │
│   登录页 / 动态路由 / v-hasPerm / axios 刷新队列            │
└───────────────────────┬─────────────────────────────────┘
                        │ HTTPS  Authorization: Bearer <jwt>
┌───────────────────────▼─────────────────────────────────┐
│                  Nginx（静态资源 + 反向代理）               │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│              Spring Boot 4.1（单体，模块化分包）            │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │ SecurityFilterChain                              │   │
│  │  CorsFilter → JwtAuthFilter → AuthorizationFilter │   │
│  └──────────────────────────────────────────────────┘   │
│                                                          │
│  Controller → Service → Mapper(MyBatis-Plus)             │
│       │           │                                      │
│       │           ├── DataPermissionInterceptor（拼SQL）  │
│       │           └── @Transactional                     │
│       │                                                  │
│       └── 统一返回 R<T> / 全局异常 / traceId               │
└──────────┬────────────────────────────┬─────────────────┘
           │                            │
    ┌──────▼──────┐              ┌──────▼──────┐
    │  MySQL 8.4+  │              │  Redis 7+   │
    │  业务数据     │              │  会话/缓存   │
    └─────────────┘              └─────────────┘
```

**为什么是单体**：后台管理系统的复杂度在权限和业务，不在服务拆分。单进程 + 按功能分包，
需要拆分时再按包边界抽模块，成本远低于一开始就上微服务。

---

## 三、模块划分与依赖方向

```
com.wejava
├── WeJavaApplication.java
│
├── common/                    # 通用能力，不依赖任何业务模块
│   ├── core/                  # R<T>、PageResult<T>、BaseEntity
│   ├── exception/             # BizException、ErrorCode、GlobalExceptionHandler
│   ├── web/                   # TraceIdFilter、IpUtils
│   ├── config/                # JacksonConfig、MybatisPlusConfig、WebMvcConfig
│   ├── util/                  # 工具类
│   └── constant/              # 常量、枚举
│
├── security/                  # 认证授权，只依赖 common
│   ├── config/                # SecurityConfig、PasswordConfig
│   ├── jwt/                   # JwtService、JwtAuthFilter、SessionStore、LoginAttemptStore
│   ├── handler/               # AuthEntryPoint、AccessDeniedHandler
│   ├── model/                 # LoginSession、AuthUser、Captcha、OnlineSession
│   └── service/               # CaptchaService
│
├── system/                    # 系统管理，依赖 common + security
│   ├── controller/            # XxxController
│   ├── service/               # XxxService（接口）
│   ├── service/impl/          # XxxServiceImpl（实现）
│   ├── mapper/                # XxxMapper
│   ├── converter/             # XxxConverter（MapStruct）
│   └── model/
│       ├── entity/            # SysUser、SysRole ...
│       ├── converter/         # SysUserConverter（MapStruct）
│       ├── query/             # SysUserQuery（继承 PageQuery）
│       ├── form/              # SysUserCreateForm / SysUserUpdateForm
│       └── vo/                # SysUserVO
│
└── biz/                       # 业务模块（预留，按领域新建包）
```

**依赖规则（硬约束）**：

1. `common` 不依赖任何其他模块
2. `security` 只依赖 `common`
3. `system` 可依赖 `common`、`security`
4. `biz` 可依赖 `common`、`security`，**原则上不直接依赖 `system` 的内部实现**（需要时通过接口）
5. 禁止 `system` 反向依赖 `biz`

---

## 四、后端目录结构

```
wejava-backend/
├── pom.xml
└── src/main/
    ├── java/com/wejava/
    │   ├── WeJavaApplication.java
    │   ├── common/
    │   │   ├── core/R.java
    │   │   ├── core/PageResult.java
    │   │   ├── core/BaseEntity.java
    │   │   ├── exception/BizException.java
    │   │   ├── exception/ErrorCode.java
    │   │   ├── exception/GlobalExceptionHandler.java
    │   │   ├── web/TraceIdFilter.java
    │   │   ├── web/JacksonConfig.java
    │   │   └── util/
    │   ├── security/
    │   │   ├── config/SecurityConfig.java
    │   │   ├── jwt/JwtService.java
    │   │   ├── jwt/JwtAuthFilter.java
    │   │   ├── jwt/TokenStore.java
    │   │   ├── handler/RestAuthEntryPoint.java
    │   │   ├── handler/RestAccessDeniedHandler.java
    │   │   ├── model/LoginUser.java
    │   │   └── service/AuthService.java
    │   ├── system/
    │   │   ├── user/
    │   │   │   ├── controller/SysUserController.java
    │   │   │   ├── service/SysUserService.java
    │   │   │   ├── mapper/SysUserMapper.java
    │   │   │   ├── entity/SysUser.java
    │   │   │   └── dto/  vo/
    │   │   ├── role/  menu/  dept/  post/
    │   │   ├── dict/  config/  notice/
    │   │   └── log/
    │   └── biz/
    └── resources/
        ├── application.yml
        ├── application-dev.yml
        ├── application-prod.yml
        ├── db/migration/
        │   ├── V1__init_schema.sql
        │   └── V2__init_data.sql
        └── mapper/                  # XML（复杂查询才用）
```

**包内分层约定**：`controller` / `service`（接口）+ `service/impl`（实现）/ `mapper` / `converter` / `model/{entity,query,form,vo}`。
`form` 是入参，`vo` 是出参，`entity` 只用于持久化，**三者不混用**，转换由 MapStruct 的 converter 完成。

---

## 五、命名与编码约定

| 对象 | 规范 | 示例 |
|---|---|---|
| 数据库表 | `sys_` / `biz_` 前缀 + snake_case | `sys_user` |
| 数据库列 | snake_case | `create_time` |
| Java 类 | PascalCase | `SysUserService` |
| Java 字段 | camelCase | `createTime` |
| 接口路径 | kebab-case，小写 | `/system/user/reset-pwd` |
| 权限字符 | `模块:实体:动作` | `system:user:add` |
| Redis key | `admin:模块:用途:标识` | `admin:auth:token:1:uuid` |
| 枚举 | 全大写 | `ErrorCode.UNAUTHORIZED` |

**强制约束**：

- 所有表必须有 `create_by/create_time/update_by/update_time`
- 需要软删的表必须有 `del_flag`
- 金额用 `decimal(18,2)`，禁用 `float/double`
- 时间统一 `datetime(3)`，时区固定 `Asia/Shanghai`
- 主键统一 `bigint AUTO_INCREMENT`（由数据库生成），**序列化给前端时转 String**

---

## 六、环境与启动

| 环境 | 端口 | 说明 |
|---|---|---|
| MySQL | 3306 | 库名 `wejava`，账号 `wejava` |
| Redis | 6379 | db 0，需要密码 |
| 后端 | 8000 | 开发上下文 `/dev/api`；生产 WAR 为 `/prod/api` |
| 前端 | 3300 | 开发访问 `/dev/`，代理 `/dev/api` 到 8000 |

```bash
# 1. 起后端（Flyway 自动建表并初始化数据）
cd wejava-backend
mvn spring-boot:run

# 只想跑验收、不手动输验证码时：
# CAPTCHA_ENABLED=false mvn spring-boot:run

# 2. 起前端
cd wejava-frontend && pnpm install && pnpm dev
```

所有敏感配置都支持环境变量覆盖，生产环境不要在配置文件里写死：

| 变量 | 用途 |
|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | MySQL |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis |
| `JWT_SECRET` | JWT 签名密钥（**必须 ≥ 32 字节**） |
| `CAPTCHA_ENABLED` | 是否开启图形验证码，默认 `true` |

默认账号：`admin / admin123`（超级管理员，拥有全部权限）。

### M2 验收脚本

```bash
# 应用需以 CAPTCHA_ENABLED=false 启动
cd wejava-backend
./scripts/verify-auth.sh
```

覆盖 14 个用例：登录、鉴权、篡改签名、token 类型混淆、refresh 轮换、
权限拒绝（403）、登出撤销、防爆破锁定。

> 其中 403 用例需要数据库里存在 `tester` 账号（角色 `common`，无任何权限）。
> 建法见 `scripts/verify-auth.sh` 头部注释；没有该账号时脚本会自动跳过这一条。

---

## 七、交付里程碑

| 阶段 | 内容 | 状态 |
|---|---|---|
| M1 | 工程骨架、统一返回、异常、traceId、Flyway、15 张表（后续迁移扩展到 20 张） | ✅ 完成 |
| M2 | 认证：登录/刷新/登出/验证码/防爆破/Redis 会话/RBAC 装配 | ✅ 完成 |
| M2.5 | 开发规范、BaseEntity/PageQuery/审计填充、MapStruct、Spotless | ✅ 完成 |
| M3-1 | 后端 RBAC：用户/角色/菜单 CRUD + 动态路由接口 | ✅ 完成 |
| M3-2 | 前端基座：Vue 工程、登录页、布局、动态路由、axios | ✅ 完成 |
| M3-3 | 配置驱动：CrudPage 四件套 + 用户/角色页 + 菜单树形页 + 多页签 | ✅ 完成 |
| M4 | 部门/岗位/字典/参数/公告 | ✅ 完成 |
| M5 | 日志（操作/登录）+ 在线用户 + 监控 | ✅ 完成 |
| M6 | 数据权限、限流、导出 | ✅ 完成 |
| M7 | 前端布局、多页签、主题、打包部署 | ✅ 完成（已部署验证） |
