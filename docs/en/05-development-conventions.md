[中文](../05-开发规范.md) | English

# 05 · Development Conventions

> Positioning: **only write down decisions that "cause disagreement"**. Anything a tool or code can block does not belong here.
> Formatting → `mvn spotless:apply`; unified response → `R<T>`; pagination → `PageQuery` / `PageResult`.
> Code review must reject any change that violates a clause marked 🔴.

---

## 1. Layering and Dependencies

```
controller → service → mapper
                │
                └── converter（Entity ↔ DTO/VO）
```

| # | Rule |
|---|---|
| 1.1 | 🔴 A Controller **must not** depend on a Mapper directly |
| 1.2 | 🔴 An Entity **must not** appear in a Controller's parameters or return value |
| 1.3 | 🔴 Services may call each other, but **circular dependencies are prohibited** |
| 1.4 | Transactions are opened only in the Service layer; Controllers do not open transactions |
| 1.5 | `common` does not depend on `security` or `system`; `security` depends only on `common` |
| 1.6 | 🔴 **`security` must not import anything from `system.*`**: security infrastructure knows nothing about business |

### Layering Responsibilities

| Package | What it holds | Test |
|---|---|---|
| `common` | Response envelope, exceptions, utilities, traceId, async thread pool | Business-agnostic |
| `security` | JWT encoding/decoding, filters, session storage, permission matching, captcha | **Does not import `system`** |
| `system` | Login business logic, users/roles/menus, login logs | May import `security` |

Typical misjudgment: `LoginUserDetailsService` needs to query `SysUserMapper`, so it looks like it "belongs to security", but it actually depends on business tables and **must live in `system`**. The same applies to `AuthService`, which depends on `LoginForm`/`LoginVO`.

Self-check (runnable in CI):

```bash
cd wejava-backend
grep -rn "import com.wejava.system" src/main/java/com/wejava/security/ && echo "❌ cross-layer dependency found" || echo "✅ no cross-layer dependency"
```

---

## 2. Package Structure and Naming

```
com.wejava.system/
├── controller/   XxxController
├── service/      XxxService（interface）
├── service/impl/ XxxServiceImpl（implementation）
├── mapper/       XxxMapper
├── converter/    XxxConverter
└── model/
    ├── entity/   SysUser                     table mapping (== camel case of the table name minus sys_)
    ├── query/    SysUserQuery                query conditions, extends PageQuery
    ├── form/     SysUserCreateForm / SysUserUpdateForm
    └── vo/       SysUserVO                   output view
```

> Cross-module infrastructure (authentication, JWT, permissions) lives in `com.wejava.security` and is not mixed into `system`.

**Models are organized into directories by purpose, not by a vague notion such as "DTO":**

| Directory | What it holds | Characteristics |
|---|---|---|
| `entity/` | Table mapping | Carries `@TableName`, extends `BaseEntity` |
| `query/` | Query conditions | Extends `PageQuery`; holds only filter fields + pagination + sorting |
| `form/` | Submission form | Carries `jakarta.validation` annotations; Create and Update are separate |
| `vo/` | Output view | Holds only the fields the frontend needs |

🔴 A request/response object may belong to only one of these categories; "one UserDTO serving as both input and output" is prohibited.

### Service Interface and Implementation Separation

🔴 `service/` holds **interfaces only**; implementations go in `service/impl/`, named exactly `XxxService` / `XxxServiceImpl`.

```java
// service/LoginService.java
public interface LoginService {
    LoginVO login(LoginForm form, String ip, String userAgent);
}

// service/impl/LoginServiceImpl.java
@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService { ... }
```

| # | Rule |
|---|---|
| S-1 | Injection points always use the **interface type**; injecting `XxxServiceImpl` is prohibited |
| S-2 | Implementation classes carry only `@Service`; interfaces carry no annotations at all |
| S-3 | Interface methods must have Javadoc; implementation methods may omit it |
| S-4 | One interface, one implementation; when a second implementation appears, choose explicitly with `@Primary` or `@Qualifier` |

**Two exceptions** (no interface created):

| Class | Reason |
|---|---|
| `LoginUserDetailsService` | It already implements Spring's `UserDetailsService` interface; wrapping it again yields no benefit |
| `CaptchaRenderer` | A stateless rendering component with no external dependencies; it will never be mocked or replaced |

🔴 **The same directory holds same-kind files from multiple modules, so filenames must carry a business prefix**:

| Directory | Example |
|---|---|
| `model/entity/` | `SysUser` `SysRole` `SysMenu` `SysLoginLog` |
| `model/form/` | `SysUserCreateForm` `SysRoleUpdateForm` `LoginForm` |
| `vo/` | `SysUserVO` `LoginVO` |

Names without a prefix, such as `CreateForm` or `UpdateForm`, are prohibited.

