-- 为已有 metadata_db 增加「物理库」字段（执行一次即可；若列已存在会报错，可忽略）
USE `metadata_db`;

ALTER TABLE `metadata_business_system`
  ADD COLUMN `database_name` varchar(128) DEFAULT NULL COMMENT '默认物理库(MySQL schema)' AFTER `package_name`;

ALTER TABLE `metadata_table`
  ADD COLUMN `database_name` varchar(128) DEFAULT NULL COMMENT '表级物理库，空则用业务系统默认库' AFTER `business_code`;
