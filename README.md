# WeJavaAdmin

基于 **Spring Boot 4.1 + Spring Security 7 + MyBatis-Plus + Vue 3** 的后台管理框架。

认证方案：**自建 JWT + Redis 会话白名单**（不使用 OAuth2 授权服务器）。

## 技术栈

| 层 | 选型 | 版本 |
|---|---|---|
| JDK | Temurin / Oracle | 17 |
| 框架 | Spring Boot | 4.1.2 |
| 安全 | Spring Security | 7.1.x |
| ORM | MyBatis-Plus | 3.5.18 |
| 转换 | MapStruct | 1.6.3 |
| 数据库 | MySQL | 8.4 LTS |
| 缓存 | Redis | 8.x |
| 前端 | Vue + Vite + TS | 3.5 |

> Boot 4 要求 Java 17+。虚拟线程需 JDK 21，当前未启用。

## 目录结构

```
WeJavaAdmin/
├── docs/                 设计文档（数据库 / 认证 / 接口 / 前端 / 规范 / 避坑）
├── wejava-backend/       Java 后端（com.wejava）
├── wejava-frontend/      Vue 前端
├── docs/07-部署方案.md   生产/开发环境部署说明（WAR + Nginx）
```

## 快速开始

```bash
# 1. 启动依赖（可选，若本机已有 MySQL/Redis 可跳过）
docker compose up -d

# 2. 后端（端口 8000，上下文 /api）
cd wejava-backend
mvn spring-boot:run

# 3. 前端（端口 3300）
cd wejava-frontend
pnpm install && pnpm dev
```

默认账号：`admin / admin123`（超级管理员）。

## 开发

```bash
cd wejava-backend
mvn spotless:apply     # 代码格式化（提交前必跑）
mvn test               # 单元测试
./scripts/verify-auth.sh   # 认证闭环验收（需 CAPTCHA_ENABLED=false 启动）
```

## 文档

设计文档见 [docs/README.md](docs/README.md)：

- [数据库设计](docs/01-数据库设计.md)
- [认证与权限设计](docs/02-认证与权限设计.md)
- [接口设计](docs/03-接口设计.md)
- [前端设计](docs/04-前端设计.md)
- [开发规范](docs/05-开发规范.md)
- [前端配置驱动设计](docs/06-前端配置驱动设计.md)
- [避坑清单](docs/避坑清单.md)
