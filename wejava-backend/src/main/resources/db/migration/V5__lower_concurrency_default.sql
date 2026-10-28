-- ============================================================
-- V5: 并发数默认值 20 → 10
-- 只更新仍是旧默认值的记录，避免覆盖用户在页面上改过的配置。
-- ============================================================

UPDATE `sys_config`
   SET `config_value` = '10',
       `update_time` = NOW(3)
 WHERE `config_key` = 'sys.request.maxConcurrency'
   AND `config_value` = '20';
