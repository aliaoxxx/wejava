-- ============================================================
-- V12: 消息发送菜单 + 消息保留天数参数
-- ============================================================

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`,
                        `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
                        `create_by`, `create_time`)
SELECT '消息发送', 1000, 9, 'message-send', 'message/send/index', 0, 0, 'C', 0, 0,
       'system:message:send', 'message', 'admin', NOW(3)
WHERE NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE path = 'message-send') t);

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`,
                          `create_time`, `remark`)
VALUES ('消息-保留天数', 'sys.message.retentionDays', '30', 1, 'admin', NOW(3),
        '已读消息保留天数，到期由定时任务清理；0 表示不清理')
ON DUPLICATE KEY UPDATE `config_name` = VALUES(`config_name`);
