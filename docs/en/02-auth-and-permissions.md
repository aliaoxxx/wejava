[中文](../02-认证与权限设计.md) | English

# 02 · Authentication and Authorization Design

> Approach: **self-managed JWT + Redis session whitelist** (Route A)
> No OAuth2 authorization server is introduced, and none of the `spring-security-oauth2-*` dependencies are used.

---

## 1. Design Goals and Boundaries

### In Scope

| Capability | Implementation |
|---|---|
| Username/password login | `AuthenticationManager` + `DaoAuthenticationProvider` + BCrypt |
| Graphical captcha | Answer stored in Redis, valid for 2 minutes |
| Login brute-force protection | Redis counter, locked for 15 minutes after 5 failures |
| Stateless authentication | JWT placed in `Authorization: Bearer` |
| Instant revocation | Redis whitelist (jti); logout/disable/password change take effect immediately |
| Dual Token | access 30min + refresh 7d (rotation) |
| Multi-device login control | Single sign-on switch; a new login kicks out the old session |
| API authorization | `@PreAuthorize("@ps.has('system:user:add')")` |
| Data permission | MyBatis-Plus interceptor assembles SQL based on `data_scope` |
| Online users | Active sessions recorded in a Redis ZSET |

### Out of Scope (Explicitly Excluded)

- ❌ OAuth2 / OIDC protocol endpoints
- ❌ Third-party client integration
- ❌ SSO single sign-on (if needed in the future, follow the migration path in Section 9)
- ❌ Phone number/email verification code login (can be an extension; the architecture already leaves room for it)

---

## 2. Token Design

### 2.1 Structure

Header:

```json
{ "alg": "HS256", "typ": "JWT" }
```

Payload (**deliberately kept minimal**):

```json
{
  "iss": "wejava-admin",
  "sub": "1",
  "uid": 1,
  "uname": "admin",
  "jti": "0f1c2d3e-4a5b-6c7d-8e9f-0123456789ab",
  "typ": "access",
  "iat": 1761537600,
  "exp": 1761539400
}
```

**Why not put the permission list inside the Token?**

1. With many permissions the Token grows to 1~2KB and is sent on every request, wasting bandwidth
2. After permissions change, the old permissions inside the Token remain valid, so you must wait for expiry
3. We **already have to query the Redis whitelist on every request anyway**, so the permissions can be picked up along the way at no extra cost

### 2.2 The Two Tokens

| Type | TTL | Storage | Purpose |
|---|---|---|---|
| `access_token` | 30 minutes | Client memory/localStorage only | Call business APIs |
| `refresh_token` | 7 days | Redis records the `jti` status | Exchange for a new access token |

**Refresh Rotation**: on every refresh, the old refresh token is invalidated immediately and a new pair is issued.
The upside is that a stolen refresh token can only be used once; the downside is that concurrent refreshes in the frontend collide — a singleton refresh queue is mandatory (see the frontend document).

### 2.3 Signing Algorithm

- Default **HS256**, `secret` at least 32 bytes (256 bit), injected from an environment variable
- Supports **dual-key transition**: configure `jwt.secret` and `jwt.secret-previous`, and try them in order during signature verification
- **Only whitelisted algorithms are accepted**; `HS256` is explicitly specified when parsing, and the `alg` in the Token header is never trusted (prevents `alg:none` and algorithm-confusion attacks)

```yaml
jwt:
  issuer: wejava-admin
  secret: ${JWT_SECRET}              # 32+ bytes, hardcoding forbidden
  secret-previous: ${JWT_SECRET_OLD:}
  access-ttl: 30m
  refresh-ttl: 7d
  clock-skew: 60s                    # clock tolerance across instances
```

---

## 3. Redis Key Conventions

Unified prefix `admin:`; **every key must have a TTL** (except explicitly declared persistent keys).

| Key | Type | Value | TTL | Purpose |
|---|---|---|---|---|
| `admin:auth:token:{jti}` | String | `LoginUser` JSON | 30m | Session whitelist, **validated during authentication** |
| `admin:auth:refresh:{jti}` | String | `{userId}` | 7d | Whether the refresh token is still valid |
| `admin:auth:user:{userId}` | Set | `jti` set | Refreshed with the session | Batch kick-out by user |
| `admin:auth:online` | ZSet | member=`jti`, score=last-active timestamp | None | Online user list |
| `admin:auth:captcha:{uuid}` | String | Captcha answer | 120s | Graphical captcha |
| `admin:auth:fail:{username}` | String | Failure count | 15m | Brute-force counter |
| `admin:auth:lock:{username}` | String | `1` | 15m | Lock flag |
| `admin:cache:perms:{userId}` | Set | Permission strings | 30m | Permission cache |
| `admin:cache:dict:{dictType}` | String | Dictionary JSON | 1h ± random | Dictionary cache |
| `admin:cache:config:{key}` | String | Parameter value | 1h | Parameter cache |
| `admin:lock:{biz}:{id}` | String | Holder identifier | ≤30s | Distributed lock (managed by Redisson) |
| `admin:limit:{uri}:{ip}` | String | Counter | 60s | API rate limiting |

