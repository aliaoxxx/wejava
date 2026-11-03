-- ============================================================
-- V15: AI 多服务商
--
-- 背景：原来只支持一个端点（代码里 LIMIT 1），且前端「服务商」字段
-- 后端 Form 里根本没有，填了也不生效。
--
-- 本次改造：
-- - sys_ai_config 变成「端点列表」，用 is_default 标记聊天默认使用的那条
-- - protocol 预留（当前只有 openai 兼容协议实现）
-- - sys_ai_message 记录每条回复实际用的端点与模型，便于排查
-- ============================================================

ALTER TABLE `sys_ai_config`
    ADD COLUMN `config_name` varchar(64)  NOT NULL DEFAULT '' COMMENT '显示名称，如「智谱 GLM」',
    ADD COLUMN `protocol`    varchar(32)  NOT NULL DEFAULT 'openai' COMMENT '协议，当前仅 openai 兼容',
    ADD COLUMN `is_default`  tinyint      NOT NULL DEFAULT 0 COMMENT '聊天默认使用的端点',
    ADD COLUMN `sort`        int          NOT NULL DEFAULT 0 COMMENT '列表排序',
    ADD COLUMN `remark`      varchar(200)          DEFAULT NULL COMMENT '备注，可放厂商文档链接';

-- 现有那行补显示名
UPDATE `sys_ai_config`
SET `config_name` = CONCAT(`provider`, ' ', `model`)
WHERE `config_name` = '';

-- 把现有唯一一行设为默认，保证升级后聊天立即可用
UPDATE `sys_ai_config`
SET `is_default` = 1
ORDER BY `ai_config_id` LIMIT 1;

ALTER TABLE `sys_ai_message`
    ADD COLUMN `config_id` bigint      DEFAULT NULL COMMENT '生成该消息的 AI 配置ID',
    ADD COLUMN `model`     varchar(64) DEFAULT NULL COMMENT '生成该消息的模型';
