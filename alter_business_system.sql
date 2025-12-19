-- 为业务系统表添加包名字段的ALTER TABLE语句
ALTER TABLE metadata_business_system ADD COLUMN package_name VARCHAR(255) COMMENT '包名';