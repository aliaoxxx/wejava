-- ============================================================
-- V2: 初始化数据
-- 默认账号 admin / admin123（首次登录强制修改）
-- BCrypt 由 htpasswd 生成，Spring 的 BCryptPasswordEncoder 可直接校验 $2a$ / $2y$
-- ============================================================

-- ---------- 部门 ----------
INSERT INTO `sys_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `status`,
                        `create_by`, `create_time`)
VALUES (100, 0, '0', 'WeJava 科技', 0, 'admin', '13800000000', 0, 'admin', NOW(3)),
       (101, 100, '0,100', '研发部', 1, 'admin', '13800000001', 0, 'admin', NOW(3)),
       (102, 100, '0,100', '市场部', 2, 'admin', '13800000002', 0, 'admin', NOW(3));

-- ---------- 用户 ----------
INSERT INTO `sys_user` (`user_id`, `dept_id`, `username`, `nickname`, `email`, `phone`, `sex`, `password`, `status`,
                        `pwd_update_time`, `create_by`, `create_time`, `remark`)
VALUES (1, 100, 'admin', '超级管理员', 'admin@wejava.dev', '13800000000', '0',
        '{bcrypt}$2a$10$WAl91V47Ss5qvFt2hRcDEel7.HWzHM7/0m6ylbRoNNJWffaYgfwOq',
        0, NOW(3), 'admin', NOW(3), '系统内置超级管理员，不可删除');

-- ---------- 角色 ----------
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `status`, `create_by`,
                        `create_time`, `remark`)
VALUES (1, '超级管理员', 'admin', 1, '1', 0, 'admin', NOW(3), '拥有全部权限'),
       (2, '普通角色', 'common', 2, '5', 0, 'admin', NOW(3), '仅本人数据');

INSERT INTO `sys_user_role` (`user_id`, `role_id`)
VALUES (1, 1);

-- ---------- 岗位 ----------
INSERT INTO `sys_post` (`post_id`, `post_code`, `post_name`, `post_sort`, `status`, `create_by`, `create_time`)
VALUES (1, 'ceo', '董事长', 1, 0, 'admin', NOW(3)),
       (2, 'se', '项目经理', 2, 0, 'admin', NOW(3)),
       (3, 'hr', '人力资源', 3, 0, 'admin', NOW(3));

-- ---------- 字典 ----------
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`)
VALUES (1, '用户性别', 'sys_user_sex', 0, 'admin', NOW(3)),
       (2, '系统开关', 'sys_normal_disable', 0, 'admin', NOW(3)),
       (3, '通知类型', 'sys_notice_type', 0, 'admin', NOW(3)),
       (4, '操作类型', 'sys_oper_type', 0, 'admin', NOW(3));

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `list_class`, `is_default`,
                             `status`, `create_by`, `create_time`)
VALUES (1, '男', '0', 'sys_user_sex', '', 1, 0, 'admin', NOW(3)),
       (2, '女', '1', 'sys_user_sex', '', 0, 0, 'admin', NOW(3)),
       (3, '未知', '2', 'sys_user_sex', '', 0, 0, 'admin', NOW(3)),
       (1, '正常', '0', 'sys_normal_disable', 'primary', 1, 0, 'admin', NOW(3)),
       (2, '停用', '1', 'sys_normal_disable', 'danger', 0, 0, 'admin', NOW(3)),
       (1, '通知', '1', 'sys_notice_type', 'warning', 1, 0, 'admin', NOW(3)),
       (2, '公告', '2', 'sys_notice_type', 'success', 0, 0, 'admin', NOW(3)),
       (1, '新增', '1', 'sys_oper_type', 'info', 0, 0, 'admin', NOW(3)),
       (2, '修改', '2', 'sys_oper_type', 'info', 0, 0, 'admin', NOW(3)),
       (3, '删除', '3', 'sys_oper_type', 'danger', 0, 0, 'admin', NOW(3));

