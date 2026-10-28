-- ============================================================
-- V7: AI 对话
--   sys_ai_session  一次会话（可多轮）
--   sys_ai_message  单条消息，assistant 记录 token 用量便于成本核算
-- ============================================================

CREATE TABLE IF NOT EXISTS `sys_ai_session` (
    `session_id`  bigint       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `user_id`     bigint       NOT NULL COMMENT '所属用户',
    `title`       varchar(100)          DEFAULT NULL COMMENT '会话标题（取首条提问）',
    `model`       varchar(64)           DEFAULT NULL COMMENT '模型名',
    `create_time` datetime(3)           DEFAULT NULL,
    `update_time` datetime(3)           DEFAULT NULL,
    PRIMARY KEY (`session_id`),
    KEY `idx_ai_session_user` (`user_id`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 会话';

CREATE TABLE IF NOT EXISTS `sys_ai_message` (
    `message_id`        bigint  NOT NULL AUTO_INCREMENT,
    `session_id`        bigint  NOT NULL COMMENT '会话ID',
    `role`              varchar(16) NOT NULL COMMENT 'user / assistant',
    `content`           mediumtext        DEFAULT NULL COMMENT '消息内容',
    `prompt_tokens`     int             DEFAULT NULL COMMENT '输入 token',
    `completion_tokens` int             DEFAULT NULL COMMENT '输出 token',
    `duration_ms`       bigint          DEFAULT NULL COMMENT '耗时',
    `create_time`       datetime(3)     DEFAULT NULL,
    PRIMARY KEY (`message_id`),
    KEY `idx_ai_message_session` (`session_id`, `message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 消息';

-- AI 参数（密钥单独用环境变量覆盖，库里存的是兜底值）
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`,
                          `create_time`, `remark`)
VALUES ('AI-Base URL', 'sys.ai.baseUrl', 'https://api.deepseek.com', 1, 'admin', NOW(3),
        'DeepSeek 兼容 OpenAI 协议，一般无需修改'),
       ('AI-模型', 'sys.ai.model', 'deepseek-chat', 1, 'admin', NOW(3),
        '可选 deepseek-chat / deepseek-reasoner')
ON DUPLICATE KEY UPDATE `config_name` = VALUES(`config_name`);
