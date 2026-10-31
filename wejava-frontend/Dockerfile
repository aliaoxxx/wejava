# syntax=docker/dockerfile:1

# ---------- 构建阶段 ----------
FROM node:20-alpine AS build
WORKDIR /app

RUN corepack enable

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

EXPOSE 80
