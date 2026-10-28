-- ============================================================
-- V4: 增加「同一用户并发数」参数
--
-- 含义：同一 IP 下、同一用户同时处理的请求数上限。
-- 0 表示不限制。默认 20：管理后台正常操作并发 5~10，20 足够宽松，
-- 只在脚本疯狂刷接口时才会触发。
-- ============================================================

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`,
                          `create_time`, `remark`)
VALUES ('接口-同一用户最大并发数', 'sys.request.maxConcurrency', '20', 1, 'admin', NOW(3),
        '同一 IP + 同一用户同时处理的请求数上限，0 表示不限制')
ON DUPLICATE KEY UPDATE `config_name` = VALUES(`config_name`);
