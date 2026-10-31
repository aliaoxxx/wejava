# Public Deployment Guide

Running the demo on the public internet is a different job from running it on your laptop.
Three paths below, ordered by effort.

**中文**: [deploy/README.md](../README.md)

---

## 0. Common prerequisites

| Item | Requirement |
|---|---|
| Host | Linux with Docker (Ubuntu 22.04 / Debian 12 recommended), 2 vCPU / 4 GB RAM minimum |
| DNS | One A record pointing at the server's public IP |
| Ports | Open **80 / 443**; **never** expose 3300 directly |
| Filing | **Mainland-China hosts require an ICP filing**, otherwise 80/443 is blocked by the cloud provider (we hit exactly this with `example.com`) |

Change the default credentials first:

```bash
cp .env.example .env
vim .env     # DB_PASSWORD / REDIS_PASSWORD / JWT_SECRET / AI_CONFIG_SECRET
```

---

## 1. Self-hosted VPS (most portable)

```bash
# 1) Install Docker
curl -fsSL https://get.docker.com | sh
sudo systemctl enable --now docker

# 2) Clone, configure, start
git clone https://github.com/aliaoxxx/wejava.git && cd wejava
cp .env.example .env && vim .env
./scripts/demo-up.sh
```

The stack is published on `127.0.0.1:3300` only if you tighten the port mapping
(see "Hardening"); by default it is `3300:80`.

**Host Nginx reverse proxy** (`/etc/nginx/sites-available/wejava`):

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

    # SSE: buffering must be off for the notification stream
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

Enable and issue a certificate:

```bash
sudo ln -s /etc/nginx/sites-available/wejava /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d demo.example.com --redirect
```

Firewall:

```bash
sudo ufw allow 22,80,443/tcp
sudo ufw enable
```

---

## 2. Render

Render can build a container straight from a `Dockerfile`, but **its managed databases are
PostgreSQL and Key-Value (Redis) only — there is no managed MySQL**. So this path splits:

| Component | Where |
|---|---|
| `frontend` (Nginx image) | Render Web Service, Dockerfile = `wejava-frontend/Dockerfile` |
| `backend` (Spring Boot) | Render Web Service, Dockerfile = `wejava-backend/Dockerfile` |
| Redis | Render Key Value (managed) |
| MySQL | External: Aiven / PlanetScale / TiDB Cloud |

Key points:

1. Create the database first; Flyway creates the tables on the backend's first start.
2. Environment variables per service:

   ```
   backend:  SPRING_PROFILES_ACTIVE=docker
             DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASSWORD
             REDIS_HOST / REDIS_PORT / REDIS_PASSWORD
             JWT_SECRET / AI_CONFIG_SECRET
   frontend: (already baked into the image — nothing to set)
   ```

3. **Important difference**: the container's `deploy/nginx.conf` proxies `/api` to
   `backend:8080`, which only resolves inside a compose network. On Render the two services
   have different hostnames — either change `proxy_pass` to your backend URL, or add a
   rewrite rule in the Render dashboard.
4. Render issues HTTPS automatically; no certbot needed.
5. Free instances sleep when idle — the first request takes 30–60 s. Fine for a demo,
   use a paid plan for anything real.

> In short: Render works, but you must convert the same-host Nginx proxy into a
> cross-service one. That is one more moving part than the VPS path.

---

## 3. Alibaba Cloud ECS

Same as the VPS path, with console-side differences:

1. **Security group**: allow `22`, `80`, `443`; do **not** allow 3300/3306/6379.
2. **ICP filing**: for mainland regions the domain must be filed and attached to Alibaba Cloud,
   otherwise 80/443 returns the `Non-compliance ICP Filing` block page.
3. To offload the database, use **RDS for MySQL** instead of the container:
   - remove or comment out the `mysql` service in compose
   - point `DB_HOST` at the RDS internal endpoint and use the RDS account
   - Redis can likewise be replaced by ApsaraDB for Redis
4. Certificates: `certbot`, or Alibaba Cloud's free certificate (Nginx format).

---

## 4. Security baseline for a public demo (important)

Publishing an admin console with `admin / admin123` hands the database to the internet.
Do at least the following:

### 4.1 Do not expose admin's write permissions

1. **Create a read-only role**
   - Roles → New: `roleKey=demo`, data scope as needed
   - Menu permissions: tick only `list` / `query`; never `add` / `edit` / `remove` / `export`
2. **Create a demo user**: Users → New `demo`, assign only the `demo` role
3. **Deal with admin**: change the default password, or disable it and keep a private account

### 4.2 Close or restrict dangerous entry points

| Entry point | Recommendation |
|---|---|
| Clear logs / batch delete | private account only |
| Export user list | not for the read-only role |
| AI configuration | admin only (already the default) |
| System settings | do not grant `system:config:*` to the demo account |

### 4.3 Add a second gate in Nginx

```nginx
# Restrict write endpoints to an allow-list (enable as needed)
location ~ ^/api/.*/(add|edit|remove|resetPwd|changeStatus) {
    allow 203.0.113.10;   # your office IP
    deny all;
}
```

Or protect the whole site with Basic Auth while showing it to a client:

```bash
sudo apt install apache2-utils
sudo htpasswd -c /etc/nginx/.htpasswd demo
```

### 4.4 Reset regularly

If the demo data gets messy, reset to the seed state (this deletes all data):

```bash
docker compose down -v
docker compose up -d
```

---

## 5. Day-2 operations

```bash
docker compose ps                        # health
docker compose logs -f backend           # backend logs
docker compose logs --tail=100 mysql     # database logs

# Backup (mysqldump from inside the container to the host)
docker compose exec -T mysql mysqldump -uwejava -p"$DB_PASSWORD" wejava > backup-$(date +%F).sql

# Upgrade
git pull && docker compose up -d --build

# Full reset
docker compose down -v && ./scripts/demo-up.sh
```

---

## 6. Hardening checklist

- [ ] All passwords and secrets in `.env` changed, and **not committed**
- [ ] 3300 bound to `127.0.0.1` only (change compose to `127.0.0.1:${WEB_PORT:-3300}:80`)
- [ ] Security group only opens 22/80/443
- [ ] admin password changed or account disabled; demo account is read-only
- [ ] HTTPS enabled with an `http → https` redirect
- [ ] ICP filing approved (mainland regions)
- [ ] A periodic reset or backup is scheduled
