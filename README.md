# WeJavaAdmin

基于 **Spring Boot 4.1 + Spring Security 7.1 + MyBatis-Plus + Vue 3** 的后台管理框架。

认证采用**自建 JWT + Redis 会话白名单**，不引入 OAuth2 授权服务器，链路短、无额外组件。

## 特性

**认证与安全**

- 双令牌：访问令牌 30 分钟、刷新令牌 7 天并**轮换**，刷新时校验账号状态
- Redis 会话白名单，支持**强制下线**与可选的单点登录
- 图形验证码、登录失败锁定（5 次 / 15 分钟）、密码 BCrypt 存储

**权限**

- RBAC：用户 / 角色 / 菜单 / 部门 / 岗位
- 动态路由：菜单树由后端下发，前端按 `component` 动态注册
- 按钮级权限：`v-hasPerm` 指令，超管通配 `*:*:*`
- **行级数据权限**：`@DataScope` 注解 + JSqlParser 改写 SQL，支持全部 / 自定义 / 本部门 / 本部门及以下 / 仅本人

**防护**

- 同一 IP + 同一用户并发数限制（Redis 计数，参数可配）
- 接口 QPS 限流（固定窗口），超限返回 HTTP 429 + `Retry-After`

**可观测**

- 操作日志：`@OperLog` 切面异步落库，密码字段自动脱敏
- 登录日志、在线用户、服务监控（CPU / 内存 / JVM / 磁盘 / DB / Redis）
- 全局 TraceId、统一响应体、全局异常处理

**前端**

- **配置驱动 CRUD**：一个 `schema.ts` 生成完整增删改查页
- 多页签、明暗模式（亮色 / 暗色 / 跟随系统）、主题色、动效档位
- 树形页面：菜单 / 部门 / 字典（主从）

## 技术栈

| 层 | 选型 | 版本 |
|---|---|---|
| JDK | Temurin / Oracle | 17 |
| 框架 | Spring Boot | 4.1.2（Spring Framework 7.0.10） |
| 安全 | Spring Security | 7.1.2 |
| ORM | MyBatis-Plus | 3.5.18（含 mybatis-plus-jsqlparser） |
| 对象映射 | MapStruct | 1.6.3 |
| 数据库 | MySQL | 8.4+（本机 9.6 验证） |
| 迁移 | Flyway | 随 Boot BOM |
| 缓存 | Redis | 7.0+（本机 8.3 验证） |
| 前端框架 | Vue | 3.5 |
| 构建 | Vite | 6 |
| 语言 | TypeScript | 5.7 |
| UI | Element Plus | 2.9 |
| 状态 | Pinia | 2.3 |

> **Boot 4 的两个变化**：JSON 库换成 Jackson 3（包名 `tools.jackson.*`）；starter 更名，如 `spring-boot-starter-webmvc`、`spring-boot-starter-aspectj`。
>
> 虚拟线程需 JDK 21，当前使用 JDK 17，未启用。

## 环境要求

| 依赖 | 版本 | 说明 |
|---|---|---|
| JDK | 17+ | Boot 4 最低要求 |
| Maven | 3.9+ | |
| Node | 20+ | Vite 6 要求 |
| pnpm | 9+ | |
| MySQL | 8.4+ | 需 `utf8mb4` |
| Redis | 7.0+ | |

## 快速开始

```bash
# 1. 建库（只需一次，表结构与初始数据由 Flyway 自动迁移）
mysql -uroot -p -e "CREATE DATABASE wejava DEFAULT CHARSET utf8mb4;"

# 2. 后端：端口 8000，上下文 /dev/api
cd wejava-backend
mvn spring-boot:run

# 3. 前端：http://localhost:3300/dev/
cd wejava-frontend
pnpm install && pnpm dev
```

打开 <http://localhost:3300/dev/>，用 `admin / admin123` 登录。

连接信息默认走本机，可用环境变量覆盖：

```
DB_HOST  DB_PORT  DB_NAME  DB_USER  DB_PASSWORD
REDIS_HOST  REDIS_PORT  REDIS_PASSWORD
```

> 首次 clone 后 `src/types/auto-imports.d.ts` 尚未生成，IDE 可能提示类型错误，
> 执行一次 `pnpm dev` 或 `pnpm build` 即会生成（该文件不入库）。
>
> 本地调试若不想输验证码，用 `CAPTCHA_ENABLED=false mvn spring-boot:run` 启动。

Flyway 初始化后会写入 61 个菜单、2 个角色、2 个账号（`admin` 超管、`tester` 普通用户）。

## 目录结构

```
wejava/
├── docs/                     设计文档
│   ├── 01-数据库设计.md        15 张表的 DDL 与初始化数据
│   ├── 02-认证与权限设计.md
│   ├── 03-接口设计.md
│   ├── 04-前端设计.md
│   ├── 05-开发规范.md          分层、文件粒度、命名
│   ├── 06-前端配置驱动设计.md
│   ├── 07-部署方案.md          WAR + Nginx
│   └── 避坑清单.md
├── wejava-backend/           Java 后端（包名 com.wejava）
│   └── src/main/java/com/wejava/
│       ├── common/           统一响应、异常、AOP、工具
│       ├── security/         认证基础设施（不得依赖 system）
│       └── system/           业务：controller / service / mapper / converter / model
└── wejava-frontend/          Vue 前端
    └── src/
        ├── api/              接口封装
        ├── components/Crud/  配置驱动 CRUD 组件
        ├── hooks/useCrud.ts
        ├── layout/           布局、侧边栏、多页签
        ├── router/           动态路由与守卫
        ├── store/modules/    user / permission / app / tagsView / theme
        └── views/            页面
```

## 开发

```bash
cd wejava-backend
mvn spotless:apply          # 代码格式化（提交前必跑）
mvn test                    # 单元测试
./scripts/verify-auth.sh    # 认证闭环验收 14 项（需 CAPTCHA_ENABLED=false 启动）
```

前端类型检查与构建：

```bash
cd wejava-frontend
pnpm build      # vue-tsc 严格模式 + Vite 构建
```

## 构建与部署

```bash
# 后端：WAR 包（prod#api.war 部署到 Tomcat 后上下文为 /prod/api）
mvn clean package -Pprod -DskipTests

# 前端：base 为 /prod/，产物在 dist/
pnpm build
```

路径约定：生产 `/prod/`，开发 `/dev/`。WAR 文件名中的 `#` 会被 Tomcat 解析为 `/`，
所以 `prod#api.war` 的上下文就是 `/prod/api`。

完整方案见 [docs/07-部署方案.md](docs/07-部署方案.md)（目录规划、Nginx 配置、发版回滚）。

## 文档

设计文档索引见 [docs/README.md](docs/README.md)：

- [数据库设计](docs/01-数据库设计.md)
- [认证与权限设计](docs/02-认证与权限设计.md)
- [接口设计](docs/03-接口设计.md)
- [前端设计](docs/04-前端设计.md)
- [开发规范](docs/05-开发规范.md)
- [前端配置驱动设计](docs/06-前端配置驱动设计.md)
- [部署方案](docs/07-部署方案.md)
- [避坑清单](docs/避坑清单.md)