**Conventions**:

- Serialization is uniformly **JSON** (`GenericJackson2JsonRedisSerializer` or the Jackson 3 equivalent); **JDK serialization is forbidden**
- Keys must not contain Chinese characters or spaces
- Batch deletion must not use `KEYS`; use `SCAN` or maintain an index Set

---

## 4. Login Flow

### 4.1 Sequence

```
Frontend                         Backend                                     Redis / MySQL
 │                               │                                         │
 │ ① GET /auth/captcha           │                                         │
 ├──────────────────────────────►│                                         │
 │◄──────────────────────────────┤ SET captcha:{uuid} 120s ───────────────►│
 │   {captchaKey, captchaImage}  │                                         │
 │                               │                                         │
 │ ② POST /auth/login            │                                         │
 │   {username,password,         │                                         │
 │    captchaKey,captchaCode}    │                                         │
 ├──────────────────────────────►│                                         │
 │                               │ check captcha (read then delete) ──────►│
 │                               │ check lock:{username} ─────────────────►│
 │                               │                                         │
 │                               │ AuthenticationManager                   │
 │                               │   → UserDetailsService queries DB ─────►│ MySQL
 │                               │   → BCrypt verification                 │
 │                               │                                         │
 │                               │ failure: INCR fail:{username} ─────────►│
 │                               │       write sys_login_log               │
 │                               │       return 1001                       │
 │                               │                                         │
 │                               │ success: DEL fail:{username}            │
 │                               │       build LoginUser                   │
 │                               │       issue access + refresh            │
 │                               │       SET token:{jti} 30m ─────────────►│
 │                               │       SET refresh:{jti} 7d ────────────►│
 │                               │       SADD user:{uid} ─────────────────►│
 │                               │       ZADD online ─────────────────────►│
 │                               │       write sys_login_log               │
 │◄──────────────────────────────┤ {accessToken,refreshToken,expiresIn}
 │                               │                                         │
```

### 4.2 Key Implementation

```java
public LoginVO login(LoginDTO dto) {
    // 1. Captcha (verify first, then delete, to prevent replay)
    captchaService.verify(dto.getCaptchaKey(), dto.getCaptchaCode());

    // 2. Lock check
    if (tokenStore.isLocked(dto.getUsername())) {
        throw new BizException(ErrorCode.ACCOUNT_LOCKED);
    }

    // 3. Authentication
    Authentication auth;
    try {
        auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
    } catch (BadCredentialsException e) {
        int failures = tokenStore.incrLoginFail(dto.getUsername());
        loginLogService.recordFailure(dto.getUsername(), "用户名或密码错误");
        throw new BizException(ErrorCode.BAD_CREDENTIALS);
    } catch (DisabledException e) {
        throw new BizException(ErrorCode.ACCOUNT_DISABLED);
    }

    tokenStore.clearLoginFail(dto.getUsername());

    LoginUser loginUser = (LoginUser) auth.getPrincipal();
    return tokenService.createSession(loginUser);
}
```

**Key points for the `UserDetailsService` implementation**:

- Include `del_flag = 0` when querying the user
- `enabled = status == 0`
- Query the role `role_key` set and the permission `perms` set in a single pass to avoid N+1
- The password field carries the `{bcrypt}` prefix and is resolved by `DelegatingPasswordEncoder`
- Even when the user does not exist, run one BCrypt verification (to prevent the timing side channel of username enumeration)

---

## 5. Request Authorization Flow

### 5.1 Sequence

```
Request ──► CorsFilter ──► JwtAuthFilter ──► AuthorizationFilter ──► Controller
                            │                       │
                            │ parse Bearer token    │ check @PreAuthorize
                            │ verify signature/exp  │ check URL rules
                            │ read token:{jti}      │
                            │ hit → build Auth      │
                            │ miss → no Auth set    │
                            ▼                       ▼
                        (request not interrupted)     unauthenticated → 401
                                                      no permission → 403
```

