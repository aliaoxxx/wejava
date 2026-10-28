-- ============================================================
-- V9: 把 AI 密钥也纳入参数设置
--
-- 优先读环境变量 DEEPSEEK_API_KEY；环境变量为空时才用这里的值。
-- 注意：该值在「参数设置」页明文可见，仅 system:config:list 权限可查看，
-- 生产环境建议仍以环境变量为准，此处留空。
-- ============================================================

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`,
                          `create_time`, `remark`)
VALUES ('AI-API Key', 'sys.ai.apiKey', '', 1, 'admin', NOW(3),
        'DeepSeek API Key；留空则读环境变量 DEEPSEEK_API_KEY，明文可见请谨慎')
ON DUPLICATE KEY UPDATE `config_name` = VALUES(`config_name`);
