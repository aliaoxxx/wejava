# Contributing

Thanks for taking the time to contribute. This is a full-stack admin framework
(Spring Boot 4 + Vue 3), so most changes touch both sides — read the notes below before
opening a PR.

## Quick start

```bash
git clone https://github.com/aliaoxxx/wejava.git && cd wejava
docker compose up -d          # full stack → http://localhost:3300/
```

Without Docker:

```bash
# backend: port 8000, context path /dev/api
cd wejava-backend && mvn spring-boot:run

# frontend: http://localhost:3300/dev/
cd wejava-frontend && pnpm install && pnpm dev
```

## Before opening a PR

CI runs exactly these commands — run them locally first or the PR will go red:

```bash
cd wejava-backend
mvn spotless:apply     # formatting is enforced, not optional
mvn test

cd wejava-frontend
pnpm lint
pnpm build
```

## Commit messages

`<type>(<scope>): <subject>`, Chinese or English:

```
feat(system): 用户列表支持按部门筛选
fix(security): 刷新令牌轮换时未校验账号状态
docs(readme): 首屏截图化改造
```

Types: `feat` / `fix` / `docs` / `chore` / `refactor` / `test` / `perf`.

## Code conventions

- One class per file, each file does one thing
- No cross-layer calls (controller → service → mapper only)
- Business method signatures and response shapes are stable; changing them is a breaking
  change — open an issue first
- New endpoints need an entry in `docs/03-接口设计.md`
- **Docs are bilingual**: changes to `docs/*.md` must be mirrored under `docs/en/`, and
  `README.md` ↔ `README.zh-CN.md`

## Pull requests

- One problem per PR
- Explain the motivation, not just the diff
- Link the related issue
- Screenshots for UI changes

## Bug reports and questions

Use the issue templates. For security problems **do not open a public issue** — see
[SECURITY.md](SECURITY.md).

---

## 中文要点

提 PR 前请确认：

```bash
cd wejava-backend && mvn spotless:apply && mvn test
cd wejava-frontend && pnpm lint && pnpm build
```

约定：

- 一个类一个文件，一个文件只做一件事；不允许跨层调用
- 不改业务方法的签名和返回结构（破坏性变更先开 issue 讨论）
- 新增接口要同步更新 `docs/03-接口设计.md`
- 文档双语：改 `docs/*.md` 就同步 `docs/en/`，改 `README.md` 就同步 `README.zh-CN.md`
- 提交信息 `<type>(<scope>): <subject>`，例如 `feat(system): 用户列表支持按部门筛选`

安全问题**不要**开公开 issue，见 [SECURITY.md](SECURITY.md)。
