[中文](../01-数据库设计.md) | English

# 01 · Database Design

> MySQL 8.4 LTS / InnoDB / `utf8mb4` / `utf8mb4_0900_ai_ci` / time zone `Asia/Shanghai`
> All tables are managed by Flyway: `V1__init_schema.sql` creates the schema, `V2__init_data.sql` seeds the data,
> and `V3`–`V14` apply incremental changes (column widening, messaging, AI, foreign keys, extra menus).

---

## 1. Design Principles

| Item | Rule |
|---|---|
| Primary key | `bigint AUTO_INCREMENT` (MyBatis-Plus `AUTO`), never generated client-side; **serialized as String in the API** |
| Audit fields | Every table must contain `create_by / create_time / update_by / update_time`, filled in automatically by `MetaObjectHandler` |
| Logical deletion | Business tables contain `del_flag tinyint`, `0` exists / `1` deleted; `@TableLogic` |
| Status field | `status tinyint`, `0` normal / `1` disabled |
| Amounts | `decimal(18,2)`; `float/double` is forbidden |
| Time | `datetime(3)`, precise to the millisecond |
| Index naming | Regular index `idx_<column>`, unique index `uk_<column>` |
| Foreign keys | **Not used**; integrity is guaranteed by the application layer, which makes sharding and batch operations easier |
| Field comments | Every field must have a `COMMENT` |

### Conflict Between Logical Deletion and Unique Indexes

`sys_user.username` must be unique, but after a soft delete the record still occupies the unique key, so recreating a user with the same name causes a conflict.

**This project adopts a "rename on delete" strategy**: when a user is deleted, `username` is rewritten
to `original_username#del_{user_id}` at the same time. This way the unique index can stay in the simple form
`uk_username(username)`, and there is no need to put `del_flag` into a composite index (which would mean a
given username could be deleted only once).

```sql
-- Executed in the same transaction as the user deletion
UPDATE sys_user
   SET del_flag = 1,
       username = CONCAT(username, '#del_', user_id),
       update_by = #{operator}, update_time = NOW(3)
 WHERE user_id = #{userId} AND del_flag = 0;
```

The same rule applies to `sys_role.role_key`, `sys_dict_type.dict_type` and `sys_config.config_key`.

---

## 2. ER Relationships

```
                ┌──────────┐
                │ sys_dept │◄──────────────┐
                └────┬─────┘               │
                     │ dept_id             │ dept_id
                ┌────▼─────┐          ┌────┴───────┐
     ┌─────────►│ sys_user │          │ sys_role_  │
     │          └────┬─────┘          │   dept     │
     │               │                └────▲───────┘
     │   ┌───────────┴────────┐            │
     │   │                    │            │
┌────┴───────┐        ┌───────▼──────┐  ┌──┴───────┐
│sys_user_role│        │ sys_user_post│  │ sys_role │
└────▲───────┘        └───────▲──────┘  └──▲────┬──┘
     │                        │            │    │
     │                        │      ┌─────┴──┐ │
     │                   ┌────┴─────┐│sys_role│ │
     │                   │ sys_post ││ _menu  │ │
     │                   └──────────┘└───▲────┘ │
     │                                   │      │
     │                            ┌──────┴──────┴─┐
     └───────────────────────────►│   sys_menu    │
                                  └───────────────┘
```

---

## 3. Table DDL

### 3.0 Create Database

```sql
CREATE DATABASE IF NOT EXISTS `wejava`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `wejava`;

SET NAMES utf8mb4;
SET time_zone = '+08:00';
```

### 3.1 Department Table

```sql
CREATE TABLE `sys_dept` (
  `dept_id`     bigint       NOT NULL AUTO_INCREMENT COMMENT 'Department ID',
  `parent_id`   bigint       NOT NULL DEFAULT 0      COMMENT 'Parent department ID, 0 for top level',
  `ancestors`   varchar(500) NOT NULL DEFAULT ''     COMMENT 'Ancestor list, comma-separated, e.g. 0,100,101',
  `dept_name`   varchar(30)  NOT NULL                COMMENT 'Department name',
  `order_num`   int          NOT NULL DEFAULT 0      COMMENT 'Display order',
  `leader`      varchar(20)           DEFAULT NULL   COMMENT 'Leader',
  `phone`       varchar(11)           DEFAULT NULL   COMMENT 'Contact phone',
  `email`       varchar(50)           DEFAULT NULL   COMMENT 'Email',
  `status`      tinyint      NOT NULL DEFAULT 0      COMMENT 'Status: 0 normal, 1 disabled',
  `del_flag`    tinyint      NOT NULL DEFAULT 0      COMMENT 'Delete flag: 0 exists, 1 deleted',
  `create_by`   varchar(64)           DEFAULT NULL   COMMENT 'Created by',
  `create_time` datetime(3)           DEFAULT NULL   COMMENT 'Create time',
  `update_by`   varchar(64)           DEFAULT NULL   COMMENT 'Updated by',
  `update_time` datetime(3)           DEFAULT NULL   COMMENT 'Update time',
  PRIMARY KEY (`dept_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_ancestors` (`ancestors`(64))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Department table';
```

