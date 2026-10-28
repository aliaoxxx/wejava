[中文](../07-部署方案.md) | English

# Deployment Plan: WAR + Nginx

> No containerized deployment. The backend is packaged as a war and handed to Tomcat, the frontend static assets are served by Nginx, and both share the same path prefix.

## 1. Path Planning

| Environment | Frontend URL | API URL | Backend artifact |
|---|---|---|---|
| Production | `https://host/prod/` | `https://host/prod/api/**` | `prod#api.war` |
| Development | `https://host/dev/` | `https://host/dev/api/**` | `dev#api.war` |

**How the prefix takes effect**

- Frontend: the Vite `base` is `/prod/` for `pnpm build` and `/dev/` for `pnpm run dev`;
  routing uses `createWebHistory(import.meta.env.BASE_URL)`, so refreshing a deep link does not 404.
- API: the request base URL is determined by `VITE_APP_BASE_API` in `.env.production` / `.env.development`.
- Backend context: **the WAR filename determines Tomcat's context path**, and `#` is resolved to `/`.
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
pnpm build          # output is in dist/, base is /prod/
pnpm dev            # dev server http://localhost:3300/dev/
```

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
```

## 6. Nginx Configuration

```nginx
server {
    listen 80;
    server_name admin.example.com;

    # ---------- Frontend: production ----------
    location /prod/ {
        alias /opt/wejava/frontend/prod/;
        try_files $uri $uri/ /prod/index.html;
        expires 7d;
    }
    location = /prod/index.html {
        alias /opt/wejava/frontend/prod/index.html;
        add_header Cache-Control "no-cache";
    }

    # ---------- API: production ----------
    location /prod/api/ {
        proxy_pass http://127.0.0.1:8080/prod/api/;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
    }

    # ---------- Development environment (optional, same approach) ----------
    # location /dev/ { alias /opt/wejava/frontend/dev/; try_files $uri $uri/ /dev/index.html; }
    # location /dev/api/ { proxy_pass http://127.0.0.1:8081/dev/api/; }

    # Root path redirects to production
    location = / {
        return 302 /prod/;
    }
}
```

**Three things to watch**

1. `try_files` falls back to `/prod/index.html`; otherwise refreshing `/prod/system/user` would 404.
2. Caching must be disabled for `index.html`; otherwise users receive a stale asset manifest after a release.
3. `proxy_pass` ends with `/`, matching the `location` path one-to-one, with no path rewriting.

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
| Frontend | `GET /prod/` returns 200 |

Periodic probing by the monitoring system is all that is needed.
