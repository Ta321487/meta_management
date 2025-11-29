-- 更新数据库表结构，添加is_enabled字段

-- 为metadata_table表添加is_enabled字段
ALTER TABLE metadata_table ADD COLUMN is_enabled INT DEFAULT 1 COMMENT '是否启用：1=启用，0=禁用';

-- 为metadata_field表添加is_enabled字段
ALTER TABLE metadata_field ADD COLUMN is_enabled INT DEFAULT 1 COMMENT '是否启用：1=启用，0=禁用';

-- 确保现有数据的is_enabled字段都设置为1
UPDATE metadata_table SET is_enabled = 1 WHERE is_enabled IS NULL;
UPDATE metadata_field SET is_enabled = 1 WHERE is_enabled IS NULL;