> `ancestors` is used for the "this department and below" data scope and matched with `FIND_IN_SET`; a prefix index on the first 64 characters is sufficient.

### 3.2 User Table

```sql
CREATE TABLE `sys_user` (
  `user_id`     bigint       NOT NULL AUTO_INCREMENT COMMENT 'User ID',
  `dept_id`     bigint                DEFAULT NULL   COMMENT 'Department ID',
  `username`    varchar(64)  NOT NULL                COMMENT 'Login account (appends #del_{user_id} on logical delete)',
  `nickname`    varchar(30)  NOT NULL                COMMENT 'User nickname',
  `user_type`   varchar(2)   NOT NULL DEFAULT '00'   COMMENT 'User type: 00 system user',
  `email`       varchar(50)           DEFAULT NULL   COMMENT 'Email',
  `phone`       varchar(11)           DEFAULT NULL   COMMENT 'Mobile phone',
  `sex`         char(1)      NOT NULL DEFAULT '2'    COMMENT 'Gender: 0 male, 1 female, 2 unknown',
  `avatar`      varchar(255)          DEFAULT NULL   COMMENT 'Avatar URL',
  `password`    varchar(100) NOT NULL                COMMENT 'Password, prefixed with {bcrypt}',
  `status`      tinyint      NOT NULL DEFAULT 0      COMMENT 'Status: 0 normal, 1 disabled',
  `del_flag`    tinyint      NOT NULL DEFAULT 0      COMMENT 'Delete flag: 0 exists, 1 deleted',
  `login_ip`    varchar(64)           DEFAULT NULL   COMMENT 'Last login IP',
  `login_date`  datetime(3)           DEFAULT NULL   COMMENT 'Last login time',
  `pwd_update_time` datetime(3)       DEFAULT NULL   COMMENT 'Last password change time, used for forced logout',
  `remark`      varchar(500)          DEFAULT NULL   COMMENT 'Remark',
  `create_by`   varchar(64)           DEFAULT NULL,
  `create_time` datetime(3)           DEFAULT NULL,
  `update_by`   varchar(64)           DEFAULT NULL,
  `update_time` datetime(3)           DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='User table';
```

### 3.3 Role Table

```sql
CREATE TABLE `sys_role` (
  `role_id`            bigint       NOT NULL AUTO_INCREMENT COMMENT 'Role ID',
  `role_name`          varchar(30)  NOT NULL                COMMENT 'Role name',
  `role_key`           varchar(100) NOT NULL                COMMENT 'Role permission key',
  `role_sort`          int          NOT NULL DEFAULT 0      COMMENT 'Display order',
  `data_scope`         char(1)      NOT NULL DEFAULT '1'    COMMENT 'Data scope: 1 all, 2 custom, 3 own department, 4 own department and below, 5 own data only',
  `menu_check_strictly` tinyint  NOT NULL DEFAULT 1      COMMENT 'Whether parent/child selection is linked in the menu tree',
  `dept_check_strictly` tinyint  NOT NULL DEFAULT 1      COMMENT 'Whether parent/child selection is linked in the department tree',
  `status`             tinyint      NOT NULL DEFAULT 0      COMMENT 'Status: 0 normal, 1 disabled',
  `del_flag`           tinyint      NOT NULL DEFAULT 0      COMMENT 'Delete flag',
  `remark`             varchar(500)          DEFAULT NULL,
  `create_by`          varchar(64)           DEFAULT NULL,
  `create_time`        datetime(3)           DEFAULT NULL,
  `update_by`          varchar(64)           DEFAULT NULL,
  `update_time`        datetime(3)           DEFAULT NULL,
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Role table';
```

### 3.4 Menu Table

```sql
CREATE TABLE `sys_menu` (
  `menu_id`     bigint       NOT NULL AUTO_INCREMENT COMMENT 'Menu ID',
  `menu_name`   varchar(50)  NOT NULL                COMMENT 'Menu name',
  `parent_id`   bigint       NOT NULL DEFAULT 0      COMMENT 'Parent menu ID, 0 for top level',
  `order_num`   int          NOT NULL DEFAULT 0      COMMENT 'Display order',
  `path`        varchar(200)          DEFAULT NULL   COMMENT 'Route path',
  `component`   varchar(255)          DEFAULT NULL   COMMENT 'Component path, Layout for directories',
  `query`       varchar(255)          DEFAULT NULL   COMMENT 'Route parameters',
  `is_frame`    tinyint   NOT NULL DEFAULT 0      COMMENT 'External link: 0 no, 1 yes',
  `is_cache`    tinyint   NOT NULL DEFAULT 0      COMMENT 'Cache: 0 no cache, 1 cache',
  `menu_type`   char(1)      NOT NULL                COMMENT 'Type: M directory, C menu, F button',
  `visible`     tinyint   NOT NULL DEFAULT 0      COMMENT 'Visible: 0 shown, 1 hidden',
  `status`      tinyint      NOT NULL DEFAULT 0      COMMENT 'Status: 0 normal, 1 disabled',
  `perms`       varchar(100)          DEFAULT NULL   COMMENT 'Permission string, e.g. system:user:add',
  `icon`        varchar(100)          DEFAULT NULL   COMMENT 'Menu icon',
  `create_by`   varchar(64)           DEFAULT NULL,
  `create_time` datetime(3)           DEFAULT NULL,
  `update_by`   varchar(64)           DEFAULT NULL,
  `update_time` datetime(3)           DEFAULT NULL,
  PRIMARY KEY (`menu_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_perms` (`perms`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Menu permission table';
```

