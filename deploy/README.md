# 公网部署指南

把演示环境放到公网，和在自己电脑上跑是两件事。这里给三条路径，按投入从低到高排列。

**English**: [deploy/en/README.md](en/README.md)

---

## 0. 通用前提

| 项 | 要求 |
|---|---|
| 主机 | 装了 Docker 的 Linux（Ubuntu 22.04 / Debian 12 推荐），2 核 4G 起步 |
| 域名 | 一条 A 记录指向服务器公网 IP |
| 端口 | 放行 **80 / 443**；**不要**把 3300 直接暴露到公网 |
| 备案 | **大陆节点必须备案**，否则 80/443 会被云厂商拦截并返回 `Non-compliance ICP Filing` 页面 |

所有路径都建议先改掉默认口令：

```bash
cp .env.example .env
vim .env     # 改 DB_PASSWORD / REDIS_PASSWORD / JWT_SECRET / AI_CONFIG_SECRET
```

---

## 1. VPS 自建（最通用）

```bash
# 1) 安装 Docker
curl -fsSL https://get.docker.com | sh
sudo systemctl enable --now docker

# 2) 取代码、配置、启动
git clone https://github.com/aliaoxxx/wejava.git && cd wejava
cp .env.example .env && vim .env
./scripts/demo-up.sh
```

此时服务只在 `127.0.0.1:3300` 可用（compose 里映射的是 `3300:80`，建议再收紧为
`127.0.0.1:${WEB_PORT}:80`，见下方“加固”一节）。

**宿主机 Nginx 反代**（`/etc/nginx/sites-available/wejava`）：

```nginx
server {
    listen 80;
    server_name demo.example.com;

    location / {
        proxy_pass http://127.0.0.1:3300;
        proxy_http_version 1.1;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
        client_max_body_size 20m;
    }

    # SSE：通知流必须关闭缓冲
    location /api/stream/ {
        proxy_pass http://127.0.0.1:3300;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header Host $host;
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 3600s;
    }
}
```

启用并签证书：

```bash
sudo ln -s /etc/nginx/sites-available/wejava /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d demo.example.com --redirect
```

防火墙：

```bash
sudo ufw allow 22,80,443/tcp
sudo ufw enable
```

---

## 2. Render

Render 支持直接读取仓库里的 `Dockerfile` 部署容器，但**它的托管数据库只有 PostgreSQL 和 Key-Value（Redis），没有 MySQL**。所以这条路要拆开：

| 组件 | 放到哪 |
|---|---|
| `frontend`（Nginx 镜像） | Render Web Service，`Dockerfile` 指向 `wejava-frontend/Dockerfile` |
| `backend`（Spring Boot） | Render Web Service，`Dockerfile` 指向 `wejava-backend/Dockerfile` |
| Redis | Render Key Value（托管） |
| MySQL | 外部托管：Aiven / PlanetScale / TiDB Cloud 任选 |

步骤要点：

1. MySQL 建库后，先执行一次连接测试；Flyway 会在后端首次启动时自动建表。
2. 两个 Web Service 的环境变量：

   ```
   backend:  SPRING_PROFILES_ACTIVE=docker
             DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASSWORD
             REDIS_HOST / REDIS_PORT / REDIS_PASSWORD
             JWT_SECRET / AI_CONFIG_SECRET
   frontend: （镜像内已构建好，无需环境变量）
   ```

3. **关键差异**：容器里的 `deploy/nginx.conf` 把 `/api` 反代到 `backend:8080`。在 Render 上
   两个服务是不同的域名，需要把 `nginx.conf` 里的 `proxy_pass http://backend:8080` 改成
   你实际的后端地址，或者在 Render 面板给前端加一条 rewrite 规则。
4. Render 会自动签发 HTTPS，无需 certbot。
5. 免费实例长时间无请求会休眠，首次访问要等 30–60 秒；演示够用，正式对外建议付费档。

> 一句话：Render 这条路能跑，但要把“同机 Nginx 反代”改成“跨服务反代”，比 VPS 多一道配置。

---

## 3. 阿里云 ECS

和 VPS 基本一致，差异在控制台侧：

1. **安全组**放行 `22`、`80`、`443`；**不要**放行 3300/3306/6379。
2. **备案**：大陆地域的实例，域名必须完成 ICP 备案并接入阿里云，否则 80/443 会被拦
   （拦截页就是 `Non-compliance ICP Filing`）。
3. 想省事可以用 **RDS MySQL** 替代容器里的 MySQL：
   - 删掉或注释 compose 里的 `mysql` 服务
   - `DB_HOST` 指向 RDS 内网地址，`DB_NAME/DB_USER/DB_PASSWORD` 用 RDS 控制台创建的账号
   - Redis 同理可换 ApsaraDB for Redis
4. 证书：`certbot` 或阿里云免费证书（下载 Nginx 格式，配 `ssl_certificate` 即可）。

---

## 4. 公网演示的安全底线（重要）

公开一个带 `admin 123456` 的管理后台，等于把数据库交给互联网。上线前至少做这几件事：

### 4.1 不要暴露 admin 的写权限

1. **新建只读角色**
   - 角色管理 → 新增：`roleKey=demo`、数据范围选「仅本人」或「全部」按需
   - 菜单权限**只勾** `list` / `query` 这类读接口，不要勾 `add` / `edit` / `remove` / `export`
2. **新建演示账号**：用户管理 → 新增 `demo`，只分配 `demo` 角色
3. **处理 admin**：改掉默认密码；或直接停用，平时用另一个私密账号登录

### 4.2 关掉或限制危险入口

| 入口 | 建议 |
|---|---|
| 清空日志 / 批量删除 | 只给私密账号 |
| 导出用户列表 | 只读角色不要给 |
| AI 配置 | 仅管理员可见（默认已是） |
| 参数设置 | 不要给演示账号 `system:config:*` |

### 4.3 Nginx 层再拦一道

```nginx
# 只允许内网/白名单访问写接口（按需启用）
location ~ ^/api/.*/(add|edit|remove|resetPwd|changeStatus) {
    allow 203.0.113.10;   # 你的办公 IP
    deny all;
}
```

或给整个站点加 Basic Auth（演示给客户看时临时开启）：

```bash
sudo apt install apache2-utils
sudo htpasswd -c /etc/nginx/.htpasswd demo
```

### 4.4 定期重置

演示数据被改乱了就重置（会清空所有数据，恢复到种子状态）：

```bash
docker compose down -v
docker compose up -d
```

---

## 5. 常用运维

```bash
docker compose ps                        # 健康状态
docker compose logs -f backend           # 后端日志
docker compose logs --tail=100 mysql     # 数据库日志

# 备份（容器内 mysqldump 到宿主机）
docker compose exec -T mysql mysqldump -uwejava -p"$DB_PASSWORD" wejava > backup-$(date +%F).sql

# 升级
git pull && docker compose up -d --build

# 彻底重置
docker compose down -v && ./scripts/demo-up.sh
```

---

## 6. 加固清单（上线前逐条过）

- [ ] `.env` 里的密码和密钥全部换过，且**没有提交到仓库**
- [ ] 3300 只监听 `127.0.0.1`（改 compose 为 `127.0.0.1:${WEB_PORT:-3300}:80`）
- [ ] 安全组只开 22/80/443
- [ ] admin 密码已改或已停用，演示账号是只读角色
- [ ] HTTPS 已启用且 `http → https` 有跳转
- [ ] 域名备案已通过（大陆节点）
- [ ] 配置了定期重置或数据备份
