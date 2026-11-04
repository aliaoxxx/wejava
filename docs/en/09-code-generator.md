[中文](../09-代码生成器设计.md) | English

# 09 · Code Generator Design

> Goal: read a database table → generate 9 backend files + frontend `api.ts` / `schema.ts` + menu SQL,
> turning "add a table" from **half a day of hand-writing** into **one minute**.
> Status: **P1 implemented** (V16 migration + import/config/preview/download + 12 templates + UI).

---

## 1. Why

The frontend is already configuration-driven (`schema.ts` → page), but the backend is still hand-written.
A complete CRUD module needs:

| Layer | File | Easy to get wrong |
|---|---|---|
| Entity | `SysXxx.java` | `@TableId` strategy, `@TableName`, `BaseEntity` |
| Mapper | `SysXxxMapper.java` | `BaseMapper` |
| Service / Impl | 2 | pagination, `LambdaQueryWrapper` assembly |
| Controller | 1 | `@PreAuthorize` permission, `@OperLog`, `@Valid` |
| VO / Form / Query | 3–4 | `@Schema`, pagination fields, validation annotations |
| Converter | 1 | MapStruct interface |
| Frontend | `api.ts` + `schema.ts` | request functions, field types, dictionaries |

Roughly **4–6 hours per table**, all of it repeating the same decisions. That is what the generator removes.

## 2. Output (12 files per generation)

```
wejava-backend/src/main/java/com/wejava/{module}/
├── model/entity/{Entity}.java
├── model/vo/{Entity}VO.java
├── model/form/{Entity}Form.java
├── model/query/{Entity}Query.java
├── mapper/{Entity}Mapper.java
├── service/{Entity}Service.java
├── service/impl/{Entity}ServiceImpl.java
├── converter/{Entity}Converter.java
└── controller/{Entity}Controller.java

wejava-frontend/src/
├── api/{module}/{entity}.ts
└── views/{module}/{entity}/schema.ts

sql/
└── menu_{table}.sql        (menu + 5 button permissions)
```

## 3. Metadata storage (V16, two new tables)

Import once, persist the configuration, share it across the team.

```sql
CREATE TABLE `gen_table`
(
    `table_id`      bigint NOT NULL AUTO_INCREMENT,
    `table_name`    varchar(200) NOT NULL,
    `table_comment` varchar(500) DEFAULT NULL,
    `class_name`    varchar(100) NOT NULL,
    `module_name`   varchar(50)  NOT NULL,
    `business_name` varchar(50)  NOT NULL,
    `function_name` varchar(100) NOT NULL,
    `package_name`  varchar(200) NOT NULL DEFAULT 'com.wejava.system',
    `author`        varchar(50)  DEFAULT NULL,
    `parent_menu_id` bigint      DEFAULT NULL,
    `create_time`   datetime(3)  DEFAULT NULL,
    `update_time`   datetime(3)  DEFAULT NULL,
    PRIMARY KEY (`table_id`),
    UNIQUE KEY `uk_table_name` (`table_name`)
) COMMENT 'code generation: table config';

CREATE TABLE `gen_table_column`
(
    `column_id`      bigint NOT NULL AUTO_INCREMENT,
    `table_id`       bigint NOT NULL,
    `column_name`    varchar(200) NOT NULL,
    `column_type`    varchar(100) NOT NULL,
    `column_comment` varchar(500) DEFAULT NULL,
    `java_type`      varchar(50)  NOT NULL,
    `java_field`     varchar(100) NOT NULL,
    `is_pk`          tinyint NOT NULL DEFAULT 0,
    `is_increment`   tinyint NOT NULL DEFAULT 0,
    `is_required`    tinyint NOT NULL DEFAULT 0,
    `is_insert`      tinyint NOT NULL DEFAULT 1,
    `is_edit`        tinyint NOT NULL DEFAULT 1,
    `is_list`        tinyint NOT NULL DEFAULT 1,
    `is_query`       tinyint NOT NULL DEFAULT 0,
    `query_type`     varchar(20) DEFAULT 'EQ',
    `html_type`      varchar(20) DEFAULT 'input',
    `dict_type`      varchar(100) DEFAULT NULL,
    `sort`           int NOT NULL DEFAULT 0,
    PRIMARY KEY (`column_id`),
    KEY `idx_table_id` (`table_id`)
) COMMENT 'code generation: column config';
```

## 4. Type mapping (inferred, overridable)

| MySQL | Java | TS | Control |
|---|---|---|---|
| `bigint` | `Long` | `number` | `number` |
| `int` / `tinyint` | `Integer` | `number` | `number`, or `switch` when 0/1 |
| `varchar(n)` | `String` | `string` | n ≤ 255 → `input`, else `textarea` |
| `text` / `longtext` | `String` | `string` | `textarea` |
| `datetime` / `timestamp` | `LocalDateTime` | `string` | `datetime` |
| `date` | `LocalDate` | `string` | `date` |
| `decimal` / `double` | `BigDecimal` | `number` | `number` |

**Column-name heuristics**

