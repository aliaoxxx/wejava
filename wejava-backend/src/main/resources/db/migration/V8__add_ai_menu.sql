-- ============================================================
-- V8: 「AI 助手」菜单入口
-- ============================================================

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`,
                        `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
                        `create_by`, `create_time`)
SELECT 'AI 助手', 0, 5, 'ai', 'ai/index', 0, 0, 'C', 0, 0, 'ai:chat:use', 'message', 'admin', NOW(3)
WHERE NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE path = 'ai' AND menu_type = 'C') t);
