# 60-Second Demo Storyboard

A 60-second screen recording that lets someone understand what this project is and what it does — without reading any docs.

## Before recording

- `docker compose up -d` has finished and all four services are healthy
- Browser window at **1440×900** (same as `docs/screenshots/`), zoom 100%
- UI language: Chinese (switch language in the profile menu if needed)
- Log in and out once beforehand so the captcha, menus and fonts are cached — the recording should not stutter
- Move the cursor at half speed and pause 0.2 s before every click

## Shot list

| Time | Screen | Action | What the viewer should notice |
|---|---|---|---|
| 0–5s | Sign-in page | Open `http://localhost:3300/` | One command and you land on a real login page, not raw API docs |
| 5–12s | Sign-in page | Type `admin` / `admin123`, fill the captcha, click Sign in | Image captcha plus credentials — the whole auth flow |
| 12–20s | Dashboard | Stop on the dashboard, move the cursor down the sidebar | Sidebar, tabs, current permissions — the overall shell |
| 20–30s | User management | Expand "系统管理" → click "用户管理", type `li` in the username box, press Enter | Search form, table, pagination, row actions (edit / delete / reset password) |
| 30–45s | Create-user dialog | Click "新增", fill username `demo01`, nickname `演示用户`, password `abc123456`, pick a department, submit | One `schema.ts` renders the whole form — validation and dictionary selects included |
| 45–55s | Server monitor | Expand "系统监控" → click "服务监控", sweep the cursor over CPU, memory, JVM heap and dependencies | Live metrics: the Linux host, MySQL and Redis versions all come from the containers |
| 55–60s | Hold | Stop on the monitor page | Let the viewer read the numbers |

## Narration beats (optional)

1. A single `docker compose up -d` brings up MySQL, Redis, backend and frontend
2. Auth is a self-managed JWT with a Redis session whitelist — no OAuth2 authorization server
3. Menus and button permissions come from the backend; routes are registered dynamically
4. The user page is generated from `schema.ts` — no hand-written CRUD
5. Everything on the monitor page is measured inside the containers

## Editing rules

- **Do not** cut the captcha step — it is part of the real flow
- **Do not** fake or splice data; the created user really lands in the database
- Reset afterwards: `docker compose down -v && docker compose up -d`

## Tooling

- macOS: QuickTime Player ("File → New Screen Recording") or OBS
- Output: MP4 / H.264, 30 fps, 1440×900; keep it under ~10 MB for GitHub, compress with `ffmpeg` if needed
