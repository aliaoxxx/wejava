[中文](../03-接口设计.md) | English

# 03 · API Design

> Base path: `http://localhost:8000/dev/api` in development, `/prod/api` in production
> Except for login, captcha, and refresh, every endpoint requires `Authorization: Bearer <access_token>`
>
> Interactive docs: Swagger UI is available in development and the demo
> (`/dev/api/swagger-ui/index.html`, `/api/swagger-ui/index.html`). Click **Authorize** and paste an
> access token to call the endpoints. Production disables it via `springdoc.api-docs.enabled=false`.

---

## 1. General Conventions

| Item | Convention |
|---|---|
| Encoding | UTF-8 |
| Request body | `application/json` (except file uploads) |
| Time format | `yyyy-MM-dd HH:mm:ss` (transport), `datetime(3)` (storage) |
| Long-typed IDs | **Serialized as strings**; the frontend treats them as strings |
| Null values | `null` fields are kept (not trimmed) so the frontend can detect them |
| Pagination parameters | `pageNum` (1-based), `pageSize` (default 10, max 100) |
| Sorting parameters | `orderBy` (field name, whitelist-validated), `orderDirection` (`asc`/`desc`) |
| Path style | All lowercase + hyphens: `/system/user/reset-pwd` |
| Idempotency | Write endpoints accept an `Idempotency-Key` header (optional) |

---

## 2. Unified Response Envelope

```json
{
  "code": 200,
  "msg": "Operation successful",
  "data": {},
  "traceId": "b7c1f3e2a9d84c5e"
}
```

| Field | Type | Description |
|---|---|---|
| `code` | int | See the error code table; `200` means success |
| `msg` | string | Human-readable message |
| `data` | any | Business data; `null` when there is none |
| `traceId` | string | Trace ID; give it to support/ops when reporting an error |

### Paginated response

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "total": 128,
    "list": [ { "userId": "1", "username": "admin" } ]
  },
  "traceId": "..."
}
```

---

## 3. Error Code Table

### 3.1 HTTP Layer

| code | HTTP | Meaning | Frontend handling |
|---|---|---|---|
| `200` | 200 | Success | — |
| `400` | 400 | Parameter validation failed | Show `msg` |
| `401` | 401 | Not authenticated | See the breakdown below |
| `403` | 403 | No permission | Show a message, **do not redirect** |
| `404` | 404 | Resource not found | Show a message |
| `405` | 405 | Method not allowed | Show a message |
| `429` | 429 | Too many requests | Show a message asking the user to retry later |
| `500` | 500 | System error | Show "system busy" + traceId |

### 3.2 Authentication Breakdown

| code | Meaning | Frontend behavior |
|---|---|---|
| `1001` | Invalid username or password | Show a message, stay on the login page |
| `1002` | Account disabled | Show a message |
| `1003` | Account locked | Show the remaining lock time |
| `1004` | Captcha incorrect or expired | Refresh the captcha |
| `1005` | access token expired | **Silently refresh** and replay the request |
| `1006` | Invalid token / session expired | Clear state, redirect to the login page |
| `1007` | Current password incorrect | Show a message |
| `1008` | The two password entries do not match | Show a message |
| `1009` | The new password must differ from the current password | Show a message |

### 3.3 Business Layer

| code | Meaning |
|---|---|
| `2001` | Data does not exist |
| `2002` | Data already exists (duplicate name/code) |
| `2003` | Child nodes exist; cannot delete |
| `2004` | Related data exists; cannot delete |
| `2005` | Operations on the super administrator are not allowed |
| `2006` | Operations on the currently logged-in user are not allowed |
| `2007` | The department contains users; cannot delete |
| `2008` | The role is assigned to users; cannot delete |

### 3.4 Files and Exports

| code | Meaning |
|---|---|
| `3001` | File upload failed |
| `3002` | File type not allowed |
| `3003` | File size exceeds the limit |
| `3004` | Too much data to export; narrow the range |
| `3005` | File does not exist or has expired |

---

## 4. Authentication Endpoints

### 4.1 Get Captcha

```
GET /auth/captcha
```

No authentication required. Response:

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "captchaKey": "8f14e45fceea167a5a36dedd4bea2543",
    "captchaImage": "data:image/png;base64,iVBORw0KGgo..."
  }
}
```

| Field | Description |
|---|---|
| `captchaKey` | Identifier for this captcha; sent back at login |
| `captchaImage` | base64 image; can be used directly in `<img src>` |

### 4.2 Login

```
POST /auth/login
```

