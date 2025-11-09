-- 数据库迁移脚本：为 metadata_table 表添加 database_name 字段
-- 执行时间：请在执行前备份数据库

-- 如果字段不存在，则添加 database_name 字段
ALTER TABLE `metadata_table` 
ADD COLUMN `database_name` varchar(100) DEFAULT NULL COMMENT '所属数据库名称' 
AFTER `pk_strategy`;

-- 注意：对于已存在的表记录，database_name 字段值为 NULL
-- 如果需要为现有记录设置默认值，可以执行以下 SQL（请根据实际情况修改）：
-- UPDATE `metadata_table` SET `database_name` = 'your_default_database' WHERE `database_name` IS NULL;