> The cost of this structure: adding one "user" feature means touching 5 directories at once, and module boundaries become weaker.
> The payoff: same-kind objects are centralized, which makes uniform constraints (for example, adding annotations to every Form) and batch refactoring easier.
> If a module is ever split into its own service, files will have to be picked across directories — this is a known trade-off.

Objects shared across modules (such as the login session) live under `security/model/` and are not placed in business modules.

| Suffix | Purpose | Prohibited |
|---|---|---|
| `Form` | Input parameter | Must not appear in a return value |
| `VO` | Output parameter | Must not carry sensitive fields such as password |
| `Query` | Query conditions, extends `PageQuery` | Must not be annotated with `@TableName` |
| `Entity` | Persistence mapping | Must not be returned to the frontend directly |

Method naming: `list` / `getById` / `create` / `update` / `remove` / `page`.
Names such as `doXxx`, `handleXxx`, `processXxx` that don't reveal the action are prohibited.

---

## 2.5 File Granularity: One File, One Type

### Hard Rules (Machine-Checkable)

🔴 **A `.java` file may contain only one type declaration** (class / interface / enum / record), and the filename matches the type name.

Corollaries:

| Case | Allowed |
|---|---|
| Two top-level classes in one file | ❌ Prohibited |
| Nested class `static class Foo {}` | ❌ Prohibited; create a separate file |
| Nested record `record Captcha(...) {}` | ❌ Prohibited; create a separate file |
| Local class / anonymous inner class | ⚠️ Allowed (it is an expression, not a declaration), but prefer a lambda |
| `Builder` and similar generated by Lombok/MapStruct | ✅ Not counted (it is not source code) |

**Why the compiler alone is not enough**: Java only enforces "one **public** top-level type"; stuffing three package-private classes into one file is legal. Nested types are not restricted at all.

**Why nesting is prohibited**: the file carries two reasons to change; the IDE's "go to file" stops working; and it is easily overlooked during code review.

Self-check (can run directly in CI):

```bash
cd wejava-backend/src/main/java
for f in $(find . -name "*.java"); do
  n=$(grep -cE "^\s*(public |private |protected |static |final |abstract )*(class|interface|enum|record) [A-Z]" "$f")
  [ "$n" -gt 1 ] && echo "Violation: $f ($n types)"
done
```

> A Checkstyle `OneTopLevelClass` rule can be wired into CI later.

### Soft Judgment: When to Create a New Class

The hard rules govern "one file, one type", but whether to split something into two types still requires judgment:

| Split | Don't split |
|---|---|
| One class carries responsibilities from two **different layers** (it is both a `UserDetails` and a Redis session object) | A group of constants/enums with one responsibility |
| One class has two **different directions** of change (the security contract changes vs. the session fields change) | Multiple pure functions of a utility class |

Evidence: `LoginUser` both implemented `UserDetails` (the Security contract) and served as the Redis session object (the storage contract); it has been split into `LoginSession` (pure data) + `AuthUser` (principal adapter).

Counterexample: splitting the 20 methods of `DateUtils` into 20 files, or creating one file for each of the 31 enum values of `ErrorCode` — those are "multiple values of one responsibility", and this rule does not apply.

### Interface Methods Can't Move, but Their Logic Must

`UserDetails.getAuthorities()` is an interface method mandated by the framework and **must stay in `AuthUser`**.
But all business semantics in the method body should move out, leaving the implementation class with a single delegating line:

```java
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    return AuthorityResolver.resolve(session);
}
```

From then on `AuthUser` knows nothing about `SimpleGrantedAuthority`, nothing about `*:*:*`, and nothing about the super-admin special case — it is responsible for "who I am", not for "what permissions I have".

### Methods Don't Split Files, but They Can Move

The file-granularity rule governs **types**, not methods. A class with 20 methods is still one file.

But when **a method mixes logic from two different layers**, the logic should move into another **class**, not into another file. The tests:

| Signal | Explanation |
|---|---|
| This logic is needed elsewhere too | For example, permission matching, needed by both `AuthUser` and `PermissionService` |
| This logic can be unit-tested without booting the framework | A pure function with no context dependency |
| The same semantics are defined twice | The most dangerous signal: change one place and you will surely miss the other |

**Evidence**: `*:*:*` was once defined once in `AuthUser.ALL_PERMISSION` and once in `PermissionService.ALL_PERMISSION`, and the matching rules were scattered across the two classes. They have now converged into `PermissionMatcher`: `resolve()` derives the held permissions, `matches()` decides a hit, and both `AuthUser` and `PermissionService` only delegate.

🔴 **A constant with the same semantics may be defined only once.** When you find a second definition, first converge the constant to a single source, then talk about features.

### Using Data Permissions

Just add the annotation to a Service **query method**; no SQL changes are needed:

