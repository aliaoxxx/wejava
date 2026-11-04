[中文](../07-部署方案.md) | English

# Deployment Plan: WAR + Nginx

> No containerized deployment. The backend is packaged as a war and handed to Tomcat, the frontend static assets are served by Nginx, and both share the same path prefix.

## 1. Path Planning

There are two shapes for the public URL. **The root-path shape is recommended**: shorter
URLs for users, and it matches how most SaaS admin consoles look.

| Shape | Frontend URL | API URL | Backend artifact |
|---|---|---|---|
| **Root path (recommended)** | `https://host/` | `https://host/prod/api/**` | `prod#api.war` |
| Sub-path | `https://host/prod/` | `https://host/prod/api/**` | `prod#api.war` |
| Development | `http://localhost:3300/dev/` | `http://localhost:8000/dev/api/**` | `dev#api.war` |

**Why the API keeps the `/prod/api` prefix**

Moving the frontend to the root path only needs a different build flag and an Nginx
change — **the backend is untouched**. Moving the API to `/api` would mean renaming the
WAR (`prod#api.war` → `api.war`), redeploying, and cleaning the old context: more risk for
a prefix users never see. So it stays.

**How the prefixes work**

- Frontend: the Vite `base` is fixed at build time. Plain `pnpm build` produces `/prod/`;
  `VITE_APP_BASE=/ pnpm build` produces `/assets/...` (root-path shape).
  Routing uses `createWebHistory(import.meta.env.BASE_URL)`, so deep links survive a refresh.
- API: the base URL comes from `VITE_APP_BASE_API` in `.env.production` / `.env.development`,
  which is **`/prod/api` for both frontend shapes** — nothing to change.
- Backend context: **the WAR filename decides the Tomcat context path**; `#` becomes `/`.
  `prod#api.war` → `/prod/api`, `dev#api.war` → `/dev/api`.

## 2. Server Directories

```
/opt/wejava/
├── app/
│   ├── prod#api.war          # backend production package
│   └── dev#api.war           # backend dev package (optional)
├── frontend/
│   ├── prod/                 # frontend production static files (contents of dist)
│   └── dev/
└── logs/
    └── wejava-backend.log
```

Tomcat and Nginx use the system-installed versions, and `webapps` points to `/opt/wejava/app` (or a symlink).

## 3. Build

### Backend

```bash
# Production
mvn clean package -Pprod -DskipTests
# Output: target/prod#api.war

# Development
mvn clean package -Pdev -DskipTests
# Output: target/dev#api.war
```

`-Pprod` writes `spring.profiles.active` as `prod` through resource filtering,
and sets `finalName=prod#api`.

### Frontend

```bash
pnpm install

# Root-path shape (recommended): assets resolve to /assets/..., API stays /prod/api
VITE_APP_BASE=/ pnpm build

# Sub-path shape: base is /prod/
pnpm build

pnpm dev            # dev server http://localhost:3300/dev/
```

Output goes to `dist/`. The two shapes differ only in the asset prefix — **the deploy
directory is the same**.

## 4. Deploying the Backend

```bash
cp target/prod#api.war $CATALINA_HOME/webapps/prod#api.war

# Configure environment variables (write into $CATALINA_BASE/bin/setenv.sh)
export DB_HOST=127.0.0.1
export DB_PORT=3306
export DB_NAME=wejava
export DB_USER=wejava
export DB_PASSWORD='******'
export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379
export REDIS_PASSWORD='******'
export JWT_SECRET='random string of at least 32 characters, be sure to replace it'
export LOG_PATH=/opt/wejava/logs
```

After startup, visiting `http://127.0.0.1:8080/prod/api/ping` should return `{"code":200,...}`.

> The database schema is migrated automatically by Flyway when the application starts; on first startup, make sure the DB account has DDL privileges.
> The migration files are in `WEB-INF/classes/db/migration`; a failed validation makes startup fail outright (intentionally).

## 5. Deploying the Frontend