### 5.2 Key Points of the JwtAuthFilter Implementation

```java
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenStore tokenStore;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PUBLIC_PATHS.stream()
                .anyMatch(p -> new AntPathMatcher().match(p, request.getServletPath()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.parseAccess(header.substring(7));
                // ★ Whitelist check: absent from Redis = logged out / kicked / expired
                LoginUser user = tokenStore.getSession(claims.getId());
                if (user != null) {
                    var auth = new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    tokenStore.touch(claims.getId());   // refresh the last-active time
                }
            } catch (ExpiredJwtException e) {
                req.setAttribute(AUTH_ERROR, ErrorCode.TOKEN_EXPIRED);
            } catch (JwtException | IllegalArgumentException e) {
                req.setAttribute(AUTH_ERROR, ErrorCode.TOKEN_INVALID);
            }
        }
        try {
            chain.doFilter(req, res);
        } finally {
            SecurityContextHolder.clearContext();   // ★ prevent thread-pool cross-contamination
        }
    }
}
```

**Three details that must be right**:

1. **On failure, only clear the context; do not throw** — throwing would bypass `ExceptionTranslationFilter` and become a 500
2. **Call `clearContext()` in `finally`** — Tomcat reuses threads; failing to clear leaks state across requests
3. **Distinguish "expired" from "invalid"** — the frontend relies on this to decide between "silent refresh" and "redirect to the login page"

### 5.3 Authentication Failure Response

```java
@Component
public class RestAuthEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res,
                         AuthenticationException ex) throws IOException {
        Object err = req.getAttribute(AUTH_ERROR);
        // 1005 expired → the frontend refreshes silently; 1006 invalid/not logged in → the frontend goes to the login page
        R<?> body = (err instanceof ErrorCode code)
                ? R.fail(code)
                : R.fail(ErrorCode.NOT_LOGIN);
        writeJson(res, HttpStatus.UNAUTHORIZED, body);
    }
}
```

| Scenario | HTTP | code | Frontend behavior |
|---|---|---|---|
| No token attached | 401 | 1006 | Redirect to the login page |
| token expired | 401 | 1005 | **Silent refresh**, then replay the request if it succeeds |
| token invalid/kicked | 401 | 1006 | Clear local state, redirect to the login page |
| Logged in but no permission | 403 | 403 | Show "no access permission", **do not navigate** |

> Redirecting to the login page on a 403 causes an infinite loop; this is the most classic frontend bug.

**Error code aliases**: `ErrorCode.NOT_LOGIN` (no token attached) and `ErrorCode.TOKEN_INVALID` (bad signature, revoked, kicked)
share `1006`, and the frontend handles them identically; `ErrorCode.TOKEN_EXPIRED` uses `1005` on its own and is the only branch that triggers a "silent refresh".

---

## 6. Permission Model (RBAC)

### 6.1 Three-Layer Structure

```
User ──N:N── Role ──N:N── Menu/Permission
  │
  └── Department (determines the data visibility scope)
```

### 6.2 Permission String Conventions

Format: `module:entity:action`

| Action | Meaning |
|---|---|
| `list` | Query the list |
| `query` | Query details |
| `add` | Create |
| `edit` | Update |
| `remove` | Delete |
| `export` | Export |
| `import` | Import |
| `resetPwd` | Reset password (special action, camelCase) |

Examples: `system:user:list`, `system:role:dataScope`, `monitor:online:forceLogout`

**Conventions**:

- All lowercase; only compound actions such as `resetPwd` use camelCase (kept consistent with the RuoYi ecosystem for easy cross-reference)
- One API **maps to exactly one permission string**
- Button permissions (`menu_type='F'`) **correspond one-to-one** with API permissions and are maintained in menu management

### 6.3 Authorization Annotations

```java
@PreAuthorize("@ps.has('system:user:add')")
@PostMapping
public R<Void> add(@RequestBody @Valid SysUserDTO dto) { ... }
```

**Why not use `hasAuthority`?**

`hasAuthority('system:user:add')` cannot express "super administrator". If every permission string were stuffed into the super admin, a newly added menu would only take effect after re-login, and the Token/cache size would be unbounded.

Therefore a custom `PermissionService` is used uniformly, with wildcard support:

```java
@Component("ps")
public class PermissionService {

    public boolean has(String perm) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;

        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        for (GrantedAuthority ga : authorities) {
            String owned = ga.getAuthority();
            if ("*:*:*".equals(owned)) return true;          // super admin
            if (owned.equals(perm)) return true;
            if (matchWildcard(owned, perm)) return true;     // things like system:user:*
        }
        return false;
    }
}
```

