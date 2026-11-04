-- ============================================================
-- V16: 代码生成器
--
-- 两张元数据表：表配置 + 字段配置。
-- 导入一次即可复用，团队共享同一份配置。
-- 注意：本迁移只建配置表，不会接触任何业务表。
-- ============================================================

CREATE TABLE `gen_table`
(
    `table_id`       bigint       NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `table_name`     varchar(200) NOT NULL COMMENT '表名',
    `table_comment`  varchar(500)          DEFAULT NULL COMMENT '表描述',
    `class_name`     varchar(100) NOT NULL COMMENT '实体类名',
    `module_name`    varchar(50)  NOT NULL COMMENT '模块名，如 system',
    `business_name`  varchar(50)  NOT NULL COMMENT '业务名，如 notice',
    `function_name`  varchar(100) NOT NULL COMMENT '功能名，用于菜单与页面标题',
    `package_name`   varchar(200) NOT NULL DEFAULT 'com.wejava.system' COMMENT '包名',
    `author`         varchar(50)           DEFAULT NULL COMMENT '作者',
    `parent_menu_id` bigint                DEFAULT NULL COMMENT '挂到哪个菜单下',
    `create_time`    datetime(3)           DEFAULT NULL COMMENT '创建时间',
    `update_time`    datetime(3)           DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`table_id`),
    UNIQUE KEY `uk_table_name` (`table_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '代码生成-表配置';

CREATE TABLE `gen_table_column`
(
    `column_id`      bigint       NOT NULL AUTO_INCREMENT COMMENT '字段配置ID',
    `table_id`       bigint       NOT NULL COMMENT '所属表配置',
    `column_name`    varchar(200) NOT NULL COMMENT '列名',
    `column_type`    varchar(100) NOT NULL COMMENT '列类型',
    `column_comment` varchar(500)          DEFAULT NULL COMMENT '列描述',
    `java_type`      varchar(50)  NOT NULL COMMENT 'Java 类型',
    `java_field`     varchar(100) NOT NULL COMMENT 'Java 属性名',
    `is_pk`          tinyint      NOT NULL DEFAULT 0 COMMENT '是否主键',
    `is_increment`   tinyint      NOT NULL DEFAULT 0 COMMENT '是否自增',
    `is_required`    tinyint      NOT NULL DEFAULT 0 COMMENT '是否必填',
    `is_insert`      tinyint      NOT NULL DEFAULT 1 COMMENT '新增表单显示',
    `is_edit`        tinyint      NOT NULL DEFAULT 1 COMMENT '编辑表单显示',
    `is_list`        tinyint      NOT NULL DEFAULT 1 COMMENT '列表显示',
    `is_query`       tinyint      NOT NULL DEFAULT 0 COMMENT '作为查询条件',
    `query_type`     varchar(20)           DEFAULT 'EQ' COMMENT 'EQ / LIKE / BETWEEN',
    `html_type`      varchar(20)           DEFAULT 'input' COMMENT '表单控件',
    `dict_type`      varchar(100)          DEFAULT NULL COMMENT '字典编码',
    `sort`           int          NOT NULL DEFAULT 0 COMMENT '排序',
    PRIMARY KEY (`column_id`),
    KEY `idx_table_id` (`table_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '代码生成-字段配置';

-- ------------------------------------------------------------
-- 菜单：系统管理 → 代码生成（含 5 个按钮权限）
-- ------------------------------------------------------------
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`,
                        `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
                        `create_by`, `create_time`)
SELECT '代码生成', 1000, 99, 'gen', 'tool/gen/index', 0, 0, 'C', 0, 0, 'system:gen:list', 'Cpu', 'admin', NOW(3)
WHERE NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE perms = 'system:gen:list') t);

SET @gen_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `perms` = 'system:gen:list' LIMIT 1);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`,
                        `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
                        `create_by`, `create_time`)
SELECT '生成查询', @gen_menu_id, 1, '', '', 0, 0, 'F', 0, 0, 'system:gen:query', '', 'admin', NOW(3)
WHERE NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE perms = 'system:gen:query') t);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`,
                        `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
                        `create_by`, `create_time`)
SELECT '生成修改', @gen_menu_id, 2, '', '', 0, 0, 'F', 0, 0, 'system:gen:edit', '', 'admin', NOW(3)
WHERE NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE perms = 'system:gen:edit') t);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`,
                        `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
                        `create_by`, `create_time`)
SELECT '生成导入', @gen_menu_id, 3, '', '', 0, 0, 'F', 0, 0, 'system:gen:import', '', 'admin', NOW(3)
WHERE NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE perms = 'system:gen:import') t);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`,
                        `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
                        `create_by`, `create_time`)
SELECT '生成删除', @gen_menu_id, 4, '', '', 0, 0, 'F', 0, 0, 'system:gen:remove', '', 'admin', NOW(3)
WHERE NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE perms = 'system:gen:remove') t);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`,
                        `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
                        `create_by`, `create_time`)
SELECT '生成代码', @gen_menu_id, 5, '', '', 0, 0, 'F', 0, 0, 'system:gen:code', '', 'admin', NOW(3)
WHERE NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE perms = 'system:gen:code') t);

-- 超级管理员角色授权（role_id = 1）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, m.menu_id
FROM `sys_menu` m
WHERE m.`perms` LIKE 'system:gen:%';
