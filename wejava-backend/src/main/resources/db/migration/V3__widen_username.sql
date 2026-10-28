-- ============================================================
-- V3: 加宽 sys_user.username
--
-- 原因：逻辑删除时会把用户名改写为 "原名#del_{user_id}"，
-- 雪花 ID 有 19 位，原名最长 30 位，30 + 5 + 19 = 54，原来的 varchar(30) 装不下，
-- 删除用户时报 "Data too long for column 'username'"。
-- 加宽到 64 后，唯一索引 uk_username 仍然可用（64*4=256 字节，远小于 3072）。
-- ============================================================

ALTER TABLE `sys_user`
    MODIFY COLUMN `username` varchar(64) NOT NULL COMMENT '登录账号（逻辑删除时追加 #del_{user_id}）';
