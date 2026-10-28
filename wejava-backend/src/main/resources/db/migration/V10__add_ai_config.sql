-- ============================================================
-- V10: AI 配置独立成表 + 独立菜单（仅管理员可见）
--
-- 原因：API Key 混在参数设置里，任何有 system:config:list 权限的人都能看到明文。
-- 迁移完成后删除 sys_config 里原有的 sys.ai.* 三项，避免两处配置打架。
-- ============================================================

CREATE TABLE IF NOT EXISTS `sys_ai_config` (
    `ai_config_id` bigint       NOT NULL AUTO_INCREMENT,
    `provider`     varchar(32)  NOT NULL DEFAULT 'deepseek' COMMENT '服务商',
    `model`        varchar(64)  NOT NULL COMMENT '模型名',
    `base_url`     varchar(200) NOT NULL COMMENT '接口地址',
    `api_key`      text                  DEFAULT NULL COMMENT '密钥（AES 加密后存储）',
    `temperature`  decimal(3, 2) NOT NULL DEFAULT 0.70 COMMENT '温度',
    `max_tokens`   int          NOT NULL DEFAULT 4096 COMMENT '最大输出 token',
    `enabled`      tinyint      NOT NULL DEFAULT 1 COMMENT '是否启用',
    `update_by`    varchar(64)           DEFAULT NULL,
    `update_time`  datetime(3)           DEFAULT NULL,
    PRIMARY KEY (`ai_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 配置';

-- 把参数设置里的三项搬过来（密钥此时仍是明文，由应用首次读取时加密）
INSERT INTO `sys_ai_config` (`provider`, `model`, `base_url`, `api_key`, `update_by`, `update_time`)
SELECT 'deepseek',
       COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'sys.ai.model'), 'deepseek-chat'),
       COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'sys.ai.baseUrl'), 'https://api.deepseek.com'),
       NULLIF((SELECT config_value FROM sys_config WHERE config_key = 'sys.ai.apiKey'), ''),
       'admin', NOW(3)
WHERE NOT EXISTS (SELECT 1 FROM (SELECT ai_config_id FROM sys_ai_config) t);

DELETE FROM `sys_config` WHERE `config_key` IN ('sys.ai.model', 'sys.ai.baseUrl', 'sys.ai.apiKey');

-- 菜单：系统管理 -> AI 配置，仅超管可见（普通角色不分配此菜单）
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`,
                        `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
                        `create_by`, `create_time`)
SELECT 'AI 配置', 1000, 8, 'ai-config', 'ai/config/index', 0, 0, 'C', 0, 0, 'system:ai:config', 'setting',
       'admin', NOW(3)
WHERE NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE path = 'ai-config') t);