```java
@DataScope(deptAlias = "u", userAlias = "u")
public PageResult<SysUserVO> page(SysUserQuery query) { ... }
```

| Rule | Explanation |
|---|---|
| Takes effect only on SQL generated by MP | Hand-written XML must append the conditions itself |
| The aliases must be correct | `deptAlias` / `userAlias` must match the table aliases in the SQL |
| No annotation = no restriction | Forgetting the annotation means privilege escalation; check for it when adding a query method |
| A resolution failure refuses execution | An error is better than letting it through; the security boundary must not "degrade" |

### Splits Already Landed

| Original class | After the split | Rationale |
|---|---|---|
| `LoginUser` | `LoginSession` (data) + `AuthUser` (Security adapter) | Two contracts |
| `TokenStore` | `SessionStore` (session/online) + `LoginAttemptStore` (brute-force protection) | Two sets of Redis keys, two directions of change |
| `AuthService` | `LoginService` (authentication) + `TokenService` (token lifecycle) | Login flow vs. token lifecycle |
| `CaptchaService` | `CaptchaService` (lifecycle/validation) + `CaptchaRenderer` (drawing) | Business rules vs. rendering implementation |
| `LoginLogService` | `LoginLogService` (persistence) + `UserAgentUtils` (UA parsing) | IO vs. pure function |
| `AuthUser.getAuthorities()` | `AuthorityResolver` (session → GrantedAuthority) | Interface contract vs. conversion logic |
| `PermissionMatcher` | `PermissionMatcher` (decides whether it is enough) + `AuthorityResolver` (which ones exist) | Matching vs. derivation |

---

## 3. The Three Model Layers

| Layer | Class | Characteristics |
|---|---|---|
| Persistence | `Xxx extends BaseEntity` | Carries `@TableName`, `@TableLogic`; fields map one-to-one to the table |
| Input | `XxxCreateDTO` / `XxxUpdateDTO` | Carries `jakarta.validation` annotations; **does not contain** id (create) or must contain id (update) |
| Output | `XxxVO` | Holds only the fields the frontend needs; **never** holds passwords, salts, or internal flags |

🔴 Using an Entity directly as a DTO or VO is **prohibited**. Three reasons: field leakage, bypassed validation, serialization traps.

Audit fields (`createBy/createTime/updateBy/updateTime`) are populated automatically by `AuditMetaObjectHandler`; hand-writing `setCreateTime(LocalDateTime.now())` in a Service is **prohibited**.

---

## 4. Converter

🔴 `BeanUtils.copyProperties` is **prohibited** (reflection, silently skipped fields, silent failure when a field is renamed).

Always use MapStruct:

```java
@Mapper(componentModel = "spring")
public interface SysUserConverter {
    SysUserVO toVO(SysUser entity);
    List<SysUserVO> toVOList(List<SysUser> entities);

    @Mapping(target = "userId", ignore = true)
    SysUser toEntity(SysUserCreateDTO dto);

    void update(SysUserUpdateDTO dto, @MappingTarget SysUser entity);
}
```

| # | Rule |
|---|---|
| 4.1 | One Converter per module, placed in the `converter` package |
| 4.2 | When field names differ, `@Mapping` **must** be written explicitly; do not rely on "happening to have the same name" |
| 4.3 | For update scenarios use `void update(dto, @MappingTarget entity)` to avoid losing fields by creating a new object |
| 4.4 | Special conversions such as enums/dates go into a `default` method or a `@Named` method; do not stuff them into the Service |

---

## 5. Pagination

```java
public class SysUserQuery extends PageQuery {
    private String username;
    private Integer status;
}
```

```java
@GetMapping("/list")
public R<PageResult<SysUserVO>> list(SysUserQuery query) {
    Page<SysUser> page = query.toPage();
    Page<SysUser> result = userService.page(query, page);
    return R.ok(PageResult.of(result));
}
```

| # | Rule |
|---|---|
| 5.1 | 🔴 `pageSize` is capped at 100 (already enforced by `PageQuery`); a Service must not relax the cap |
| 5.2 | 🔴 `orderBy` must be a database column name drawn from a whitelist; concatenating a frontend string straight into SQL is prohibited |
| 5.3 | The return value is uniformly `{ total, list }`, not `records` / `rows` / `items` |
| 5.4 | Large export-style queries use streaming queries; reading a whole table into memory is prohibited |

---

## 6. Unified Response and Error Codes

```json
{ "code": 200, "msg": "Operation successful", "data": {}, "traceId": "..." }
```

| # | Rule |
|---|---|
| 6.1 | Controllers always return `R<T>`; returning a bare object or `ResponseEntity<Map>` is prohibited |
| 6.2 | Error codes may only come from the `ErrorCode` enum; magic numbers such as `R.fail(5001, "...")` are **prohibited** |
| 6.3 | Adding an error code requires updating the error code table in `docs/03-api-design.md` in the same change |

