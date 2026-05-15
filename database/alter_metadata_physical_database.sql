-- 已有 metadata_db 时执行一次：物理库登记表
USE `metadata_db`;

CREATE TABLE IF NOT EXISTS `metadata_physical_database` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `catalog_name` varchar(64) NOT NULL COMMENT 'MySQL 库名(schema)',
  `display_name` varchar(100) DEFAULT NULL COMMENT '展示名称',
  `description` varchar(500) DEFAULT NULL COMMENT '说明',
  `charset_name` varchar(64) NOT NULL DEFAULT 'utf8mb4' COMMENT '字符集(登记说明)',
  `collation_name` varchar(64) NOT NULL DEFAULT 'utf8mb4_unicode_ci' COMMENT '排序规则(登记说明)',
  `sync_to_instance` tinyint NOT NULL DEFAULT 1 COMMENT '保存时是否在实例执行 CREATE IF NOT EXISTS：1-是 0-否',
  `is_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_catalog_name` (`catalog_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物理库登记表';
