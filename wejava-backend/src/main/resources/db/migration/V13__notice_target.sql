-- ============================================================
-- V13: 公告支持定向投递
--   target_type: 1 全员 2 按角色 3 按部门 4 指定用户
--   target_ids : 逗号分隔的 ID 列表
-- 历史数据默认全员，行为不变。
-- ============================================================

ALTER TABLE `sys_notice`
    ADD COLUMN `target_type` tinyint      NOT NULL DEFAULT 1 COMMENT '1全员 2角色 3部门 4用户' AFTER `status`,
    ADD COLUMN `target_ids`  varchar(500)          DEFAULT NULL COMMENT '接收对象ID，逗号分隔' AFTER `target_type`;
