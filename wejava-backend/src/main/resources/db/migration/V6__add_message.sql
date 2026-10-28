-- ============================================================
-- V6: 站内消息
--
-- 设计说明：
--   1. sys_message 存消息本体（公告投递、系统通知、个人消息）
--   2. sys_message_user 存「谁该收到 + 是否已读」，是收件箱表
--   3. 通知公告发布时不复制正文，而是生成一条 message 并把 source_id 指回公告，
--      这样公告改了内容，用户点进去看到的仍是最新版
-- ============================================================

CREATE TABLE IF NOT EXISTS `sys_message` (
    `message_id`  bigint       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `title`       varchar(100) NOT NULL COMMENT '标题',
    `content`     text                  DEFAULT NULL COMMENT '内容',
    `msg_type`    tinyint      NOT NULL DEFAULT 1 COMMENT '类型：1公告 2系统 3个人',
    `level`       tinyint      NOT NULL DEFAULT 0 COMMENT '级别：0普通 1重要 2紧急',
    `link`        varchar(200)          DEFAULT NULL COMMENT '点击跳转的前端路由',
    `source_id`   bigint                DEFAULT NULL COMMENT '来源业务ID（如公告ID）',
    `create_by`   varchar(64)           DEFAULT NULL COMMENT '创建者',
    `create_time` datetime(3)           DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`message_id`),
    KEY `idx_message_source` (`source_id`),
    KEY `idx_message_type` (`msg_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息';

CREATE TABLE IF NOT EXISTS `sys_message_user` (
    `id`          bigint      NOT NULL AUTO_INCREMENT,
    `message_id`  bigint      NOT NULL COMMENT '消息ID',
    `user_id`     bigint      NOT NULL COMMENT '接收人',
    `read_flag`   tinyint     NOT NULL DEFAULT 0 COMMENT '是否已读：0未读 1已读',
    `read_time`   datetime(3)          DEFAULT NULL COMMENT '阅读时间',
    `create_time` datetime(3)          DEFAULT NULL COMMENT '投递时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_user` (`message_id`, `user_id`),
    KEY `idx_user_unread` (`user_id`, `read_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息收件人';
