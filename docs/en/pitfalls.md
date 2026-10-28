[中文](../避坑清单.md) | English

# WeJavaAdmin Pitfalls Checklist

> Tech stack baseline: Spring Boot 4.1.x / Spring Security 7 / MyBatis-Plus 3.5.18 / MySQL 8.4 / Redis 8 / Vue 3.5 + Element Plus
> How to use: go through this list item by item when starting a new project; before going live, sign off using Section 12.

---

## 0. Priority Levels

- 🔴 **P0**: Must be handled — it will definitely cause trouble, and when it does, it is hard to diagnose (production-incident level)
- 🟡 **P1**: Very likely to be hit; costs half a day to two days
- 🟢 **P2**: Convention recommendation, affects long-term maintainability

---

## 1. Dependencies and Versions (Boot 4 Special)

Spring Boot 4 is not "just change the version number" — it simultaneously switches Spring Framework 7, Jakarta EE 11, Jackson 3, and the way starters are organized.

- [ ] 🔴 **All starters were renamed**
  `spring-boot-starter-web` → `webmvc`; `-aop` → `-aspectj`; `-json` → `-jackson`;
  `-oauth2-client` → `-security-oauth2-client`; `-oauth2-resource-server` → `-security-oauth2-resource-server`.
  The old names still compile, but they are deprecated — don't use them in new projects.

- [ ] 🔴 **Jackson 3 package migration**
  `com.fasterxml.jackson.databind.*` → `tools.jackson.databind.*`
  `com.fasterxml.jackson.core.*` → `tools.jackson.core.*`
  **Exception**: `com.fasterxml.jackson.annotation.*` (`@JsonProperty`, `@JsonIgnore`) is unchanged.
  Self-check command: `grep -rn "import com.fasterxml.jackson" src/ | grep -v annotation`

