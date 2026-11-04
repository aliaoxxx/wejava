-- ============================================================
-- ${table.functionName} 菜单与按钮权限
-- 执行前请确认父菜单 ID：当前使用 ${(table.parentMenuId!'0')}
-- ============================================================

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`,
                        `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
                        `create_by`, `create_time`)
SELECT '${table.functionName}', ${(table.parentMenuId!'0')}, 99, '${table.businessName}',
       '${table.moduleName}/${table.businessName}/index', 0, 0, 'C', 0, 0, '${table.permissionPrefix}:list', 'form', '${table.author}', NOW(3)
WHERE NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE perms = '${table.permissionPrefix}:list') t);

SET @menu_id_${table.businessName} = (SELECT `menu_id` FROM `sys_menu` WHERE `perms` = '${table.permissionPrefix}:list' LIMIT 1);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`,
                        `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
                        `create_by`, `create_time`)
SELECT '${table.functionName}查询', @menu_id_${table.businessName}, 1, '', '', 0, 0, 'F', 0, 0, '${table.permissionPrefix}:query', '', '${table.author}', NOW(3)
WHERE NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE perms = '${table.permissionPrefix}:query') t);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`,
                        `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
                        `create_by`, `create_time`)
SELECT '${table.functionName}新增', @menu_id_${table.businessName}, 2, '', '', 0, 0, 'F', 0, 0, '${table.permissionPrefix}:add', '', '${table.author}', NOW(3)
WHERE NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE perms = '${table.permissionPrefix}:add') t);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`,
                        `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
                        `create_by`, `create_time`)
SELECT '${table.functionName}修改', @menu_id_${table.businessName}, 3, '', '', 0, 0, 'F', 0, 0, '${table.permissionPrefix}:edit', '', '${table.author}', NOW(3)
WHERE NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE perms = '${table.permissionPrefix}:edit') t);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`,
                        `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
                        `create_by`, `create_time`)
SELECT '${table.functionName}删除', @menu_id_${table.businessName}, 4, '', '', 0, 0, 'F', 0, 0, '${table.permissionPrefix}:remove', '', '${table.author}', NOW(3)
WHERE NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE perms = '${table.permissionPrefix}:remove') t);

-- 授权给超级管理员（role_id = 1）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, m.menu_id FROM `sys_menu` m WHERE m.`perms` LIKE '${table.permissionPrefix}:%';
