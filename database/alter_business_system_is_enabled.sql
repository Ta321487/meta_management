-- 业务系统启用状态：1 启用 0 停用；停用后其下表在业务接口中不可见（管理端可传 includeDisabled）
ALTER TABLE `metadata_business_system`
  ADD COLUMN `is_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '1启用 0停用' AFTER `database_name`;