-- ---------- 参数 ----------
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`,
                          `remark`)
VALUES ('账号自助-是否开启注册', 'sys.account.registerUser', 'false', 1, 'admin', NOW(3), '是否开启用户自助注册'),
       ('用户管理-初始密码', 'sys.user.initPassword', '123456', 1, 'admin', NOW(3), '新建用户的默认密码'),
       ('登录-失败锁定次数', 'sys.login.maxRetry', '5', 1, 'admin', NOW(3), '连续失败几次锁定'),
       ('登录-锁定时长(分钟)', 'sys.login.lockMinutes', '15', 1, 'admin', NOW(3), '锁定时长'),
       ('上传-单文件大小(MB)', 'sys.upload.maxSize', '10', 1, 'admin', NOW(3), '单文件大小上限');

-- ---------- 菜单 ----------
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`,
                        `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES
-- 系统管理
(1000, '系统管理', 0, 1, '/system', 'Layout', 'M', 0, 0, NULL, 'setting', 'admin', NOW(3)),

(1001, '用户管理', 1000, 1, 'user', 'system/user/index', 'C', 0, 0, 'system:user:list', 'user', 'admin', NOW(3)),
(10011, '用户查询', 1001, 1, '', '', 'F', 0, 0, 'system:user:query', '', 'admin', NOW(3)),
(10012, '用户新增', 1001, 2, '', '', 'F', 0, 0, 'system:user:add', '', 'admin', NOW(3)),
(10013, '用户修改', 1001, 3, '', '', 'F', 0, 0, 'system:user:edit', '', 'admin', NOW(3)),
(10014, '用户删除', 1001, 4, '', '', 'F', 0, 0, 'system:user:remove', '', 'admin', NOW(3)),
(10015, '重置密码', 1001, 5, '', '', 'F', 0, 0, 'system:user:resetPwd', '', 'admin', NOW(3)),
(10016, '用户导出', 1001, 6, '', '', 'F', 0, 0, 'system:user:export', '', 'admin', NOW(3)),
(10017, '用户导入', 1001, 7, '', '', 'F', 0, 0, 'system:user:import', '', 'admin', NOW(3)),

(1002, '角色管理', 1000, 2, 'role', 'system/role/index', 'C', 0, 0, 'system:role:list', 'peoples', 'admin', NOW(3)),
(10021, '角色查询', 1002, 1, '', '', 'F', 0, 0, 'system:role:query', '', 'admin', NOW(3)),
(10022, '角色新增', 1002, 2, '', '', 'F', 0, 0, 'system:role:add', '', 'admin', NOW(3)),
(10023, '角色修改', 1002, 3, '', '', 'F', 0, 0, 'system:role:edit', '', 'admin', NOW(3)),
(10024, '角色删除', 1002, 4, '', '', 'F', 0, 0, 'system:role:remove', '', 'admin', NOW(3)),
(10025, '角色导出', 1002, 5, '', '', 'F', 0, 0, 'system:role:export', '', 'admin', NOW(3)),
(10026, '分配数据权限', 1002, 6, '', '', 'F', 0, 0, 'system:role:dataScope', '', 'admin', NOW(3)),

(1003, '菜单管理', 1000, 3, 'menu', 'system/menu/index', 'C', 0, 0, 'system:menu:list', 'tree-table', 'admin', NOW(3)),
(10031, '菜单查询', 1003, 1, '', '', 'F', 0, 0, 'system:menu:query', '', 'admin', NOW(3)),
(10032, '菜单新增', 1003, 2, '', '', 'F', 0, 0, 'system:menu:add', '', 'admin', NOW(3)),
(10033, '菜单修改', 1003, 3, '', '', 'F', 0, 0, 'system:menu:edit', '', 'admin', NOW(3)),
(10034, '菜单删除', 1003, 4, '', '', 'F', 0, 0, 'system:menu:remove', '', 'admin', NOW(3)),

(1004, '部门管理', 1000, 4, 'dept', 'system/dept/index', 'C', 0, 0, 'system:dept:list', 'tree', 'admin', NOW(3)),
(10041, '部门查询', 1004, 1, '', '', 'F', 0, 0, 'system:dept:query', '', 'admin', NOW(3)),
(10042, '部门新增', 1004, 2, '', '', 'F', 0, 0, 'system:dept:add', '', 'admin', NOW(3)),
(10043, '部门修改', 1004, 3, '', '', 'F', 0, 0, 'system:dept:edit', '', 'admin', NOW(3)),
(10044, '部门删除', 1004, 4, '', '', 'F', 0, 0, 'system:dept:remove', '', 'admin', NOW(3)),

(1005, '岗位管理', 1000, 5, 'post', 'system/post/index', 'C', 0, 0, 'system:post:list', 'post', 'admin', NOW(3)),
(10051, '岗位查询', 1005, 1, '', '', 'F', 0, 0, 'system:post:query', '', 'admin', NOW(3)),
(10052, '岗位新增', 1005, 2, '', '', 'F', 0, 0, 'system:post:add', '', 'admin', NOW(3)),
(10053, '岗位修改', 1005, 3, '', '', 'F', 0, 0, 'system:post:edit', '', 'admin', NOW(3)),
(10054, '岗位删除', 1005, 4, '', '', 'F', 0, 0, 'system:post:remove', '', 'admin', NOW(3)),
(10055, '岗位导出', 1005, 5, '', '', 'F', 0, 0, 'system:post:export', '', 'admin', NOW(3)),

(1006, '字典管理', 1000, 6, 'dict', 'system/dict/index', 'C', 0, 0, 'system:dict:list', 'dict', 'admin', NOW(3)),
(10061, '字典查询', 1006, 1, '', '', 'F', 0, 0, 'system:dict:query', '', 'admin', NOW(3)),
(10062, '字典新增', 1006, 2, '', '', 'F', 0, 0, 'system:dict:add', '', 'admin', NOW(3)),
(10063, '字典修改', 1006, 3, '', '', 'F', 0, 0, 'system:dict:edit', '', 'admin', NOW(3)),
(10064, '字典删除', 1006, 4, '', '', 'F', 0, 0, 'system:dict:remove', '', 'admin', NOW(3)),
(10065, '字典导出', 1006, 5, '', '', 'F', 0, 0, 'system:dict:export', '', 'admin', NOW(3)),

(1007, '参数设置', 1000, 7, 'config', 'system/config/index', 'C', 0, 0, 'system:config:list', 'edit', 'admin', NOW(3)),
(10071, '参数查询', 1007, 1, '', '', 'F', 0, 0, 'system:config:query', '', 'admin', NOW(3)),
(10072, '参数新增', 1007, 2, '', '', 'F', 0, 0, 'system:config:add', '', 'admin', NOW(3)),
(10073, '参数修改', 1007, 3, '', '', 'F', 0, 0, 'system:config:edit', '', 'admin', NOW(3)),
(10074, '参数删除', 1007, 4, '', '', 'F', 0, 0, 'system:config:remove', '', 'admin', NOW(3)),

(1008, '通知公告', 1000, 8, 'notice', 'system/notice/index', 'C', 0, 0, 'system:notice:list', 'message', 'admin', NOW(3)),
(10081, '公告查询', 1008, 1, '', '', 'F', 0, 0, 'system:notice:query', '', 'admin', NOW(3)),
(10082, '公告新增', 1008, 2, '', '', 'F', 0, 0, 'system:notice:add', '', 'admin', NOW(3)),
(10083, '公告修改', 1008, 3, '', '', 'F', 0, 0, 'system:notice:edit', '', 'admin', NOW(3)),
(10084, '公告删除', 1008, 4, '', '', 'F', 0, 0, 'system:notice:remove', '', 'admin', NOW(3)),

-- 系统监控
(1010, '系统监控', 0, 2, '/monitor', 'Layout', 'M', 0, 0, NULL, 'monitor', 'admin', NOW(3)),
(1011, '在线用户', 1010, 1, 'online', 'monitor/online/index', 'C', 0, 0, 'monitor:online:list', 'online', 'admin', NOW(3)),
(10111, '强制下线', 1011, 1, '', '', 'F', 0, 0, 'monitor:online:forceLogout', '', 'admin', NOW(3)),
(1012, '操作日志', 1010, 2, 'operlog', 'monitor/operlog/index', 'C', 0, 0, 'monitor:operlog:list', 'form', 'admin', NOW(3)),
(10121, '日志查询', 1012, 1, '', '', 'F', 0, 0, 'monitor:operlog:query', '', 'admin', NOW(3)),
(10122, '日志删除', 1012, 2, '', '', 'F', 0, 0, 'monitor:operlog:remove', '', 'admin', NOW(3)),
(10123, '日志导出', 1012, 3, '', '', 'F', 0, 0, 'monitor:operlog:export', '', 'admin', NOW(3)),
(1013, '登录日志', 1010, 3, 'loginlog', 'monitor/loginlog/index', 'C', 0, 0, 'monitor:loginlog:list', 'logininfor', 'admin', NOW(3)),
(10131, '登录查询', 1013, 1, '', '', 'F', 0, 0, 'monitor:loginlog:query', '', 'admin', NOW(3)),
(10132, '登录删除', 1013, 2, '', '', 'F', 0, 0, 'monitor:loginlog:remove', '', 'admin', NOW(3)),
(10133, '登录导出', 1013, 3, '', '', 'F', 0, 0, 'monitor:loginlog:export', '', 'admin', NOW(3)),
(10134, '账户解锁', 1013, 4, '', '', 'F', 0, 0, 'monitor:loginlog:unlock', '', 'admin', NOW(3)),
(1014, '服务监控', 1010, 4, 'server', 'monitor/server/index', 'C', 0, 0, 'monitor:server:list', 'server', 'admin', NOW(3));

-- 超级管理员关联全部菜单
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu`;