```json
{
  "username": "admin",
  "password": "admin123",
  "captchaKey": "8f14e45fceea167a5a36dedd4bea2543",
  "captchaCode": "12"
}
```

```json
{
  "code": 200,
  "msg": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 1800,
    "tokenType": "Bearer"
  }
}
```

### 4.3 Refresh Token

```
POST /auth/refresh
```

```json
{ "refreshToken": "eyJhbGciOiJIUzI1NiIs..." }
```

The response is the same as for login. **The old refresh token is invalidated immediately** (rotation); reusing it returns `1006`.

### 4.4 Logout

```
POST /auth/logout
```

No request body; the token is taken from the `Authorization` header. Response is `R<Void>`.

### 4.5 Get Current User Info

```
GET /auth/me
```

```json
{
  "code": 200,
  "data": {
    "user": {
      "userId": "1",
      "username": "admin",
      "nickname": "Super Administrator",
      "avatar": null,
      "deptId": "100",
      "deptName": "WeJava Tech"
    },
    "roles": ["admin"],
    "perms": ["*:*:*"],
    "superAdmin": true
  }
}
```

### 4.6 Get Dynamic Routes (Menu Tree)

```
GET /auth/routers
```

Returns a route tree the frontend can mount directly; it contains only nodes whose `menu_type` is `M` or `C`:

```json
{
  "code": 200,
  "data": [
    {
      "name": "System",
      "path": "/system",
      "component": "Layout",
      "redirect": "/system/user",
      "meta": { "title": "System Management", "icon": "setting", "hidden": false },
      "children": [
        {
          "name": "SystemUser",
          "path": "user",
          "component": "system/user/index",
          "meta": { "title": "User Management", "icon": "user", "cache": false }
        }
      ]
    }
  ]
}
```

### 4.7 Change Password

```
PUT /auth/password
```

```json
{ "oldPassword": "admin123", "newPassword": "Abc@123456" }
```