`matchWildcard` rules: `*` matches a single segment, `**` matches multiple segments.

### 6.4 Super Administrator

- Determination: the user holds a role with `role_key = 'admin'`
- Granted permission: `*:*:*`
- **Must not be deleted, must not be disabled, and must not have its roles changed** (enforced at the API layer)
- The super admin sees all menus in the menu tree (no filtering through `sys_role_menu`)

```java
if (loginUser.isSuperAdmin()) {
    return menuMapper.selectAllEnabled();
}
```

---

## 7. Data Permission

### 7.1 Implementation Chain

```
Service method annotated with @DataScope
        │
        ▼
DataScopeAspect (AOP)
  1. Take the current LoginUser
  2. Query data_scope of all the user's roles and take the most permissive one
  3. Generate the SQL fragment and put it into ThreadLocal
        │
        ▼
DataScopeInnerInterceptor (MP interceptor)
  In beforeQuery, use JSqlParser to AND the fragment into the WHERE clause
        │
        ▼
Final SQL
```

### 7.2 Annotation

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {
    /** Department table alias, defaults to d */
    String deptAlias() default "d";
    /** User table alias, defaults to u */
    String userAlias() default "u";
    /** Whether empty is allowed (no condition is appended when empty), defaults to false */
    boolean allowEmpty() default false;
}
```

### 7.3 Generation Rules

| data_scope | SQL fragment |
|---|---|
| `1` All | Empty (no condition appended) |
| `2` Custom | `{deptAlias}.dept_id IN (SELECT dept_id FROM sys_role_dept WHERE role_id IN (...))` |
| `3` Own department | `{deptAlias}.dept_id = {currentDeptId}` |
| `4` Own department and below | `{deptAlias}.dept_id IN (SELECT dept_id FROM sys_dept WHERE dept_id = {currentDeptId} OR FIND_IN_SET({currentDeptId}, ancestors))` |
| `5` Self only | `{userAlias}.user_id = {currentUserId}` |

**Merging multiple roles**: `1` > `2` > `4` > `3` > `5`; if any role is "all", no condition is appended.

**Empty-value protection**: if the user has no role at all / the department is empty / the custom scope is an empty set, the fragment must be generated as
`AND 1 = 0` (no data is returned), and it **must never degrade into "no condition"** — that would cause unauthorized access.

### 7.4 Handling Handwritten SQL

The MP interceptor only rewrites SQL that it generates itself. Queries in handwritten XML must call it explicitly:

```java
// Inside the Service
String fragment = DataScopeContextHolder.get().toSql();   // already includes the AND prefix
List<OrderVO> list = orderMapper.selectPageWithScope(fragment);
```

```xml
<select id="selectPageWithScope" resultType="OrderVO">
    SELECT o.*, u.nickname
      FROM biz_order o
      LEFT JOIN sys_user u ON u.user_id = o.create_by
     WHERE o.del_flag = 0
     <if test="scope != null and scope != ''">
       ${scope}
     </if>
     ORDER BY o.create_time DESC
