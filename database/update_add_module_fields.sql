-- 数据库更新脚本：为metadata_module表添加新字段
-- 作者：AI Assistant
-- 时间：2024-07-26

-- 为metadata_module表添加排序号字段
ALTER TABLE metadata_module
ADD COLUMN sort INT(11) NULL DEFAULT 1 COMMENT '排序号' AFTER status;

-- 为metadata_module表添加图标字段
ALTER TABLE metadata_module
ADD COLUMN icon VARCHAR(50) NULL COMMENT '图标' AFTER sort;

-- 为metadata_module表添加路由路径字段
ALTER TABLE metadata_module
ADD COLUMN route_path VARCHAR(200) NULL COMMENT '路由路径' AFTER icon;

-- 为metadata_module表添加组件路径字段
ALTER TABLE metadata_module
ADD COLUMN component_path VARCHAR(200) NULL COMMENT '组件路径' AFTER route_path;

-- 更新已存在记录的默认值
UPDATE metadata_module
SET sort = IF(sort IS NULL, 1, sort);

-- 为常用字段添加索引
ALTER TABLE metadata_module
ADD INDEX idx_module_type (module_type),
ADD INDEX idx_sort (sort),
ADD INDEX idx_status (status);

-- 记录更新日志
INSERT INTO operation_log (operator, operation_type, operation_desc, create_time)
VALUES ('system', 'UPDATE', '为metadata_module表添加sort、icon、route_path、component_path字段', NOW());

SELECT '数据库更新完成！' AS message;