**`menu_type` semantics (important)**:

| Value | Meaning | Frontend behavior | Generates a route |
|---|---|---|---|
| `M` | Directory | First-level sidebar group, collapsible | Yes (`Layout` container) |
| `C` | Menu | A concrete page | Yes (leaf route) |
| `F` | Button | Not displayed, used only for permission checks | No |

### 3.5 Association Tables

```sql
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT 'User ID',
  `role_id` bigint NOT NULL COMMENT 'Role ID',
  PRIMARY KEY (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='User-role association table';

CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT 'Role ID',
  `menu_id` bigint NOT NULL COMMENT 'Menu ID',
  PRIMARY KEY (`role_id`, `menu_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Role-menu association table';

CREATE TABLE `sys_role_dept` (
  `role_id` bigint NOT NULL COMMENT 'Role ID',
  `dept_id` bigint NOT NULL COMMENT 'Department ID',
  PRIMARY KEY (`role_id`, `dept_id`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Role-department association table (custom data scope)';
```

### 3.6 Post Table

```sql
CREATE TABLE `sys_post` (
  `post_id`     bigint       NOT NULL AUTO_INCREMENT COMMENT 'Post ID',
  `post_code`   varchar(64)  NOT NULL                COMMENT 'Post code',
  `post_name`   varchar(50)  NOT NULL                COMMENT 'Post name',
  `post_sort`   int          NOT NULL DEFAULT 0      COMMENT 'Display order',
  `status`      tinyint      NOT NULL DEFAULT 0      COMMENT 'Status: 0 normal, 1 disabled',
  `remark`      varchar(500)          DEFAULT NULL,
  `create_by`   varchar(64)           DEFAULT NULL,
  `create_time` datetime(3)           DEFAULT NULL,
  `update_by`   varchar(64)           DEFAULT NULL,
  `update_time` datetime(3)           DEFAULT NULL,
  PRIMARY KEY (`post_id`),
  UNIQUE KEY `uk_post_code` (`post_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Post table';

CREATE TABLE `sys_user_post` (
  `user_id` bigint NOT NULL COMMENT 'User ID',
  `post_id` bigint NOT NULL COMMENT 'Post ID',
  PRIMARY KEY (`user_id`, `post_id`),
  KEY `idx_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='User-post association table';
```

### 3.7 Dictionary Tables

```sql
CREATE TABLE `sys_dict_type` (
  `dict_id`     bigint      NOT NULL AUTO_INCREMENT COMMENT 'Dictionary primary key',
  `dict_name`   varchar(100) NOT NULL               COMMENT 'Dictionary name',
  `dict_type`   varchar(100) NOT NULL               COMMENT 'Dictionary type, unique identifier',
  `status`      tinyint     NOT NULL DEFAULT 0      COMMENT 'Status: 0 normal, 1 disabled',
  `remark`      varchar(500)         DEFAULT NULL,
  `create_by`   varchar(64)          DEFAULT NULL,
  `create_time` datetime(3)          DEFAULT NULL,
  `update_by`   varchar(64)          DEFAULT NULL,
  `update_time` datetime(3)          DEFAULT NULL,
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Dictionary type table';

CREATE TABLE `sys_dict_data` (
  `dict_code`   bigint       NOT NULL AUTO_INCREMENT COMMENT 'Dictionary code',
  `dict_sort`   int          NOT NULL DEFAULT 0      COMMENT 'Sort order',
  `dict_label`  varchar(100) NOT NULL                COMMENT 'Dictionary label (displayed)',
  `dict_value`  varchar(100) NOT NULL                COMMENT 'Dictionary value (stored)',
  `dict_type`   varchar(100) NOT NULL                COMMENT 'Dictionary type',
  `css_class`   varchar(100)          DEFAULT NULL   COMMENT 'CSS class',
  `list_class`  varchar(100)          DEFAULT NULL   COMMENT 'Table display class',
  `is_default`  tinyint   NOT NULL DEFAULT 0      COMMENT 'Default: 0 no, 1 yes',
  `status`      tinyint      NOT NULL DEFAULT 0      COMMENT 'Status: 0 normal, 1 disabled',
  `remark`      varchar(500)          DEFAULT NULL,
  `create_by`   varchar(64)           DEFAULT NULL,
  `create_time` datetime(3)           DEFAULT NULL,
  `update_by`   varchar(64)           DEFAULT NULL,
  `update_time` datetime(3)           DEFAULT NULL,
  PRIMARY KEY (`dict_code`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Dictionary data table';
```

### 3.8 Parameter Configuration Table

```sql
CREATE TABLE `sys_config` (
  `config_id`    bigint       NOT NULL AUTO_INCREMENT COMMENT 'Parameter primary key',
  `config_name`  varchar(100) NOT NULL                COMMENT 'Parameter name',
  `config_key`   varchar(100) NOT NULL                COMMENT 'Parameter key',
  `config_value` varchar(500) NOT NULL                COMMENT 'Parameter value',
  `config_type`  tinyint   NOT NULL DEFAULT 0      COMMENT 'Built-in: 0 no, 1 yes (built-in entries cannot be deleted)',
  `remark`       varchar(500)          DEFAULT NULL,
  `create_by`    varchar(64)           DEFAULT NULL,
  `create_time`  datetime(3)           DEFAULT NULL,
  `update_by`    varchar(64)           DEFAULT NULL,
  `update_time`  datetime(3)           DEFAULT NULL,
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Parameter configuration table';
```

### 3.9 Notice and Announcement Table

```sql
CREATE TABLE `sys_notice` (
  `notice_id`      bigint       NOT NULL AUTO_INCREMENT COMMENT 'Notice ID',
  `notice_title`   varchar(50)  NOT NULL                COMMENT 'Notice title',
  `notice_type`    tinyint      NOT NULL DEFAULT 1      COMMENT 'Type: 1 notification, 2 announcement',
  `notice_content` longtext    NULL                  COMMENT 'Notice content (rich text)',
  `status`         tinyint      NOT NULL DEFAULT 0      COMMENT 'Status: 0 normal, 1 closed',
  `create_by`      varchar(64)           DEFAULT NULL,
  `create_time`    datetime(3)           DEFAULT NULL,
  `update_by`      varchar(64)           DEFAULT NULL,
  `update_time`    datetime(3)           DEFAULT NULL,
  PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Notice and announcement table';
```

### 3.10 Login Log

```sql
CREATE TABLE `sys_login_log` (
  `info_id`        bigint       NOT NULL AUTO_INCREMENT COMMENT 'Access ID',
  `username`       varchar(50)           DEFAULT NULL   COMMENT 'Login account',
  `ipaddr`         varchar(64)           DEFAULT NULL   COMMENT 'Login IP',
  `login_location` varchar(255)          DEFAULT NULL   COMMENT 'Login location',
  `browser`        varchar(50)           DEFAULT NULL   COMMENT 'Browser',
  `os`             varchar(50)           DEFAULT NULL   COMMENT 'Operating system',
  `status`         tinyint      NOT NULL DEFAULT 0      COMMENT 'Status: 0 success, 1 failure',
  `msg`            varchar(255)          DEFAULT NULL   COMMENT 'Message',
  `login_time`     datetime(3)           DEFAULT NULL   COMMENT 'Login time',
  PRIMARY KEY (`info_id`),
  KEY `idx_login_time` (`login_time`),
  KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Login log table';
```

### 3.11 Operation Log

```sql
CREATE TABLE `sys_oper_log` (
  `oper_id`       bigint       NOT NULL AUTO_INCREMENT COMMENT 'Log primary key',
  `title`         varchar(50)           DEFAULT NULL   COMMENT 'Module title',
  `business_type` tinyint      NOT NULL DEFAULT 0      COMMENT 'Business type: 0 other, 1 insert, 2 update, 3 delete, 4 export, 5 import',
  `method`        varchar(200)          DEFAULT NULL   COMMENT 'Method name',
  `request_method` varchar(10)          DEFAULT NULL   COMMENT 'HTTP method',
  `operator_type` tinyint      NOT NULL DEFAULT 0      COMMENT 'Operator type: 0 other, 1 backend user',
  `oper_name`     varchar(50)           DEFAULT NULL   COMMENT 'Operator',
  `dept_name`     varchar(50)           DEFAULT NULL   COMMENT 'Department name',
  `oper_url`      varchar(255)          DEFAULT NULL   COMMENT 'Request URL',
  `oper_ip`       varchar(64)           DEFAULT NULL   COMMENT 'Host address',
  `oper_location` varchar(255)          DEFAULT NULL   COMMENT 'Operation location',
  `oper_param`    varchar(2000)         DEFAULT NULL   COMMENT 'Request parameters (masked)',
  `json_result`   varchar(2000)         DEFAULT NULL   COMMENT 'Response result',
  `status`        tinyint      NOT NULL DEFAULT 0      COMMENT 'Status: 0 success, 1 failure',
  `error_msg`     varchar(2000)         DEFAULT NULL   COMMENT 'Error message',
  `cost_time`     bigint       NOT NULL DEFAULT 0      COMMENT 'Elapsed time (ms)',
  `oper_time`     datetime(3)           DEFAULT NULL   COMMENT 'Operation time',
  PRIMARY KEY (`oper_id`),
  KEY `idx_oper_time` (`oper_time`),
  KEY `idx_title` (`title`),
  KEY `idx_business_type` (`business_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Operation log table';
```

> The operation log table grows the fastest. Recommended: partition it by month, or archive rows periodically to `sys_oper_log_history`.

---

### Site Messages (added in V6)

Publishing a notice delivers it to user inboxes. `sys_message` holds the message body,
`sys_message_user` holds one row per recipient with its read state.

```sql
CREATE TABLE `sys_message` (
  `message_id`  bigint       NOT NULL AUTO_INCREMENT COMMENT 'message id',
  `title`       varchar(100) NOT NULL COMMENT 'title',
  `content`     text                  DEFAULT NULL COMMENT 'body',
  `msg_type`    tinyint      NOT NULL DEFAULT 1 COMMENT 'type: 1 notice, 2 system, 3 personal',
  `level`       tinyint      NOT NULL DEFAULT 0 COMMENT 'level: 0 normal, 1 important, 2 urgent',
  `link`        varchar(200)          DEFAULT NULL COMMENT 'frontend route opened on click',
  `source_id`   bigint                DEFAULT NULL COMMENT 'source business id (e.g. notice id)',
  `create_by`   varchar(64)           DEFAULT NULL,
  `create_time` datetime(3)           DEFAULT NULL,
  PRIMARY KEY (`message_id`),
  KEY `idx_message_source` (`source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='site messages';

CREATE TABLE `sys_message_user` (
  `id`          bigint      NOT NULL AUTO_INCREMENT,
  `message_id`  bigint      NOT NULL COMMENT 'message id',
  `user_id`     bigint      NOT NULL COMMENT 'recipient',
  `read_flag`   tinyint     NOT NULL DEFAULT 0 COMMENT '0 unread, 1 read',
  `read_time`   datetime(3)          DEFAULT NULL,
  `create_time` datetime(3)          DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_user` (`message_id`, `user_id`),
  KEY `idx_user_unread` (`user_id`, `read_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='message recipients';
```

**Why two tables**: the body is stored once while each recipient keeps its own read state.
Editing a notice updates what every recipient sees, because the body is never copied into inboxes.

---

### AI Assistant (added in V7 / V10)

`sys_ai_session` and `sys_ai_message` hold conversation history; `sys_ai_config` holds model
parameters plus the encrypted API key.

Configuration lives in its own table (V10) because a key must not sit next to ordinary
settings — anyone with `system:config:list` can read `sys_config` in plain text, while AI
configuration is admin-only.

```sql
CREATE TABLE `sys_ai_session` (
  `session_id`  bigint       NOT NULL AUTO_INCREMENT COMMENT 'session id',
  `user_id`     bigint       NOT NULL COMMENT 'owner',
  `title`       varchar(100)          DEFAULT NULL COMMENT 'title (first question)',
  `model`       varchar(64)           DEFAULT NULL COMMENT 'model name',
  `create_time` datetime(3)           DEFAULT NULL,
  `update_time` datetime(3)           DEFAULT NULL,
  PRIMARY KEY (`session_id`),
  KEY `idx_ai_session_user` (`user_id`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI session';

CREATE TABLE `sys_ai_message` (
  `message_id`        bigint      NOT NULL AUTO_INCREMENT,
  `session_id`        bigint      NOT NULL COMMENT 'session id',
  `role`              varchar(16) NOT NULL COMMENT 'user / assistant',
  `content`           mediumtext           DEFAULT NULL COMMENT 'message body',
  `prompt_tokens`     int                  DEFAULT NULL COMMENT 'input tokens',
  `completion_tokens` int                  DEFAULT NULL COMMENT 'output tokens',
  `duration_ms`       bigint               DEFAULT NULL COMMENT 'latency',
  `create_time`       datetime(3)          DEFAULT NULL,
  PRIMARY KEY (`message_id`),
  KEY `idx_ai_message_session` (`session_id`, `message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI message';

CREATE TABLE `sys_ai_config` (
  `ai_config_id` bigint       NOT NULL AUTO_INCREMENT,
  `provider`     varchar(32)  NOT NULL DEFAULT 'deepseek' COMMENT 'provider',
  `model`        varchar(64)  NOT NULL COMMENT 'model name',
  `base_url`     varchar(200) NOT NULL COMMENT 'endpoint',
  `api_key`      text                  DEFAULT NULL COMMENT 'key (AES-GCM encrypted)',
  `temperature`  decimal(3, 2) NOT NULL DEFAULT 0.70 COMMENT 'temperature',
  `max_tokens`   int          NOT NULL DEFAULT 4096 COMMENT 'max output tokens',
  `enabled`      tinyint      NOT NULL DEFAULT 1 COMMENT 'enabled',
  `update_by`    varchar(64)           DEFAULT NULL,
  `update_time`  datetime(3)           DEFAULT NULL,
  PRIMARY KEY (`ai_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI config';
```

**Why the key is encrypted**: `api_key` is encrypted with AES-GCM using `AI_CONFIG_SECRET`;
a dumped table yields no plaintext, and the API only returns a mask
(first 5 characters, `****`, last 4 characters).

---

## 4. Initial Data

```sql
-- ============ Departments ============
INSERT INTO `sys_dept` (`dept_id`,`parent_id`,`ancestors`,`dept_name`,`order_num`,`leader`,`phone`,`status`,`create_by`,`create_time`)
VALUES
 (100, 0,   '0',       'WeJava 科技',   0, 'admin', '13800000000', 0, 'admin', NOW(3)),
 (101, 100, '0,100',   '研发部',         1, 'admin', '13800000001', 0, 'admin', NOW(3)),
 (102, 100, '0,100',   '市场部',         2, 'admin', '13800000002', 0, 'admin', NOW(3));

-- ============ Users ============
-- Plaintext password is admin123, stored after BCrypt hashing; a password change is forced on first login
INSERT INTO `sys_user`
 (`user_id`,`dept_id`,`username`,`nickname`,`email`,`phone`,`sex`,`password`,`status`,`pwd_update_time`,`create_by`,`create_time`,`remark`)
VALUES
 (1, 100, 'admin', '超级管理员', 'admin@wejava.dev', '13800000000', '0',
  '{bcrypt}$2a$10$WAl91V47Ss5qvFt2hRcDEel7.HWzHM7/0m6ylbRoNNJWffaYgfwOq',
  0, NOW(3), 'admin', NOW(3), '系统内置超级管理员，不可删除');

-- ============ Roles ============
INSERT INTO `sys_role`
 (`role_id`,`role_name`,`role_key`,`role_sort`,`data_scope`,`status`,`create_by`,`create_time`,`remark`)
VALUES
 (1, '超级管理员', 'admin',  1, '1', 0, 'admin', NOW(3), '拥有全部权限'),
 (2, '普通角色',   'common', 2, '5', 0, 'admin', NOW(3), '仅本人数据');

INSERT INTO `sys_user_role` (`user_id`,`role_id`) VALUES (1, 1);

-- ============ Posts ============
INSERT INTO `sys_post` (`post_id`,`post_code`,`post_name`,`post_sort`,`status`,`create_by`,`create_time`)
VALUES (1,'ceo','董事长',1,0,'admin',NOW(3)),
       (2,'se','项目经理',2,0,'admin',NOW(3)),
       (3,'hr','人力资源',3,0,'admin',NOW(3));

-- ============ Dictionaries ============
INSERT INTO `sys_dict_type` (`dict_id`,`dict_name`,`dict_type`,`status`,`create_by`,`create_time`)
VALUES (1,'用户性别','sys_user_sex',0,'admin',NOW(3)),
       (2,'系统开关','sys_normal_disable',0,'admin',NOW(3)),
       (3,'通知类型','sys_notice_type',0,'admin',NOW(3)),
       (4,'操作类型','sys_oper_type',0,'admin',NOW(3));

INSERT INTO `sys_dict_data` (`dict_sort`,`dict_label`,`dict_value`,`dict_type`,`list_class`,`is_default`,`status`,`create_by`,`create_time`)
VALUES
 (1,'男','0','sys_user_sex','',1,0,'admin',NOW(3)),
 (2,'女','1','sys_user_sex','',0,0,'admin',NOW(3)),
 (3,'未知','2','sys_user_sex','',0,0,'admin',NOW(3)),
 (1,'正常','0','sys_normal_disable','primary',1,0,'admin',NOW(3)),
 (2,'停用','1','sys_normal_disable','danger',0,0,'admin',NOW(3)),
 (1,'通知','1','sys_notice_type','warning',1,0,'admin',NOW(3)),
 (2,'公告','2','sys_notice_type','success',0,0,'admin',NOW(3)),
 (1,'新增','1','sys_oper_type','info',0,0,'admin',NOW(3)),
 (2,'修改','2','sys_oper_type','info',0,0,'admin',NOW(3)),
 (3,'删除','3','sys_oper_type','danger',0,0,'admin',NOW(3));

-- ============ Parameters ============
INSERT INTO `sys_config` (`config_name`,`config_key`,`config_value`,`config_type`,`create_by`,`create_time`,`remark`)
VALUES
 ('账号自助-是否开启注册','sys.account.registerUser','false',1,'admin',NOW(3),'是否开启用户自助注册'),
 ('用户管理-初始密码','sys.user.initPassword','123456',1,'admin',NOW(3),'新建用户的默认密码'),
 ('登录-失败锁定次数','sys.login.maxRetry','5',1,'admin',NOW(3),'连续失败几次锁定'),
 ('登录-锁定时长(分钟)','sys.login.lockMinutes','15',1,'admin',NOW(3),'锁定时长'),
 ('上传-单文件大小(MB)','sys.upload.maxSize','10',1,'admin',NOW(3),'单文件大小上限'),
 ('接口-同一用户最大并发数','sys.request.maxConcurrency','10',1,'admin',NOW(3),'同一 IP + 同一用户同时处理的请求数上限，0 表示不限制');
```

### Menu Initialization (excerpt, covering System Management + System Monitoring)

```sql
INSERT INTO `sys_menu`
 (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`)
VALUES
-- ===== System Management =====
(1000,'系统管理',0,1,'/system','Layout','M',0,0,NULL,'setting','admin',NOW(3)),

(1001,'用户管理',1000,1,'user','system/user/index','C',0,0,'system:user:list','user','admin',NOW(3)),
(10011,'用户查询',1001,1,'','','F',0,0,'system:user:query','','admin',NOW(3)),
(10012,'用户新增',1001,2,'','','F',0,0,'system:user:add','','admin',NOW(3)),
(10013,'用户修改',1001,3,'','','F',0,0,'system:user:edit','','admin',NOW(3)),
(10014,'用户删除',1001,4,'','','F',0,0,'system:user:remove','','admin',NOW(3)),
(10015,'重置密码',1001,5,'','','F',0,0,'system:user:resetPwd','','admin',NOW(3)),
(10016,'用户导出',1001,6,'','','F',0,0,'system:user:export','','admin',NOW(3)),
(10017,'用户导入',1001,7,'','','F',0,0,'system:user:import','','admin',NOW(3)),

(1002,'角色管理',1000,2,'role','system/role/index','C',0,0,'system:role:list','peoples','admin',NOW(3)),
(10021,'角色查询',1002,1,'','','F',0,0,'system:role:query','','admin',NOW(3)),
(10022,'角色新增',1002,2,'','','F',0,0,'system:role:add','','admin',NOW(3)),
(10023,'角色修改',1002,3,'','','F',0,0,'system:role:edit','','admin',NOW(3)),
(10024,'角色删除',1002,4,'','','F',0,0,'system:role:remove','','admin',NOW(3)),
(10025,'角色导出',1002,5,'','','F',0,0,'system:role:export','','admin',NOW(3)),
(10026,'分配数据权限',1002,6,'','','F',0,0,'system:role:dataScope','','admin',NOW(3)),

(1003,'菜单管理',1000,3,'menu','system/menu/index','C',0,0,'system:menu:list','tree-table','admin',NOW(3)),
(10031,'菜单查询',1003,1,'','','F',0,0,'system:menu:query','','admin',NOW(3)),
(10032,'菜单新增',1003,2,'','','F',0,0,'system:menu:add','','admin',NOW(3)),
(10033,'菜单修改',1003,3,'','','F',0,0,'system:menu:edit','','admin',NOW(3)),
(10034,'菜单删除',1003,4,'','','F',0,0,'system:menu:remove','','admin',NOW(3)),

(1004,'部门管理',1000,4,'dept','system/dept/index','C',0,0,'system:dept:list','tree','admin',NOW(3)),
(10041,'部门查询',1004,1,'','','F',0,0,'system:dept:query','','admin',NOW(3)),
(10042,'部门新增',1004,2,'','','F',0,0,'system:dept:add','','admin',NOW(3)),
(10043,'部门修改',1004,3,'','','F',0,0,'system:dept:edit','','admin',NOW(3)),
(10044,'部门删除',1004,4,'','','F',0,0,'system:dept:remove','','admin',NOW(3)),

(1005,'岗位管理',1000,5,'post','system/post/index','C',0,0,'system:post:list','post','admin',NOW(3)),
(10051,'岗位查询',1005,1,'','','F',0,0,'system:post:query','','admin',NOW(3)),
(10052,'岗位新增',1005,2,'','','F',0,0,'system:post:add','','admin',NOW(3)),
(10053,'岗位修改',1005,3,'','','F',0,0,'system:post:edit','','admin',NOW(3)),
(10054,'岗位删除',1005,4,'','','F',0,0,'system:post:remove','','admin',NOW(3)),
(10055,'岗位导出',1005,5,'','','F',0,0,'system:post:export','','admin',NOW(3)),

(1006,'字典管理',1000,6,'dict','system/dict/index','C',0,0,'system:dict:list','dict','admin',NOW(3)),
(10061,'字典查询',1006,1,'','','F',0,0,'system:dict:query','','admin',NOW(3)),
(10062,'字典新增',1006,2,'','','F',0,0,'system:dict:add','','admin',NOW(3)),
(10063,'字典修改',1006,3,'','','F',0,0,'system:dict:edit','','admin',NOW(3)),
(10064,'字典删除',1006,4,'','','F',0,0,'system:dict:remove','','admin',NOW(3)),
(10065,'字典导出',1006,5,'','','F',0,0,'system:dict:export','','admin',NOW(3)),

(1007,'参数设置',1000,7,'config','system/config/index','C',0,0,'system:config:list','edit','admin',NOW(3)),
(10071,'参数查询',1007,1,'','','F',0,0,'system:config:query','','admin',NOW(3)),
(10072,'参数新增',1007,2,'','','F',0,0,'system:config:add','','admin',NOW(3)),
(10073,'参数修改',1007,3,'','','F',0,0,'system:config:edit','','admin',NOW(3)),
(10074,'参数删除',1007,4,'','','F',0,0,'system:config:remove','','admin',NOW(3)),

(1008,'通知公告',1000,8,'notice','system/notice/index','C',0,0,'system:notice:list','message','admin',NOW(3)),
(10081,'公告查询',1008,1,'','','F',0,0,'system:notice:query','','admin',NOW(3)),
(10082,'公告新增',1008,2,'','','F',0,0,'system:notice:add','','admin',NOW(3)),
(10083,'公告修改',1008,3,'','','F',0,0,'system:notice:edit','','admin',NOW(3)),
(10084,'公告删除',1008,4,'','','F',0,0,'system:notice:remove','','admin',NOW(3)),

-- ===== System Monitoring =====
(1010,'系统监控',0,2,'/monitor','Layout','M',0,0,NULL,'monitor','admin',NOW(3)),
(1011,'在线用户',1010,1,'online','monitor/online/index','C',0,0,'monitor:online:list','online','admin',NOW(3)),
(10111,'强制下线',1011,1,'','','F',0,0,'monitor:online:forceLogout','','admin',NOW(3)),
(1012,'操作日志',1010,2,'operlog','monitor/operlog/index','C',0,0,'monitor:operlog:list','form','admin',NOW(3)),
(10121,'日志查询',1012,1,'','','F',0,0,'monitor:operlog:query','','admin',NOW(3)),
(10122,'日志删除',1012,2,'','','F',0,0,'monitor:operlog:remove','','admin',NOW(3)),
(10123,'日志导出',1012,3,'','','F',0,0,'monitor:operlog:export','','admin',NOW(3)),
(1013,'登录日志',1010,3,'loginlog','monitor/loginlog/index','C',0,0,'monitor:loginlog:list','logininfor','admin',NOW(3)),
(10131,'登录查询',1013,1,'','','F',0,0,'monitor:loginlog:query','','admin',NOW(3)),
(10132,'登录删除',1013,2,'','','F',0,0,'monitor:loginlog:remove','','admin',NOW(3)),
(10133,'登录导出',1013,3,'','','F',0,0,'monitor:loginlog:export','','admin',NOW(3)),
(10134,'账户解锁',1013,4,'','','F',0,0,'monitor:loginlog:unlock','','admin',NOW(3)),
(1014,'服务监控',1010,4,'server','monitor/server/index','C',0,0,'monitor:server:list','server','admin',NOW(3));

-- Associate all menus with the super administrator
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`) SELECT 1, `menu_id` FROM `sys_menu`;
```

---

## 5. Data Scope Field

`sys_role.data_scope` determines which rows of data a role can see:

| Value | Meaning | Generated condition (using `sys_user` as an example) |
|---|---|---|
| `1` | All data | No condition is appended |
| `2` | Custom departments | `dept_id IN (SELECT dept_id FROM sys_role_dept WHERE role_id IN (...))` |
| `3` | Own department | `dept_id = #{user.deptId}` |
| `4` | Own department and below | `dept_id IN (SELECT dept_id FROM sys_dept WHERE dept_id = #{deptId} OR FIND_IN_SET(#{deptId}, ancestors))` |
| `5` | Own data only | `user_id = #{userId}` (user table) or `create_by = #{username}` (other tables) |

**Multi-role merge rule**: take the **most permissive** scope among all roles (`1` > `2` > `4` > `3` > `5`).

**Note**: the automatically appended conditions only take effect for SQL generated by MyBatis-Plus; SQL written by hand in XML must explicitly call
`DataScopeHelper.apply(...)`. See [02-Authentication and Permissions](02-auth-and-permissions.md#7-data-permission).

---

## 6. Common Query Examples

```sql
-- Query the set of permission strings for a user (executed once at login, result cached in Redis)
SELECT DISTINCT m.perms
  FROM sys_menu m
  JOIN sys_role_menu rm ON rm.menu_id = m.menu_id
  JOIN sys_user_role ur ON ur.role_id = rm.role_id
 WHERE ur.user_id = ?
   AND m.status = 0
   AND m.perms IS NOT NULL
   AND m.perms <> '';

-- Query the role keys of a user (used by the frontend for checks)
SELECT r.role_key
  FROM sys_role r
  JOIN sys_user_role ur ON ur.role_id = r.role_id
 WHERE ur.user_id = ? AND r.status = 0 AND r.del_flag = 0;

-- Query the menu tree (used for dynamic routing, only M and C are selected)
SELECT *
  FROM sys_menu
 WHERE menu_type IN ('M','C') AND status = 0
   AND menu_id IN (
       SELECT rm.menu_id FROM sys_role_menu rm WHERE rm.role_id IN (...)
   )
 ORDER BY parent_id, order_num;
```

---

## 7. Flyway Conventions

| Rule | Description |
|---|---|
| Naming | `V{sequence}__{description}.sql`, with consecutive sequence numbers |
| Append only | Released scripts **must not be modified**; change the schema with a new script |
| No clean | `spring.flyway.clean-disabled=true` in production |
| Production strategy | Only `migrate` is executed; `baseline` must not overwrite an existing database |
| Large-table changes | Add indexes with `ALGORITHM=INPLACE, LOCK=NONE` (automatic in most cases on MySQL 8) |
| Script content | One script does one thing (create table / add index / alter column kept separate) |

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    clean-disabled: true
    validate-on-migrate: true
```
