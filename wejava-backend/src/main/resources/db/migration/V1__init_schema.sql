-- ============================================================
-- V1: 初始化表结构（15 张表）
-- 字符集 utf8mb4 / 排序规则 utf8mb4_0900_ai_ci
-- 说明：不使用外键，关联由应用层保证
-- ============================================================

-- ---------- 部门 ----------
CREATE TABLE `sys_dept`
(
    `dept_id`     bigint       NOT NULL AUTO_INCREMENT COMMENT '部门ID',
    `parent_id`   bigint       NOT NULL DEFAULT 0 COMMENT '父部门ID，0为顶级',
    `ancestors`   varchar(500) NOT NULL DEFAULT '' COMMENT '祖级列表，逗号分隔',
    `dept_name`   varchar(30)  NOT NULL COMMENT '部门名称',
    `order_num`   int          NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `leader`      varchar(20)           DEFAULT NULL COMMENT '负责人',
    `phone`       varchar(11)           DEFAULT NULL COMMENT '联系电话',
    `email`       varchar(50)           DEFAULT NULL COMMENT '邮箱',
    `status`      tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0正常 1停用',
    `del_flag`    tinyint      NOT NULL DEFAULT 0 COMMENT '删除标志：0存在 1删除',
    `create_by`   varchar(64)           DEFAULT NULL COMMENT '创建者',
    `create_time` datetime(3)           DEFAULT NULL COMMENT '创建时间',
    `update_by`   varchar(64)           DEFAULT NULL COMMENT '更新者',
    `update_time` datetime(3)           DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`dept_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_ancestors` (`ancestors`(64))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='部门表';

-- ---------- 用户 ----------
CREATE TABLE `sys_user`
(
    `user_id`         bigint       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `dept_id`         bigint                DEFAULT NULL COMMENT '部门ID',
    `username`        varchar(30)  NOT NULL COMMENT '登录账号',
    `nickname`        varchar(30)  NOT NULL COMMENT '用户昵称',
    `user_type`       varchar(2)   NOT NULL DEFAULT '00' COMMENT '用户类型：00系统用户',
    `email`           varchar(50)           DEFAULT NULL COMMENT '邮箱',
    `phone`           varchar(11)           DEFAULT NULL COMMENT '手机号',
    `sex`             char(1)      NOT NULL DEFAULT '2' COMMENT '性别：0男 1女 2未知',
    `avatar`          varchar(255)          DEFAULT NULL COMMENT '头像地址',
    `password`        varchar(100) NOT NULL COMMENT '密码，{bcrypt}开头',
    `status`          tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0正常 1停用',
    `del_flag`        tinyint      NOT NULL DEFAULT 0 COMMENT '删除标志：0存在 1删除',
    `login_ip`        varchar(64)           DEFAULT NULL COMMENT '最后登录IP',
    `login_date`      datetime(3)           DEFAULT NULL COMMENT '最后登录时间',
    `pwd_update_time` datetime(3)           DEFAULT NULL COMMENT '密码最后修改时间，用于强制下线',
    `remark`          varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_by`       varchar(64)           DEFAULT NULL COMMENT '创建者',
    `create_time`     datetime(3)           DEFAULT NULL COMMENT '创建时间',
    `update_by`       varchar(64)           DEFAULT NULL COMMENT '更新者',
    `update_time`     datetime(3)           DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户表';

-- ---------- 角色 ----------
CREATE TABLE `sys_role`
(
    `role_id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_name`           varchar(30)  NOT NULL COMMENT '角色名称',
    `role_key`            varchar(100) NOT NULL COMMENT '角色权限字符',
    `role_sort`           int          NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `data_scope`          char(1)      NOT NULL DEFAULT '1' COMMENT '数据范围：1全部 2自定义 3本部门 4本部门及以下 5仅本人',
    `menu_check_strictly` tinyint   NOT NULL DEFAULT 1 COMMENT '菜单树选择项是否父子联动',
    `dept_check_strictly` tinyint   NOT NULL DEFAULT 1 COMMENT '部门树选择项是否父子联动',
    `status`              tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0正常 1停用',
    `del_flag`            tinyint      NOT NULL DEFAULT 0 COMMENT '删除标志',
    `remark`              varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_by`           varchar(64)           DEFAULT NULL COMMENT '创建者',
    `create_time`         datetime(3)           DEFAULT NULL COMMENT '创建时间',
    `update_by`           varchar(64)           DEFAULT NULL COMMENT '更新者',
    `update_time`         datetime(3)           DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`role_id`),
    UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='角色表';

-- ---------- 菜单 ----------
CREATE TABLE `sys_menu`
(
    `menu_id`     bigint       NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `menu_name`   varchar(50)  NOT NULL COMMENT '菜单名称',
    `parent_id`   bigint       NOT NULL DEFAULT 0 COMMENT '父菜单ID，0为顶级',
    `order_num`   int          NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `path`        varchar(200)          DEFAULT NULL COMMENT '路由地址',
    `component`   varchar(255)          DEFAULT NULL COMMENT '组件路径，目录为 Layout',
    `query`       varchar(255)          DEFAULT NULL COMMENT '路由参数',
    `is_frame`    tinyint   NOT NULL DEFAULT 0 COMMENT '是否外链：0否 1是',
    `is_cache`    tinyint   NOT NULL DEFAULT 0 COMMENT '是否缓存：0不缓存 1缓存',
    `menu_type`   char(1)      NOT NULL COMMENT '类型：M目录 C菜单 F按钮',
    `visible`     tinyint   NOT NULL DEFAULT 0 COMMENT '是否显示：0显示 1隐藏',
    `status`      tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0正常 1停用',
    `perms`       varchar(100)          DEFAULT NULL COMMENT '权限字符',
    `icon`        varchar(100)          DEFAULT NULL COMMENT '菜单图标',
    `create_by`   varchar(64)           DEFAULT NULL COMMENT '创建者',
    `create_time` datetime(3)           DEFAULT NULL COMMENT '创建时间',
    `update_by`   varchar(64)           DEFAULT NULL COMMENT '更新者',
    `update_time` datetime(3)           DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`menu_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_perms` (`perms`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='菜单权限表';

-- ---------- 关联表 ----------
CREATE TABLE `sys_user_role`
(
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `role_id` bigint NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户角色关联表';

CREATE TABLE `sys_role_menu`
(
    `role_id` bigint NOT NULL COMMENT '角色ID',
    `menu_id` bigint NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (`role_id`, `menu_id`),
    KEY `idx_menu_id` (`menu_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='角色菜单关联表';

CREATE TABLE `sys_role_dept`
(
    `role_id` bigint NOT NULL COMMENT '角色ID',
    `dept_id` bigint NOT NULL COMMENT '部门ID',
    PRIMARY KEY (`role_id`, `dept_id`),
    KEY `idx_dept_id` (`dept_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='角色部门关联表（自定义数据权限）';

-- ---------- 岗位 ----------
CREATE TABLE `sys_post`
(
    `post_id`     bigint       NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
    `post_code`   varchar(64)  NOT NULL COMMENT '岗位编码',
    `post_name`   varchar(50)  NOT NULL COMMENT '岗位名称',
    `post_sort`   int          NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `status`      tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0正常 1停用',
    `remark`      varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_by`   varchar(64)           DEFAULT NULL COMMENT '创建者',
    `create_time` datetime(3)           DEFAULT NULL COMMENT '创建时间',
    `update_by`   varchar(64)           DEFAULT NULL COMMENT '更新者',
    `update_time` datetime(3)           DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`post_id`),
    UNIQUE KEY `uk_post_code` (`post_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='岗位表';

CREATE TABLE `sys_user_post`
(
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `post_id` bigint NOT NULL COMMENT '岗位ID',
    PRIMARY KEY (`user_id`, `post_id`),
    KEY `idx_post_id` (`post_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户岗位关联表';

-- ---------- 字典 ----------
CREATE TABLE `sys_dict_type`
(
    `dict_id`     bigint       NOT NULL AUTO_INCREMENT COMMENT '字典主键',
    `dict_name`   varchar(100) NOT NULL COMMENT '字典名称',
    `dict_type`   varchar(100) NOT NULL COMMENT '字典类型，唯一标识',
    `status`      tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0正常 1停用',
    `remark`      varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_by`   varchar(64)           DEFAULT NULL COMMENT '创建者',
    `create_time` datetime(3)           DEFAULT NULL COMMENT '创建时间',
    `update_by`   varchar(64)           DEFAULT NULL COMMENT '更新者',
    `update_time` datetime(3)           DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`dict_id`),
    UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='字典类型表';

CREATE TABLE `sys_dict_data`
(
    `dict_code`   bigint       NOT NULL AUTO_INCREMENT COMMENT '字典编码',
    `dict_sort`   int          NOT NULL DEFAULT 0 COMMENT '排序',
    `dict_label`  varchar(100) NOT NULL COMMENT '字典标签（展示）',
    `dict_value`  varchar(100) NOT NULL COMMENT '字典值（存储）',
    `dict_type`   varchar(100) NOT NULL COMMENT '字典类型',
    `css_class`   varchar(100)          DEFAULT NULL COMMENT '样式属性',
    `list_class`  varchar(100)          DEFAULT NULL COMMENT '表格回显样式',
    `is_default`  tinyint   NOT NULL DEFAULT 0 COMMENT '是否默认：0否 1是',
    `status`      tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0正常 1停用',
    `remark`      varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_by`   varchar(64)           DEFAULT NULL COMMENT '创建者',
    `create_time` datetime(3)           DEFAULT NULL COMMENT '创建时间',
    `update_by`   varchar(64)           DEFAULT NULL COMMENT '更新者',
    `update_time` datetime(3)           DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`dict_code`),
    KEY `idx_dict_type` (`dict_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='字典数据表';

-- ---------- 参数配置 ----------
CREATE TABLE `sys_config`
(
    `config_id`    bigint       NOT NULL AUTO_INCREMENT COMMENT '参数主键',
    `config_name`  varchar(100) NOT NULL COMMENT '参数名称',
    `config_key`   varchar(100) NOT NULL COMMENT '参数键名',
    `config_value` varchar(500) NOT NULL COMMENT '参数键值',
    `config_type`  tinyint   NOT NULL DEFAULT 0 COMMENT '是否内置：0否 1是',
    `remark`       varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_by`    varchar(64)           DEFAULT NULL COMMENT '创建者',
    `create_time`  datetime(3)           DEFAULT NULL COMMENT '创建时间',
    `update_by`    varchar(64)           DEFAULT NULL COMMENT '更新者',
    `update_time`  datetime(3)           DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`config_id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='参数配置表';

-- ---------- 通知公告 ----------
CREATE TABLE `sys_notice`
(
    `notice_id`      bigint      NOT NULL AUTO_INCREMENT COMMENT '公告ID',
    `notice_title`   varchar(50) NOT NULL COMMENT '公告标题',
    `notice_type`    tinyint     NOT NULL DEFAULT 1 COMMENT '类型：1通知 2公告',
    `notice_content` longtext NULL COMMENT '公告内容（富文本）',
    `status`         tinyint     NOT NULL DEFAULT 0 COMMENT '状态：0正常 1关闭',
    `create_by`      varchar(64)          DEFAULT NULL COMMENT '创建者',
    `create_time`    datetime(3)          DEFAULT NULL COMMENT '创建时间',
    `update_by`      varchar(64)          DEFAULT NULL COMMENT '更新者',
    `update_time`    datetime(3)          DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`notice_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='通知公告表';

-- ---------- 日志 ----------
CREATE TABLE `sys_login_log`
(
    `info_id`        bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
    `username`       varchar(50)  DEFAULT NULL COMMENT '登录账号',
    `ipaddr`         varchar(64)  DEFAULT NULL COMMENT '登录IP',
    `login_location` varchar(255) DEFAULT NULL COMMENT '登录地点',
    `browser`        varchar(50)  DEFAULT NULL COMMENT '浏览器',
    `os`             varchar(50)  DEFAULT NULL COMMENT '操作系统',
    `status`         tinyint NOT NULL DEFAULT 0 COMMENT '状态：0成功 1失败',
    `msg`            varchar(255) DEFAULT NULL COMMENT '提示消息',
    `login_time`     datetime(3)  DEFAULT NULL COMMENT '登录时间',
    PRIMARY KEY (`info_id`),
    KEY `idx_login_time` (`login_time`),
    KEY `idx_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='登录日志表';

CREATE TABLE `sys_oper_log`
(
    `oper_id`        bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键',
    `title`          varchar(50)   DEFAULT NULL COMMENT '模块标题',
    `business_type`  tinyint NOT NULL DEFAULT 0 COMMENT '业务类型：0其它 1新增 2修改 3删除 4导出 5导入',
    `method`         varchar(200)  DEFAULT NULL COMMENT '方法名称',
    `request_method` varchar(10)   DEFAULT NULL COMMENT 'HTTP方法',
    `operator_type`  tinyint NOT NULL DEFAULT 0 COMMENT '操作类别：0其它 1后台用户',
    `oper_name`      varchar(50)   DEFAULT NULL COMMENT '操作人',
    `dept_name`      varchar(50)   DEFAULT NULL COMMENT '部门名称',
    `oper_url`       varchar(255)  DEFAULT NULL COMMENT '请求URL',
    `oper_ip`        varchar(64)   DEFAULT NULL COMMENT '主机地址',
    `oper_location`  varchar(255)  DEFAULT NULL COMMENT '操作地点',
    `oper_param`     varchar(2000) DEFAULT NULL COMMENT '请求参数（已脱敏）',
    `json_result`    varchar(2000) DEFAULT NULL COMMENT '返回结果',
    `status`         tinyint NOT NULL DEFAULT 0 COMMENT '状态：0成功 1失败',
    `error_msg`      varchar(2000) DEFAULT NULL COMMENT '错误消息',
    `cost_time`      bigint  NOT NULL DEFAULT 0 COMMENT '耗时（毫秒）',
    `oper_time`      datetime(3)   DEFAULT NULL COMMENT '操作时间',
    PRIMARY KEY (`oper_id`),
    KEY `idx_oper_time` (`oper_time`),
    KEY `idx_title` (`title`),
    KEY `idx_business_type` (`business_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='操作日志表';