</select>
```

> `${}` must be used here, but the content is a fixed fragment **generated by the backend** that contains no user input, so there is no injection risk.
> This exception must be explicitly flagged during code review.

---

## 8. Session Management

### 8.1 Logout

```
1. Take the jti from Authentication
2. DEL admin:auth:token:{jti}
3. DEL admin:auth:refresh:{refreshJti}
4. SREM admin:auth:user:{uid} {jti}
5. ZREM admin:auth:online {jti}
```

Clearing only the frontend localStorage is not enough — the Token can still be used in attacks until it expires.

### 8.2 Forced Offline (Kicking a User)

| Scenario | Operation |
|---|---|
| An administrator kicks a user from "Online Users" | Delete all keys for that jti |
| Account disabled | Delete every jti in the `user:{uid}` set |
| Password changed | Same as above, plus compare `pwd_update_time` as a fallback |
| Account deleted | Same as above |
| New login while single sign-on is enabled | Kick the old session before creating the new one |

```java
public void kickUser(Long userId) {
    Set<String> jtis = redis.opsForSet().members(USER_KEY + userId);
    if (jtis != null) {
        jtis.forEach(jti -> {
            redis.delete(TOKEN_KEY + jti);
            redis.delete(REFRESH_KEY + jti);
            redis.opsForZSet().remove(ONLINE_KEY, jti);
        });
        redis.delete(USER_KEY + userId);
    }
}
```

### 8.3 Online User List

```java
public PageResult<OnlineUserVO> online(Integer pageNum, Integer pageSize) {
    // The ZSet is ordered by last-active time descending; fetch one page at a time
    Set<ZSetOperations.TypedTuple<String>> tuples = redis.opsForZSet()
            .reverseRangeWithScores(ONLINE_KEY, start, end);
    // Then batch-load LoginUser by jti and filter out the expired ones
}
```

**Across instances**: `admin:auth:online` is a global ZSet, so it is naturally shared by multi-instance deployments.
If Redis data is lost, users are forced to log in again — an acceptable degradation.

---

## 9. Future Migration Path to OAuth2

The current Token already follows the standard claims (`iss/sub/iat/exp/jti`), and the permission strings are already independent of the framework.

Migrating to a Resource Server takes only three steps:

1. Deploy Keycloak (or another IdP) and sync users/roles into it
2. Remove `JwtAuthFilter` and `TokenStore`, and switch to `oauth2ResourceServer().jwt()`
3. Write a `JwtAuthenticationConverter` that maps the `perms` claim to `GrantedAuthority`

**Zero changes to business code**, because `@PreAuthorize("@ps.has(...)")` only depends on `Authentication`.

---

## 10. Responsibilities of Key Classes

| Class | Responsibility | Notes |
|---|---|---|
| `SecurityConfig` | Filter chain, CORS, password encoder, `AuthenticationManager` | List the whitelist explicitly; `anyRequest().permitAll()` is forbidden |
| `JwtService` | Issue/parse/verify | Bind the algorithm whitelist; validate `iss`, `exp`, `typ` |
| `JwtAuthFilter` | Parse Bearer, query Redis, populate the context | Do not throw on failure; clear the context in `finally` |
| `TokenStore` | Redis session read/write, kick users, online list | Every key carries a TTL |
| `AuthService` | Login, refresh, logout, get the current user | Do not let the transaction boundary wrap Redis calls |
| `CaptchaService` | Generate/verify captchas | Delete immediately after verification to prevent replay |
| `PermissionService` | Implementation of `@ps.has()` | Supports `*:*:*` and wildcards |
| `DataScopeAspect` | Resolve data permission, write to ThreadLocal | `finally` must remove it |
| `DataScopeInnerInterceptor` | Rewrite SQL | Handles SELECT only; skip when there is no scope |
| `RestAuthEntryPoint` | 401 JSON | Distinguish expired/invalid |
| `RestAccessDeniedHandler` | 403 JSON | Do not return stack traces |

---

## 11. Security Requirements (Confirm Item by Item Before Going Live)

- [ ] The JWT `secret` is injected from an environment variable, is ≥ 32 bytes long, and has not been committed to Git
- [ ] The algorithm is **specified explicitly** when parsing; `alg:none` is rejected
- [ ] Passwords use `DelegatingPasswordEncoder`, and `{bcrypt}$2a$...` is stored in the database
- [ ] 5 failed logins lock the account for 15 minutes, and failures are counted on **both the username and the IP dimension**
- [ ] Captchas are single-use and are deleted immediately after verification
- [ ] The Token is only placed in the `Authorization` header, **never in URL parameters** (they end up in the Nginx logs)
- [ ] Old Tokens become invalid immediately after logout, disablement, or a password change (guaranteed by the Redis whitelist)
- [ ] 401/403 return JSON, not an HTML login page
- [ ] CORS allows only the configured domains; `allowedOrigins("*")` and `allowCredentials(true)` must never appear together
- [ ] Swagger is disabled in production
- [ ] Every write API has `@PreAuthorize` (verified in CI by a scanning script)

---

## 12. Test Points

| Case | Expected |
|---|---|
| Login with the correct password | Both tokens are returned and a session exists in Redis |
| 5 wrong passwords | The 6th attempt returns "account locked", even with the correct password |
| Reusing a captcha | The second attempt returns "invalid captcha" |
| Request with an old Token after logout | 401 + code 1006 |
| Request with an old Token after the user is disabled | 401 + code 1006 |
| Request with an old Token after a password change | 401 + code 1006 |
| A role without permission accesses an API | 403 + code 403 |
| Accessing someone else's data (changing the id) | 403 or no data found (data permission takes effect) |
| Super admin accesses any API | 200 |
| access expired, refresh still valid | 401 + code 1005; the frontend succeeds after refreshing |
| refresh reused | 401 on the second attempt (rotation works) |
| 10 concurrent requests all get 401 | Only 1 refresh is triggered |