```bash
rm -rf /opt/wejava/frontend/prod/*
cp -r wejava-frontend/dist/* /opt/wejava/frontend/prod/

# Static landing page (separate directory, survives the rm above)
mkdir -p /opt/wejava/landing
rm -rf /opt/wejava/landing/*
cp -r landing/* /opt/wejava/landing/
chmod -R a+rX /opt/wejava/landing
```

### Why a separate landing page

The app is a Vue SPA: `curl` returns a shell with nothing but `<div id="app"></div>`.
Baiduspider and GPTBot / ClaudeBot / PerplexityBot do not execute JavaScript, so they see
an empty page — useless for SEO and GEO.

So `/` is served by a **pure static** landing page (intro, features, screenshots, FAQ,
structured data), while the app keeps answering at the same level (`/login`, `/dashboard`).
The SPA shell carries `noindex` so the empty pages are not indexed.

| Path | Content | Audience |
|---|---|---|
| `/` | static `landing/index.html` | search engines, AI crawlers, first-time visitors |
| `/robots.txt`, `/sitemap.xml` | SEO files | crawlers |
| `/login`, `/dashboard`, … | SPA | signed-in users |
| `/prod/api/**` | backend API | the app |

Nginx `root` must point at the `prod` level (not its parent), otherwise in the root-path
shape `/assets/app.js` resolves to `/opt/wejava/frontend/assets/app.js` and 404s.

## 6. Nginx Configuration

Root-path shape (this is what production runs; swap `listen 80` for the certificate block
when HTTPS is enabled):

```nginx
server {
    listen 80;
    server_name admin.example.com;

    # ---------- Static landing page: exact match on /, wins over the prefix below ----------
    location = / {
        root /opt/wejava/landing;
        try_files /index.html =404;
    }

    location ^~ /landing-assets/ {
        root /opt/wejava/landing;
    }

    location = /robots.txt  { root /opt/wejava/landing; }
    location = /sitemap.xml { root /opt/wejava/landing; }

    # ---------- Frontend app: root path ----------
    location / {
        root /opt/wejava/frontend/prod;
        try_files $uri $uri/ /index.html;
    }

    # Entry document is never cached, so a release takes effect immediately
    location = /index.html {
        root /opt/wejava/frontend/prod;
        add_header Cache-Control "no-cache, no-store, must-revalidate";
    }

    # Old /prod/ URLs redirect to the root; /prod/api/ is longer and unaffected
    location = /prod { return 301 /; }
    location /prod/  { return 301 /; }

    # ---------- SSE: buffering must be off ----------
    location ~ ^/prod/api/(stream|ai)/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 3600s;
    }

    # ---------- Business API ----------
    location /prod/api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
        client_max_body_size 20m;
    }
}
```

For the sub-path shape, replace `location /` with `location /prod/`, drop the two 301
rules, and leave everything else as is.

**Four things to watch**

1. `try_files` must fall back to `index.html`; otherwise refreshing `/system/user` returns 404.
2. `index.html` must not be cached; otherwise users receive a stale asset manifest.
3. `location /prod/api/` wins over `location /` (longer prefix), so API calls never fall
   into the SPA fallback.
4. The SSE regex `location ~` has the highest priority — `stream` and `ai` long-lived
   connections need their own block, otherwise Nginx buffers the events and they never arrive.

## 7. Release and Rollback

```bash
# Release
scp target/prod#api.war server:/opt/wejava/app/
ssh server 'systemctl restart tomcat'
ssh server 'rsync -a --delete dist/ /opt/wejava/frontend/prod/'

# Rollback: keep the previous war and dist
cp /opt/wejava/app/prod#api.war.bak /opt/wejava/app/prod#api.war
systemctl restart tomcat
rsync -a /opt/wejava/frontend/prod.bak/ /opt/wejava/frontend/prod/
```

## 8. Health Checks

| Check | Method |
|---|---|
| Backend liveness | `GET /prod/api/ping` |
| Database | `GET /prod/api/ping/db` (login required) |
| Frontend | `GET /` returns 200 (probe `/prod/` for the sub-path shape) |
| Port exposure | Only 80/443 public; Tomcat 8080 bound to `127.0.0.1` |

Periodic probing by the monitoring system is all that is needed.