On success the user is **forcibly logged out** (all of that user's sessions are cleared); the frontend redirects to the login page.

---

## 5. User Management

Path prefix `/system/user`

| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/list` | `system:user:list` | Paginated query |
| GET | `/{userId}` | `system:user:query` | Details (including role/post IDs) |
| POST | `` | `system:user:add` | Create |
| PUT | `` | `system:user:edit` | Update |
| DELETE | `/{userIds}` | `system:user:remove` | Delete (comma-separated supports batch) |
| PUT | `/reset-pwd` | `system:user:resetPwd` | Reset password |
| PUT | `/change-status` | `system:user:edit` | Enable/disable |
| GET | `/export` | `system:user:export` | Export to Excel |
| GET | `/import-template` | `system:user:import` | Download the import template |

### Query parameters

| Parameter | Type | Description |
|---|---|---|
| `pageNum` / `pageSize` | int | Pagination |
| `username` | string | Fuzzy match |
| `nickname` | string | Fuzzy match |
| `phone` | string | Fuzzy match |
| `status` | int | Exact match |
| `deptId` | long | **Includes sub-departments** |
| `beginTime` / `endTime` | string | Creation time range |

### List response item

```json
{
  "userId": "1",
  "deptId": "100",
  "deptName": "WeJava Tech",
  "username": "admin",
  "nickname": "Super Administrator",
  "email": "admin@wejava.dev",
  "phone": "13800000000",
  "sex": "0",
  "status": 0,
  "loginIp": "127.0.0.1",
  "loginDate": "2026-10-27 13:20:00",
  "createTime": "2026-10-01 09:00:00"
}
```

> The list **does not return** `password`, `delFlag`, or `pwdUpdateTime`.

### Create/update request

```json
{
  "deptId": "101",
  "username": "zhangsan",
  "nickname": "Zhang San",
  "password": "Abc@123456",
  "email": "zs@wejava.dev",
  "phone": "13900000000",
  "sex": "0",
  "status": 0,
  "roleIds": ["2"],
  "postIds": ["1"],
  "remark": ""
}
```

> `password` is required only on create; if omitted on update, the password is left unchanged.

### Reset password

```json
{ "userId": "2", "password": "Abc@123456" }
```

---

## 6. Role Management

Path prefix `/system/role`

| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/list` | `system:role:list` | Paginated query |
| GET | `/{roleId}` | `system:role:query` | Details (including the list of menu IDs) |
| POST | `` | `system:role:add` | Create |
| PUT | `` | `system:role:edit` | Update |
| DELETE | `/{roleIds}` | `system:role:remove` | Delete |
| PUT | `/change-status` | `system:role:edit` | Enable/disable |
| PUT | `/data-scope` | `system:role:dataScope` | Save data permissions |
| GET | `/option-select` | — | Dropdown options (any logged-in user) |

### List response item

```json
{
  "roleId": "2",
  "roleName": "Common Role",
  "roleKey": "common",
  "roleSort": 2,
  "dataScope": "5",
  "status": 0,
  "createTime": "2026-10-01 09:00:00"
}
```

### Saving data permissions

```json
{ "roleId": "2", "dataScope": "2", "deptIds": ["101", "102"] }
```

> When `dataScope` is `"2"` (custom), `deptIds` is required; it is ignored for any other value.

---

## 7. Menu Management

Path prefix `/system/menu`

| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/list` | `system:menu:list` | Tree list (not paginated) |
| GET | `/{menuId}` | `system:menu:query` | Details |
| POST | `` | `system:menu:add` | Create |
| PUT | `` | `system:menu:edit` | Update |
| DELETE | `/{menuId}` | `system:menu:remove` | Delete (returns `2003` if the node has children) |
| GET | `/tree-select` | — | Menu tree dropdown |
| GET | `/role-tree/{roleId}` | `system:role:query` | Menu tree already assigned to the role |

### Menu object

```json
{
  "menuId": "1001",
  "menuName": "User Management",
  "parentId": "1000",
  "orderNum": 1,
  "path": "user",
  "component": "system/user/index",
  "query": "",
  "isFrame": 0,
  "isCache": 0,
  "menuType": "C",
  "visible": 0,
  "status": 0,
  "perms": "system:user:list",
  "icon": "user",
  "children": []
}
```

**Validation rules**:

- When `menuType = 'F'`, `perms` is required
- When `menuType = 'C'`, `component` is required
- For external links (`isFrame = 1`), leave `component` empty

---

## 8. Department Management

Path prefix `/system/dept`

| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/list` | `system:dept:list` | Tree list |
| GET | `/{deptId}` | `system:dept:query` | Details |
| POST | `` | `system:dept:add` | Create |
| PUT | `` | `system:dept:edit` | Update |
| DELETE | `/{deptId}` | `system:dept:remove` | Delete (returns an error if the department has sub-departments/users) |
| GET | `/tree-select` | — | Department tree dropdown |

**Data permissions**: users with `system:dept:list` are filtered by their own data scope by default.

---

## 9. Posts / Dictionaries / Config / Notices

### 9.1 Posts `/system/post`

| Method | Path | Permission |
|---|---|---|
| GET | `/list` | `system:post:list` |
| GET | `/{postId}` | `system:post:query` |
| POST | `` | `system:post:add` |
| PUT | `` | `system:post:edit` |
| DELETE | `/{postIds}` | `system:post:remove` |
| GET | `/export` | `system:post:export` |
| GET | `/option-select` | — |

### 9.2 Dictionaries `/system/dict`

| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/type/list` | `system:dict:list` | Dictionary type pagination |
| POST | `/type` | `system:dict:add` | Create type |
| PUT | `/type` | `system:dict:edit` | Update type |
| DELETE | `/type/{dictIds}` | `system:dict:remove` | Delete type |
| GET | `/data/list` | `system:dict:list` | Dictionary data pagination |
| POST | `/data` | `system:dict:add` | Create data |
| PUT | `/data` | `system:dict:edit` | Update data |
| DELETE | `/data/{dictCodes}` | `system:dict:remove` | Delete data |
| GET | `/data/type/{dictType}` | — | **Fetch the dictionary by type (high-frequency, served from cache)** |

> `/data/type/{dictType}` needs no permission annotation; any logged-in user can call it — nearly every page uses it.

### 9.3 Config `/system/config`

| Method | Path | Permission |
|---|---|---|
| GET | `/list` | `system:config:list` |
| GET | `/{configId}` | `system:config:query` |
| POST | `` | `system:config:add` |
| PUT | `` | `system:config:edit` |
| DELETE | `/{configIds}` | `system:config:remove` |
| GET | `/key/{configKey}` | — |

### 9.4 Notices `/system/notice`

| Method | Path | Permission |
|---|---|---|
| GET | `/list` | `system:notice:list` |
| GET | `/{noticeId}` | `system:notice:query` |
| POST | `` | `system:notice:add` |
| PUT | `` | `system:notice:edit` |
| DELETE | `/{noticeIds}` | `system:notice:remove` |

---

## 10. Monitoring Endpoints


### 10.1 Online Users `/monitor/online`

| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/list` | `monitor:online:list` | Paginated (sorted by last active time, descending) |
| DELETE | `/{jti}` | `monitor:online:forceLogout` | Force logout |

```json
{
  "jti": "0f1c2d3e-...",
  "username": "admin",
  "nickname": "Super Administrator",
  "deptName": "WeJava Tech",
  "ipaddr": "127.0.0.1",
  "browser": "Chrome 130",
  "os": "macOS",
  "loginTime": "2026-10-27 13:20:00",
  "lastActiveTime": "2026-10-27 13:25:31"
}
```

### 10.2 Operation Logs `/monitor/operlog`

| Method | Path | Permission |
|---|---|---|
| GET | `/list` | `monitor:operlog:list` |
| DELETE | `/{operIds}` | `monitor:operlog:remove` |
| DELETE | `/clean` | `monitor:operlog:remove` |
| GET | `/export` | `monitor:operlog:export` |

### 10.3 Login Logs `/monitor/loginlog`

| Method | Path | Permission |
|---|---|---|
| GET | `/list` | `monitor:loginlog:list` |
| DELETE | `/{infoIds}` | `monitor:loginlog:remove` |
| DELETE | `/clean` | `monitor:loginlog:remove` |
| GET | `/export` | `monitor:loginlog:export` |
| PUT | `/unlock/{username}` | `monitor:loginlog:unlock` |

### 10.4 Server Monitoring `/monitor/server`

| Method | Path | Permission |
|---|---|---|
| GET | `` | `monitor:server:list` |

Returns CPU, memory, JVM, disk, Redis, and MySQL connection pool metrics.

---

## 11. Common Endpoints

| Method | Path | Permission | Description |
|---|---|---|---|
| POST | `/common/upload` | Any logged-in user | Single-file upload, returns `{url, name, size}` |
| POST | `/common/uploads` | Any logged-in user | Multiple-file upload |
| GET | `/common/download/{fileId}` | Any logged-in user | Download |
| GET | `/common/captcha` | No authentication required | Same as `/auth/captcha` (kept for backward compatibility) |

**Upload limits**:

- Single file ≤ 10MB (configurable via `sys.upload.maxSize`)
- Extension whitelist: `jpg,jpeg,png,gif,webp,pdf,doc,docx,xls,xlsx,zip`
- Storage path `/{yyyy}/{MM}/{dd}/{uuid}.{ext}`; **the physical path is never returned**
- File names are renamed to prevent path traversal

---

## 12. Endpoint and Permission String Summary

> This table is the **source of the button data in menu management** and also the checklist for code review.

| Module | Permission strings |
|---|---|
| User | `system:user:list` `system:user:query` `system:user:add` `system:user:edit` `system:user:remove` `system:user:export` `system:user:import` `system:user:resetPwd` |
| Role | `system:role:list` `system:role:query` `system:role:add` `system:role:edit` `system:role:remove` `system:role:export` `system:role:dataScope` |
| Menu | `system:menu:list` `system:menu:query` `system:menu:add` `system:menu:edit` `system:menu:remove` |
| Department | `system:dept:list` `system:dept:query` `system:dept:add` `system:dept:edit` `system:dept:remove` |
| Post | `system:post:list` `system:post:query` `system:post:add` `system:post:edit` `system:post:remove` `system:post:export` |
| Dictionary | `system:dict:list` `system:dict:query` `system:dict:add` `system:dict:edit` `system:dict:remove` `system:dict:export` |
| Config | `system:config:list` `system:config:query` `system:config:add` `system:config:edit` `system:config:remove` |
| Notice | `system:notice:list` `system:notice:query` `system:notice:add` `system:notice:edit` `system:notice:remove` |
| Online user | `monitor:online:list` `monitor:online:forceLogout` |
| Operation log | `monitor:operlog:list` `monitor:operlog:query` `monitor:operlog:remove` `monitor:operlog:export` |
| Login log | `monitor:loginlog:list` `monitor:loginlog:query` `monitor:loginlog:remove` `monitor:loginlog:export` `monitor:loginlog:unlock` |
| Server monitoring | `monitor:server:list` |

**Verification rules**:

- Every `@PostMapping` / `@PutMapping` / `@DeleteMapping` must have an `@PreAuthorize`
- Every `@GetMapping` must have an `@PreAuthorize` unless it is whitelisted
- CI script: count the `@*.Mapping` annotations and the `@PreAuthorize` annotations; the difference must equal the number of whitelist entries

---

## 13. API Documentation

Uses **springdoc-openapi 3.x**:

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha
  default-produces-media-type: application/json
```

Disabled in production:

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

**Annotation conventions**:

```java
@Tag(name = "User Management")
@RestController
@RequestMapping("/system/user")
public class SysUserController {

    @Operation(summary = "Paginate users")
    @PreAuthorize("@ps.has('system:user:list')")
    @GetMapping("/list")
    public R<PageResult<UserVO>> list(UserQuery query) { ... }
}
```

---

## 14. SSE Real-time Channel

Messages are pushed over SSE. `EventSource` cannot send request headers,
so the connection is authenticated with a **one-time ticket**.

| Step | Endpoint | Notes |
|---|---|---|
| 1 | `POST /auth/sse-ticket` | Requires Bearer auth, returns `{ ticket, expiresIn }` |
| 2 | `GET /stream/notifications?ticket=xxx` | Opens the stream; the ticket is valid for 30s and consumed once |

### Event Format

```
event:connected
data:{"userId":"1"}

event:message
data:{"messageId":"5","title":"...","msgType":1,"level":0}

:ping

```

| event | When | data |
|---|---|---|
| `connected` | Connection established | `{ userId }` |
| `message` | A notice or system message was delivered | message summary |
| `unread` | Unread count changed | `{ count }` |
| Comment `:ping` | Every 15s | Keeps idle connections from being dropped by proxies |

### Transport Requirements

```
Content-Type: text/event-stream; charset=utf-8
Cache-Control: no-cache, no-transform
X-Accel-Buffering: no
```

Buffering must be disabled behind Nginx, otherwise events are held back:

```nginx
location /prod/api/stream/ {
    proxy_pass http://127.0.0.1:8080/prod/api/stream/;
    proxy_buffering off;
    proxy_cache off;
    proxy_read_timeout 3600s;
}
```

### Multiple Instances

`SseEmitter` lives in a single JVM, so with several replicas a message may be delivered
to the wrong node. Delivery always goes through Redis Pub/Sub (`admin:sse:user:{userId}`);
each instance forwards only to its own live connections. Broadcasts carry a `from`
instance id so an instance never consumes its own message and double-delivers.

---

---

## 15. Site Messages and AI

### 15.1 Site Messages `/system/message`

Personal data: any authenticated user can access it, no permission string required.

| Method | Path | Notes |
|---|---|---|
| GET | `/unread-count` | Unread count |
| GET | `/list` | Inbox page (msgType / readFlag / keyword) |
| GET | `/{messageId}` | Detail; 403 if the message is not yours |
| PUT | `/{messageId}/read` | Mark one as read |
| PUT | `/read-all` | Mark all as read |
| POST | `/send` | Send from the admin side, permission `system:message:send` |

Send payload:

```json
{
  "title": "Maintenance notice",
  "content": "Maintenance starts at 23:00 tonight",
  "msgType": 2,
  "level": 1,
  "targetType": 3,
  "targetIds": [101],
  "link": "/system/notice"
}
```

`targetType`: 1 all / 2 by role / 3 by department / 4 explicit users.
Department matching uses `FIND_IN_SET` on `ancestors`, so selecting a parent
department also includes every member of its descendants.

### 15.2 Targeted Notice Delivery

`sys_notice` gained `target_type` and `target_ids`; publishing reuses the same
recipient resolver. Existing rows default to `target_type = 1` (everyone),
so their behaviour is unchanged.

### 15.3 AI Chat `/ai`

| Method | Path | Notes |
|---|---|---|
| POST | `/chat` | Streaming chat, returns `text/event-stream` |
| GET | `/session/{sessionId}/messages` | Session history |

Events: `delta` (incremental text), `done` (usage and latency), `error` (failure reason).

```json
{"sessionId": "1", "content": "Hello"}
```

### 15.4 AI Config `/system/ai-config`

| Method | Path | Permission |
|---|---|---|
| GET | `/system/ai-config` | `system:ai:config` |
| PUT | `/system/ai-config` | `system:ai:config` |

The API key is stored with AES-GCM and **only ever returned masked**
(first 5 characters, `****`, last 4 characters); submitting an empty value or the mask keeps
the existing key.
The encryption secret comes from the `AI_CONFIG_SECRET` environment variable.

### 15.5 Startup Check in Production

With the `prod` profile active, `JWT_SECRET` and `AI_CONFIG_SECRET` are validated at
startup. If either still holds the development default the process exits immediately,
so a deployment can never go live with breakable secrets.