---

## 7. Exceptions

| # | Rule |
|---|---|
| 7.1 | 🔴 Services throw only `BizException`; `throw new RuntimeException("...")` is prohibited |
| 7.2 | 🔴 Empty `catch` blocks are prohibited; either handle the exception or wrap it, rethrow, and keep the cause |
| 7.3 | Parameter validation uses `@Valid` + annotations; do not hand-write piles of `if (x == null) throw ...` in a Service |
| 7.4 | Third-party exceptions (such as SQLException) must be converted to `BizException` at the boundary and must not leak out to the Controller |

---

## 8. Transactions

| # | Rule |
|---|---|
| 8.1 | 🔴 `@Transactional` is placed only on **public** methods of a Service |
| 8.2 | 🔴 Self-invoking a transactional method inside the same class is prohibited (`this.xxx()` bypasses the proxy, so the transaction silently fails) |
| 8.3 | Query methods are annotated with `@Transactional(readOnly = true)` |
| 8.4 | Inside a transaction, remote calls, publishing to MQ, exporting files, and uploading to OSS are prohibited |
| 8.5 | One transaction does one thing; batch operations commit in batches to avoid long transactions locking tables |

---

## 9. Logging

| # | Rule |
|---|---|
| 9.1 | 🔴 `System.out.println` is prohibited; `e.printStackTrace()` is prohibited |
| 9.2 | 🔴 Printing passwords, tokens, or whole entities is prohibited; use placeholders for the information you need |
| 9.3 | Exception logs must carry context: `log.error("Failed to create user username={}", username, e)` |
| 9.4 | Business exceptions log at `warn`, system exceptions at `error`; do not log checkpoint lines on the normal path |
| 9.5 | Log level: INFO in production; do not log DEBUG inside loops |

---

## 10. MyBatis-Plus Usage Boundaries

| Scenario | Approach |
|---|---|
| Single-table CRUD, conditional queries, pagination | ✅ MP (`LambdaQueryWrapper` / `selectPage`) |
| Cross-table joins, aggregate statistics, complex reports | ✅ Hand-written XML (`src/main/resources/mapper/`) |
| More than 3 chained Wrappers | ⚠️ Consider extracting a Mapper method or writing XML |

| # | Rule |
|---|---|
| 10.1 | 🔴 SQL-concatenating constructs such as `wrapper.last("limit 1")` are prohibited |
| 10.2 | 🔴 When `selectOne` may return multiple rows, add `orderBy` + `last("limit 1")` or switch to `selectList` |
| 10.3 | The logical-delete field is always named `del_flag` and uses `@TableLogic` |
| 10.4 | Batch inserts use `saveBatch`, and the JDBC URL carries `rewriteBatchedStatements=true` |

---

## 11. XML Conventions

```xml
<!-- src/main/resources/mapper/system/SysUserMapper.xml -->
<mapper namespace="com.wejava.system.user.mapper.SysUserMapper">
    <resultMap id="UserVOMap" type="com.wejava.system.user.vo.SysUserVO">
        <id property="userId" column="user_id"/>
        <result property="deptId" column="dept_id"/>
    </resultMap>
</mapper>
```

| # | Rule |
|---|---|
| 11.1 | 🔴 `SELECT *` is prohibited; columns must be listed explicitly |
| 11.2 | 🔴 Parameters always use `#{}`; `${}` is allowed only for fixed fragments **generated by the backend** (such as data permission conditions), and it must be explained in a comment |
| 11.3 | Multi-table join queries must declare a `resultMap`; do not rely on `autoMapping` to guess fields |
| 11.4 | XML files live in `resources/mapper/<module>/`, and the file path matches the Mapper interface |
| 11.5 | Dynamic conditions use `<if test="...">`; concatenating SQL strings in Java is prohibited |

---

## 12. New Endpoint Checklist

Copy into the PR description and tick off item by item:

```
[ ] Path follows /module/entity/action, all lowercase with hyphens
[ ] Has @PreAuthorize("@ps.has('module:entity:action')"), and the permission string is already in the menu data
[ ] Input is a DTO annotated with @Valid
[ ] Output is a VO with no sensitive fields
[ ] Returns R<T>, with PageResult for pagination
[ ] Service methods carry the correct transaction annotations
[ ] Exceptions throw only BizException, with error codes from ErrorCode
[ ] Logs contain no sensitive information
[ ] Hand-written SQL lists columns explicitly, uses #{}, and has a resultMap
[ ] The endpoint table in docs/03-api-design.md has been updated
[ ] mvn spotless:apply && mvn test pass
```

---

## 13. Pre-Commit Self-Check

```bash
cd wejava-backend
mvn spotless:apply   # format
mvn test             # unit tests
```

> It is recommended to add `mvn spotless:check` to CI later so that a formatting violation fails the build immediately.
