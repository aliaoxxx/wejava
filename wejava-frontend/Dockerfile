# syntax=docker/dockerfile:1

# ---------- 构建阶段 ----------
FROM node:20-alpine AS build
WORKDIR /app

# 固定 pnpm 版本：新大版本对构建脚本的策略不同，会导致安装中断
RUN corepack enable && corepack prepare pnpm@10.33.0 --activate

# 先装依赖，源码改动时可复用缓存
COPY wejava-frontend/package.json wejava-frontend/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY wejava-frontend/ ./
# 演示环境：前端走根路径（/），接口走 /api（见 .env.docker）
# 只跑 vite build：类型检查由 CI 负责，镜像构建不必重复；
# 且 auto-imports.d.ts / components.d.ts 是 vite 构建时生成的，vue-tsc 必须排在它之后
RUN VITE_APP_BASE=/ pnpm exec vite build --mode docker

# ---------- 运行阶段 ----------
FROM nginx:1.27-alpine

COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

# 静态落地页：占住根路径 /，承担 SEO/GEO；应用本身回退到 SPA
# 显式放开读权限：源文件权限随打包机不同，600 会让 nginx worker 读不到（403）
COPY landing/ /usr/share/nginx/landing/
RUN chmod -R a+rX /usr/share/nginx/landing

EXPOSE 80