| Column | Inference |
|---|---|
| `xxx_id` as PK | primary key, visible in list, hidden in form |
| `status` | query condition + `select`, dict `sys_normal_disable` |
| `sort` / `order_num` | `number` |
| `remark` | `textarea`, hidden in list |
| `create_time` / `update_time` / `create_by` / `update_by` | list only, not in form (from `BaseEntity`) |

## 5. Naming rules

| Item | Rule | Example |
|---|---|---|
| Table | `{module}_{business}` | `system_notice` |
| Entity | camel case of full table name | `SystemNotice` |
| Variable | camel case of business name | `notice` |
| Permission | `{module}:{business}` | `system:notice` |
| Route | `/{module}/{business}` | `/system/notice` |
| Frontend dir | `views/{module}/{business}/` | `views/system/notice/` |

No forced `sys_` prefix: both `system_notice` and `notice` resolve correctly; the former is clearer.

## 6. API

| Method | Path | Permission | Purpose |
|---|---|---|---|
| GET | `/system/gen/table/list` | `system:gen:list` | imported tables |
| GET | `/system/gen/table/db/list` | `system:gen:list` | **not yet imported** tables (`information_schema`) |
| POST | `/system/gen/table/import` | `system:gen:import` | import a table, infer columns |
| GET | `/system/gen/table/{id}` | `system:gen:query` | table config with columns |
| PUT | `/system/gen/table/{id}` | `system:gen:edit` | save config |
| DELETE | `/system/gen/table/{id}` | `system:gen:remove` | delete config (never the business table) |
| GET | `/system/gen/preview/{id}` | `system:gen:preview` | `Map<filename, code>` |
| GET | `/system/gen/download/{id}` | `system:gen:code` | download zip |
| POST | `/system/gen/batch-download` | `system:gen:code` | batch download |

**Zip layout**

```
wejava-{business}.zip
├── backend/…   (9 java files, package-structured)
├── frontend/
│   ├── api/{module}/{business}.ts
│   └── views/{module}/{business}/schema.ts
└── sql/menu_{table}.sql
```

## 7. Template engine

**FreeMarker**, templates under `src/main/resources/template/gen/`:

```
entity.java.ftl          mapper.java.ftl         service.java.ftl
serviceImpl.java.ftl     controller.java.ftl     vo.java.ftl
form.java.ftl            query.java.ftl          converter.java.ftl
api.ts.ftl               schema.ts.ftl           menu.sql.ftl
```

Template files rather than string concatenation: they can be edited without touching Java code,
and users can adapt them to their own style.

## 8. UI

New menu "系统工具 → 代码生成". The page is hand-written (it is special, not schema-driven):

```
1. imported table list  → preview / edit / download / delete
2. "import table"       → dialog listing unimported DB tables with comments
3. "edit"               → table info + column config grid (roles, control type, dict)
4. "preview"            → file tree on the left, code on the right
```

## 9. Security constraints

| Risk | Mitigation |
|---|---|
| used by normal users | every endpoint `@PreAuthorize`, granted to super admin only |
| reading unintended tables | read `information_schema` only, exclude `gen_*` (and optionally `sys_*`) |
| writing files on the server | **not done** — zip download only, no path traversal, no overwrite |
| SQL injection in generated code | pure text templates, never splice user input into SQL |
| oversized download | max 20 tables per request |

## 10. Phases

| Phase | Content | Estimate |
|---|---|---|
| **P1** | V16 migration + import/config/preview/download + 12 templates + UI | 4 days |
| **P2** | batch generation, online template editing, auto-attach menu parent | 1.5 days |
| **P3** | master-detail, tree tables, import/export templates | 3 days |

## 11. Acceptance criteria

- [ ] Importing a new table (`demo_product`) infers types, required flags and query columns correctly
- [ ] Generated backend code compiles as-is and passes `spotless:check`
- [ ] Generated `schema.ts` passes `vue-tsc` and the page supports full CRUD
- [ ] Generated `menu.sql` yields menus and button permissions
- [ ] Preview output matches the downloaded zip
- [ ] Non-admin requests to generator endpoints return 403

## 13. Implementation notes (P1)

| Item | Result |
|---|---|
| Migration | `V16__code_generator.sql`: two config tables + menu with five button permissions |
| Templates | 12 `.ftl` files under `resources/template/gen/` |
| Output | **13 files** per generation (12 code/config + `README.txt`) |
| UI | 系统管理 → 代码生成: list / import / edit / preview / download / delete |

**A detail worth recording**: the templates must decide line wrapping according to palantir's
120-column rule; otherwise the generated code compiles but fails `spotless:check`. The model
estimates width as `83 + 3 × field name length + class name length` and only emits the wrapped
form above 120.

**Measured on a `demo_product` table**:

```
inference    long text → textarea, status → switch + dict, audit columns hidden in forms
backend      9 Java files compile on the first try, spotless:check passes unchanged
frontend     schema.ts passes vue-tsc, api.ts passes lint
menu SQL     menu + 5 button permissions, granted to role_id=1
download     zip 8.2 KB, layout matches the design
```

## 12. Explicit non-goals

- ❌ No runtime low-code (metadata-driven dynamic APIs) — that is a different product
- ❌ No writing to the server filesystem
- ❌ No Flyway migration generation (developers create business tables; the generator only writes code)
