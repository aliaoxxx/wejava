-- ============================================================
-- V17: 代码生成器 - 在线模板编辑
--
-- 模板默认从 classpath:template/gen 读取（在 WAR 内，只读）。
-- 在线编辑后写入本表，渲染时优先使用库里的版本；删除记录即恢复默认。
-- ============================================================

CREATE TABLE `gen_template`
(
    `template_id`   bigint       NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    `template_name` varchar(100) NOT NULL COMMENT '模板文件名，如 entity.java.ftl',
    `content`       mediumtext   NOT NULL COMMENT '模板内容',
    `create_time`   datetime(3)           DEFAULT NULL COMMENT '创建时间',
    `update_time`   datetime(3)           DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`template_id`),
    UNIQUE KEY `uk_template_name` (`template_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '代码生成-模板覆盖';