- [ ] 🔴 **Boot's own Jackson integration classes were renamed too**
  `Jackson2ObjectMapperBuilderCustomizer` → `JsonMapperBuilderCustomizer`
  **The correct package is `org.springframework.boot.jackson.autoconfigure`** (many migration articles online miss the `.autoconfigure` part, and it simply won't compile).
  `@JsonComponent` → `@JacksonComponent`
  `JsonObjectSerializer` → `ObjectValueSerializer` / `JsonObjectDeserializer` → `ObjectValueDeserializer`

- [ ] 🔴 **Jackson configuration properties switched namespaces**
  `spring.jackson.write.*` / `spring.jackson.read.*` / `spring.jackson.parser.*`
  → `spring.jackson.json.write.*` / `spring.jackson.json.read.*`
  The old configuration does not raise an error — it just **silently has no effect**, and date formats and null handling all fall back to defaults.

- [ ] 🔴 **`spring.jackson.json.*` only accepts `JsonWriteFeature` / `JsonReadFeature` enums**
  **Verified in practice (Boot 4.1.2 + Jackson 3.1.2)**: writing
  ```yaml
  spring.jackson.json.write.write-dates-as-timestamps: false
  ```
  makes the application fail to start outright:
  ```
  Failed to bind properties under 'spring.jackson.json.write'
  Caused by: No enum constant tools.jackson.core.json.JsonWriteFeature.write-dates-as-timestamps
  ```
  because `WRITE_DATES_AS_TIMESTAMPS` belongs to the databind layer's `SerializationFeature`, not to `JsonWriteFeature`.
  **Correct approach**: configure it programmatically with `JsonMapperBuilderCustomizer`; don't force it into `spring.jackson.json.*`.

- [ ] 🟡 **Jackson behavior defaults changed — do not assume byte-for-byte identical output**
  During the transition you can first add `spring.jackson.use-jackson2-defaults=true` to align with 3.x behavior, then remove the items one by one once stable.

- [ ] 🔴 **`spring-boot-starter-flyway` / `-liquibase` / `-restclient` / `-webclient` must be added explicitly**
  Boot 4 no longer "auto-configures just because it is on the classpath". Symptoms: the Flyway dependency is added but migration scripts never run, or `RestClient.Builder` cannot be injected.

- [ ] 🔴 **Flyway 10+ database dialects were split into separate modules**
  Adding only `spring-boot-starter-flyway` is not enough; for MySQL you must also add:
  ```xml
  <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-mysql</artifactId>
      <version>${flyway.version}</version>
  </dependency>
  ```
  Otherwise startup reports `No database found to handle jdbc:mysql://...`.
  Also: when using H2 for tests, remember `spring.flyway.enabled=false`, otherwise it will run the MySQL scripts against H2.

- [ ] 🟡 **Flyway only warns (does not block) on "database versions newer than the ones it has verified against"**
  Measured: MySQL 9.6 + Flyway 11: `Using MySQL 9.6 which is newer than the version Flyway has been verified with. The latest verified version of MySQL is 9.4.`
  **It is only a WARN; migrations still run**. But run a full migration verification before upgrading a major database version.

- [ ] 🟡 **The display width of `tinyint(1)` is deprecated**
  MySQL warns: `Integer display width is deprecated and will be removed in a future release`.
  Just write `tinyint`; the semantics are unaffected (booleans are still 0/1).

- [ ] 🟡 **Undertow is no longer supported** (it does not implement Servlet 6.1); you can only use Tomcat or Jetty.

- [ ] 🟡 **Testing-related**
  `@MockBean` → `@MockitoBean`; `@SpyBean` → `@MockitoSpyBean`
  `@SpringBootTest` no longer auto-configures MockMvc; you must add `@AutoConfigureMockMvc`
  `TestRestTemplate` → `RestTestClient` + `@AutoConfigureRestTestClient`

- [ ] 🟡 **Health probes are enabled by default**: `/livez`, `/readyz` work out of the box; confirm in production whether you want to expose them.

- [ ] 🟡 **MyBatis-Plus must use the Boot4-specific starter**
  `mybatis-plus-spring-boot4-starter` (available since 3.5.13) + `mybatis-plus-jsqlparser` is mandatory.
  Adding only the starter without jsqlparser → the pagination and data-permission plugins fail at startup.

- [ ] 🟡 **Run `spring-boot-properties-migrator` over the configuration**
  Add it temporarily as a runtime dependency; at startup it prints all obsolete configuration items. Remember to remove it after fixing them.

- [ ] 🟢 **Pin dependency versions**: archive `mvn dependency:tree`, diff it in CI, to prevent some merge from silently downgrading a version.

---

## 2. Authentication and Sessions

- [ ] 🔴 **Long primary keys lose precision when sent to the frontend**
  Snowflake IDs are 19 digits, exceeding JS `Number.MAX_SAFE_INTEGER` (2^53-1); the frontend receives `...000`, and edit/delete hits the wrong row.
  Fix: globally serialize `Long` as String.
  ```java
  @Bean
  JsonMapperBuilderCustomizer longToString() {
      return builder -> builder.addModule(new SimpleModule()
          .addSerializer(Long.class, ToStringSerializer.instance)
          .addSerializer(Long.TYPE, ToStringSerializer.instance));
  }
  ```
  Or use `json-bigint` on the frontend. **Doing it on the backend is less work — only one place in the whole project.**

- [ ] 🔴 **JWTs must be revocable**
  Pure JWT is stateless; after logout/password change/account disabling the token is still valid.
  Fix: include `jti` when issuing, store `login:token:{userId}:{jti}` in Redis, and have the filter check its existence on every request.
  This is also the **single unified implementation point for kicking users offline, single sign-on, and forcing re-login after a password change**.

- [ ] 🔴 **Distinguish 401 from 403**
  401 = not logged in / login expired → the frontend redirects to the login page
  403 = logged in but not authorized → the frontend shows a message, and **must not redirect to the login page** (that causes an infinite loop)

- [ ] 🔴 **Authentication exceptions do not go through `@RestControllerAdvice`**
  Exceptions thrown in the filter layer are outside the DispatcherServlet, so you must configure `AuthenticationEntryPoint` (401) and `AccessDeniedHandler` (403), and return JSON instead of a redirect.

- [ ] 🟡 **Passwords: BCrypt, never MD5/SHA**
  Use `BCryptPasswordEncoder`. If you need to switch algorithms later, use `DelegatingPasswordEncoder` and store `{bcrypt}$2a$...` in the database, which makes a smooth migration easy.

- [ ] 🟡 **Brute-force protection**: keep the login failure counter in Redis (`login:fail:{username}`, lock for 15 minutes after 5 failures), and add a graphical captcha. A frontend-only captcha is useless.

- [ ] 🟡 **JWT secret management**: at least 256 bit, never hard-coded into the code or Git; use environment variables / a config center, and plan for rotation in advance (two keys coexisting during the transition).

- [ ] 🟡 **Dual-token design**: access token 30 minutes; refresh token 7 days, stored in Redis, and rotated one-time-per-use (rotation). When access expires, refresh silently renews it — the user notices nothing.

- [ ] 🟡 **Concurrent refresh requests**: if 5 frontend requests get 401 at the same time, 5 refreshes fire, and later ones may void earlier tokens. The frontend must implement a **single-flight refresh queue** (see Section 8).

- [ ] 🟢 **Logout must actually delete the Redis key** — clearing only the frontend localStorage is not enough.

---

## 3. Permissions and Privilege Escalation

- [ ] 🔴 **Hiding on the frontend ≠ allowing on the backend**
  `v-hasPerm` only controls "whether it is visible"; every endpoint must also be given `@PreAuthorize("hasAuthority('system:user:add')")`.
  Missing one leaves an entry point for unauthorized backend calls.

- [ ] 🔴 **Horizontal privilege escalation: every endpoint that operates by id must verify ownership**
  For `GET /api/order/{id}`, checking only "is logged in" is not enough; you must also check "is this order yours".
  The most common vulnerability shape: changing the id in the URL lets you see someone else's data.

- [ ] 🔴 **`hasRole` and `hasAuthority` must not be mixed**
  `hasRole("ADMIN")` automatically prefixes `ROLE_` and matches `ROLE_ADMIN`; `hasAuthority` does not.
  **Use only `hasAuthority` + the `perms` strings in the database** (`system:user:add`).

- [ ] 🟡 **Use a dedicated flag for the super admin**
  Use `user_id = 1` or an `is_admin` field; do not implement it as "this person has all roles" — the super admin will mysteriously lose permissions when a new permission is added.

- [ ] 🟡 **Permission caches must be invalidatable**
  On login, assemble `perms` into `UserDetails` and cache it in Redis. After changing role permissions you must clear the cache; otherwise the user has to wait for the token to expire before it takes effect.

- [ ] 🔴 **Data-permission rewriting only applies to SQL generated by MP**
  `DataPermissionInterceptor` will not touch hand-written XML SQL.
  Hand-written SQL must build the conditions manually, or you can simply expose the data-permission table column to the XML.

- [ ] 🟡 **Menu table design**: `menu_type` (M directory / C menu / F button) + a `perms` field. Button permissions rely on the `perms` of the F-type row.

---

## 4. Database Design

- [ ] 🔴 **Logical delete + unique index conflict**
  A deleted user still occupies the `username` unique key, so recreating a user with the same name fails outright.
  Fix: include `del_flag` in the unique index (`uk_username_del(username, del_flag)`), or rename the username with a suffix on delete.

- [ ] 🔴 **Unify the time zone across the whole chain**
  Three places must agree: JVM (`-Duser.timezone=Asia/Shanghai`), MySQL (`serverTimezone=Asia/Shanghai`), container (`TZ=Asia/Shanghai`).
  Typical symptom of a mismatch: the time in the database is 8 hours behind the page, or scheduled tasks run in the middle of the night.

- [ ] 🟡 **Column conventions**
  Money is always `decimal(18,2)`; `float/double` is forbidden; time is `datetime(3)`; charset `utf8mb4` + `utf8mb4_0900_ai_ci`; booleans use `tinyint(1)`.

- [ ] 🟡 **Unified audit columns**: `create_by / create_time / update_by / update_time / del_flag / remark`, filled automatically with MP's `MetaObjectHandler` — don't hand-write it in every Service.

- [ ] 🟡 **Indexing principles**
  Put high-cardinality columns first; composite indexes follow the leftmost prefix rule; don't use functions on indexed columns (`where date(create_time) = ...` won't use the index); run `explain` once before going live.

- [ ] 🟡 **Deep pagination**
  `limit 100000, 10` scans 100,000 rows. Use cursor pagination for admin lists (`where id < lastId`) or deferred joins.

- [ ] 🟡 **Do not use foreign keys** — business databases rely on the application layer, otherwise migrations and bulk operations get stuck.

- [ ] 🟡 **Split large columns into separate tables**: put `text`/`blob` in a dedicated extension table, otherwise main-table queries are slowed down.

- [ ] 🟢 **Flyway scripts are append-only**; `flyway.clean` is forbidden in production. Changing a single character in an already-applied script causes a checksum failure.

- [ ] 🟢 **Use HikariCP for the connection pool** (Boot's default); do not add Druid — under Boot 4 both compatibility and benefits are not worth it.

---

## 5. Transactions and Concurrency

- [ ] 🔴 **Five cases where `@Transactional` silently fails**
  1. Self-invocation within the class (`this.saveXxx()` does not go through the proxy)
  2. The method is not `public`
  3. The exception is swallowed by `catch` and not rethrown
  4. A checked exception is thrown (by default only `RuntimeException` and `Error` roll back)
  5. The class is not managed by Spring (created by hand with `new`)
  Self-check mantra: **a transactional method must be a public method called on an injected bean, and exceptions must be thrown out.**

- [ ] 🔴 **Do no time-consuming work inside a transaction**
  Remote calls, sending MQ messages, generating Excel, uploading to OSS — none of these belong inside a transaction. The connection stays occupied, and under high concurrency the pool is exhausted in an instant.

- [ ] 🟡 **Control transaction granularity**: don't open transactions at the Controller layer, and don't wrap an entire request in one transaction.

- [ ] 🟡 **Use `REQUIRES_NEW` for logging/auditing**: the business transaction rolled back, but the operation log must still be kept.

- [ ] 🔴 **Write operations must be idempotent**
  Repeated frontend clicks, network retries, and MQ redelivery all cause duplicate orders/duplicate deductions.
  Fix: a unique index as a safety net + a Redis dedup token (acquired before submit, deleted on submit).

- [ ] 🟡 **Use optimistic locking for concurrent updates**
  MP's `OptimisticLockerInnerInterceptor` + a `version` column. Don't "select first, then update" — there is a window in between.

- [ ] 🟡 **Use atomic SQL for inventory-style deductions**
  `update ... set stock = stock - #{n} where id = ? and stock >= #{n}`, and judge success from the affected row count; don't select first and then update.

- [ ] 🟡 **Use Redisson for distributed locks; do not hand-roll `setnx`**
  The hand-rolled version has three big traps: no renewal (the lock auto-releases when the business runs past its TTL), accidentally deleting someone else's lock, and non-reentrancy.

---

## 6. Redis and Caching

- [ ] 🔴 **Unified key convention**: `business:module:identifier`, e.g. `admin:user:perms:1`. **Every key must have an expiration time** (except a very few permanent configuration keys), otherwise Redis slowly fills up.

- [ ] 🔴 **`KEYS *` is forbidden**; use `SCAN` cursor iteration. `KEYS` blocks the entire instance on a large database.

- [ ] 🔴 **The three cache problems**
  Penetration (querying data that does not exist) → null caching + short TTL, or a Bloom filter
  Breakdown (a hot key expiring at that instant) → mutex rebuild / logical expiry
  Avalanche (a large batch of keys expiring at the same time) → add a random offset to the TTL

- [ ] 🔴 **Update strategy: update the DB first, then delete the cache** (not update the cache). Accept brief inconsistency, backed by delayed double deletion. The reverse (delete the cache first, then update the DB) necessarily produces dirty data under concurrency.

- [ ] 🟡 **Use JSON for serialization**, not JDK serialization — it is unreadable, not cross-language, and deserialization fails as soon as a class field changes.

- [ ] 🟡 **Big key governance**: a single value over 10KB, or a collection with more than ten thousand elements, must be split. Deleting a big key blocks.

- [ ] 🟡 **Hot keys**: for things like flash sales/homepage configuration, use local cache + Redis as a second level; don't let all requests hit the same key.

- [ ] 🟡 **Redis is not a database**: enabling persistence does not mean the data is safe. Core data must land in MySQL; Redis is only for acceleration.

- [ ] 🟡 **Connection pool**: Lettuce shares connections by default — watch out for `shareNativeConnection`; don't run Lua scripts longer than 100ms in Redis.

---

## 7. API Contract (the place where frontend and backend argue most easily)

- [ ] 🔴 **Unified response envelope**: `{ code, msg, data, traceId }`. Use an enum for error codes; don't invent your own in each place.

- [ ] 🔴 **Unified time format**: `yyyy-MM-dd HH:mm:ss`, configured once globally; don't put `@JsonFormat` on every DTO.

- [ ] 🟡 **Pagination parameter ceiling**: `pageSize` must be capped (e.g. 100), otherwise `pageSize=999999` drags the database down in one request.

- [ ] 🟡 **Parameter validation with `@Valid` + global exception handling**; `MethodArgumentNotValidException` must be turned into a friendly message, and the stack trace must not be returned to the frontend.

- [ ] 🟡 **Error messages must not expose internal details**: in production return "系统繁忙" (system busy), and the stack trace goes only to the logs. Returning raw SQL error text to the frontend is equivalent to telling attackers the table structure.

- [ ] 🟡 **File upload**: size limit, extension whitelist (do not trust `Content-Type` alone), rename on storage, **do not return the server physical path**, and remove script execution permission from the upload directory.

- [ ] 🟡 **Stream Excel exports** (EasyExcel / SXSSF); a one-shot `select *` of the whole table is guaranteed OOM. For large volumes use async export + a download center.

- [ ] 🟢 **Add traceId to APIs**: carry it in both the response header and the log MDC, so you can stitch a whole call chain together when troubleshooting.

---

## 8. Frontend Engineering

- [ ] 🔴 **Blank screen / 404 after refreshing dynamic routes**
  Routes are added via `addRoute` after login; on refresh the page has no login state yet and the route table is empty.
  Fix: in the route guard, first `await` fetching user info and menus → `addRoute` → `next({ ...to, replace: true })`.

- [ ] 🔴 **axios 401 concurrent refresh storm**
  10 requests get 401 at the same time, 10 refreshes fire, tokens overwrite each other, and finally all of them become invalid.
  Fix: a module-level `isRefreshing` flag + a wait queue, replaying everything uniformly after the refresh completes.

- [ ] 🔴 **Guard infinite loop**
  Forgetting to whitelist `/login` when redirecting to the login page, or also redirecting to the login page on 403, causes infinite redirects. The browser freezes outright.

- [ ] 🟡 **Debounce error toasts**: it is very common for one failed endpoint to pop 3 toasts. Handle it uniformly in the interceptor — "the same message pops only once within 2 seconds".

- [ ] 🟡 **Prevent double submit on buttons**: set `loading` to true on submit and release it when the request ends. Backend idempotency alone is not enough, and the UX is poor.

- [ ] 🟡 **Align pagination parameters**: whether the backend is 1-based or 0-based, `pageNum` or `page`, `size` or `pageSize` — write it into the API doc, don't rely on memory.

- [ ] 🟡 **Large tables**: above 1000 rows switch to `el-table-v2` (virtual scrolling); a plain `el-table` lags noticeably.

- [ ] 🟡 **Environment variables**: manage `baseURL` with `.env.development` / `.env.production`; don't hard-code `localhost:8000` in the source.

- [ ] 🟢 **Bundle size**: on-demand Element Plus imports, lazy-loaded routes, `manualChunks` to split vendors — first paint can differ by 3x.

- [ ] 🟢 **Nginx history mode**: `try_files $uri $uri/ /index.html;`, otherwise refreshing a sub-route returns 404.

---

## 9. Security

- [ ] 🔴 **SQL injection**: in MyBatis always use `#{}`; `${}` must not be used to splice user input. Dynamic sorting like `order by ${column}` must have whitelist validation.

- [ ] 🔴 **XSS**: filter rich-text content on the backend (e.g. OWASP Sanitizer); on the frontend do not use `v-html` to render user input.

- [ ] 🟡 **CSRF**: the JWT-in-Header mode is inherently immune; if the token is stored in a Cookie, CSRF protection + `SameSite` must be enabled.

- [ ] 🟡 **Log masking**: mask passwords, phone numbers, ID card numbers, and bank card numbers before printing them to logs. Don't write `log.info("user: {}", JSON.toJSONString(user))`.

- [ ] 🟡 **Rate limiting**: login, captcha, SMS, and export must be rate-limited (Redis counters or Sentinel).

- [ ] 🟡 **Disable in production**: Swagger / Knife4j / all Actuator endpoints / H2 console / Druid monitoring page.

- [ ] 🟡 **Default credentials**: `admin/admin123` must force a password change on first login, or be randomly generated at initialization and printed to the log.

- [ ] 🟢 **Dependency vulnerability scanning**: `mvn dependency-check` or Dependabot, run once a week.

---

## 10. Deployment and Operations

- [ ] 🔴 **Container time zone**: base images default to UTC; both `TZ=Asia/Shanghai` and the JVM's `-Duser.timezone` must be set, otherwise log times and business times won't match.

- [ ] 🟡 **The JVM heap must match the container limit**: `-XX:MaxRAMPercentage=75`; don't let the JVM mindlessly eat the host memory and get OOMKilled.

- [ ] 🟡 **Graceful shutdown**: `server.shutdown=graceful`, together with K8s `preStop`; otherwise every in-flight request 502s during a release.

- [ ] 🟡 **Log rotation**: configure `SizeAndTimeBasedRollingPolicy` in `logback`; without it, logs can fill the disk. Use INFO in production, don't enable DEBUG.

- [ ] 🟡 **Database backup + restore drills**: having a backup does not mean you can restore it; do a restore verification every quarter.

- [ ] 🟡 **Externalize configuration + encrypt sensitive information**: database passwords must not sit in plaintext in Git; use environment variables or a config center.

- [ ] 🟢 **HTTPS + reverse proxy headers**: Nginx must pass `X-Forwarded-For/Proto`, and the backend must set `server.forward-headers-strategy=native`, otherwise every client IP you get is 127.0.0.1.

---

## 11. Code Engineering

- [ ] 🟡 **Do not return Entity directly to the frontend**
  It leaks fields that should not be exposed (password, salt, internal flags) and easily triggers lazy-loading/serialization exceptions.
  Use the three layers uniformly: DTO (input) / VO (output) / Entity (persistence).

- [ ] 🟡 **Global exception fallback**: `@RestControllerAdvice` catches `Exception`, but **do not swallow it** — log it + traceId and return the unified error structure.

- [ ] 🟡 **N+1 queries**: querying the database inside a loop means 11 SQL statements for 10 rows. Use a batch `in` query or a join.

- [ ] 🟡 **Circular dependencies**: A depends on B and B depends on A; covering it up with `@Lazy` only delays the explosion. The Service should be split.

- [ ] 🟡 **Do not stuff Wrappers into Services**: more than 3 chained `LambdaQueryWrapper` calls means you should consider writing XML or a dedicated Mapper method.

- [ ] 🟢 **Cover permission logic with unit tests**: whether `@PreAuthorize` takes effect and whether the data-permission SQL is correct — these two areas are the easiest to get silently wrong.

---

## 12. 20-Item Pre-Release Self-Check

| # | Check item | Pass criteria |
|---|---|---|
| 1 | All endpoints have authorization annotations | The global counts of `@RequestMapping` and `@PreAuthorize` match up |
| 2 | Long IDs serialized as String | Capture the response: ids are quoted strings |
| 3 | Token invalid immediately after logout | Requesting with the old token after logout returns 401 |
| 4 | Disabled user kicked offline immediately | Old-token requests return 401 after disabling |
| 5 | Privilege escalation test | Use account A with B's id: returns 403 |
| 6 | Time zone | Database time, API time, and page display all agree |
| 7 | Pagination ceiling | `pageSize=100000` is rejected or truncated |
| 8 | File upload | `.jsp` / `.sh` uploads are rejected; the response contains no physical path |
| 9 | Production configuration | Swagger, Actuator, H2 console are disabled |
| 10 | Default credentials | First login forces a password change |
| 11 | Log masking | No plaintext phone numbers/passwords in the logs |
| 12 | Frontend refresh | Refreshing any sub-route gives no 404 and no blank screen |
| 13 | Concurrent 401 | Multiple simultaneous 401s trigger only one token refresh |
| 14 | Transaction boundaries | No remote calls inside transactional methods |
| 15 | Cache consistency | The list page takes effect immediately after updating data |
| 16 | Idempotency | Clicking submit twice creates only one record |
| 17 | Slow queries | The core list endpoint's explain shows no `ALL` full table scan |
| 18 | Backup restore | Restoring from backup to a test database starts normally |
| 19 | Graceful shutdown | No 5xx during the release |
| 20 | Dependency vulnerabilities | No HIGH/CRITICAL vulnerabilities |

---

## Appendix: One-Line Mnemonic

> **IDs to strings, transactions need proxies, permissions always double-checked, cache: DB first then delete, time zones in three places, frontend guards against concurrency, docs off before release.**
