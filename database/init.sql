-- 创建数据库
CREATE DATABASE IF NOT EXISTS `metadata_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `metadata_db`;

-- 1. 业务系统表
CREATE TABLE IF NOT EXISTS `metadata_business_system` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `business_code` varchar(50) NOT NULL COMMENT '业务系统编码',
  `business_name` varchar(100) NOT NULL COMMENT '业务系统名称',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `is_default` tinyint NOT NULL DEFAULT 0 COMMENT '是否默认系统：1-是，0-否',
  `package_name` varchar(255) DEFAULT NULL COMMENT '包名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_business_code` (`business_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务系统表';

-- 2. 模块表（抽象模块）
CREATE TABLE IF NOT EXISTS `metadata_module` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `module_code` varchar(50) NOT NULL COMMENT '模块唯一编码（如MODULE_001）',
  `module_name` varchar(100) NOT NULL COMMENT '模块名称（抽象名）',
  `module_type` varchar(50) NOT NULL COMMENT '模块类型（数据管理型/流程审批型等）',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `sort` int(11) NULL DEFAULT 1 COMMENT '排序号',
  `icon` varchar(50) NULL COMMENT '图标',
  `route_path` varchar(200) NULL COMMENT '路由路径',
  `component_path` varchar(200) NULL COMMENT '组件路径',
  `business_code` varchar(50) NOT NULL DEFAULT 'DEFAULT' COMMENT '业务系统编码',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_module_code` (`module_code`),
  INDEX idx_module_type (module_type),
  INDEX idx_sort (sort),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽象模块表';

-- 3. 模块类型表（预设+自定义）
CREATE TABLE IF NOT EXISTS `metadata_module_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `type_code` varchar(50) NOT NULL COMMENT '类型编码（如DATA_MANAGE）',
  `type_name` varchar(100) NOT NULL COMMENT '类型名称（如数据管理型）',
  `default_nodes` varchar(500) DEFAULT NULL COMMENT '默认功能节点（编码逗号分隔）',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_code` (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块类型表';

-- 4. 抽象表表
CREATE TABLE IF NOT EXISTS `metadata_table` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `table_code` varchar(50) NOT NULL COMMENT '表唯一编码（如TABLE_001）',
  `table_name` varchar(100) NOT NULL COMMENT '表名称（抽象名）',
  `pk_strategy` varchar(20) NOT NULL COMMENT '主键生成策略（AUTO/UUID）',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `is_enabled` INT DEFAULT 1 COMMENT '是否启用：1=启用，0=禁用',
  `business_code` varchar(50) NOT NULL DEFAULT 'DEFAULT' COMMENT '业务系统编码',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_table_code` (`table_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽象表结构表';

-- 5. 模块与表关联表
CREATE TABLE IF NOT EXISTS `metadata_module_table` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `module_code` varchar(50) NOT NULL COMMENT '模块编码',
  `table_code` varchar(50) NOT NULL COMMENT '表编码',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_module_table` (`module_code`,`table_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块与表关联表';

-- 6. 抽象字段表
CREATE TABLE IF NOT EXISTS `metadata_field` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `field_code` varchar(50) NOT NULL COMMENT '字段唯一编码（如FIELD_001）',
  `table_code` varchar(50) NOT NULL COMMENT '所属表编码',
  `field_name` varchar(100) NOT NULL COMMENT '字段名称（抽象名）',
  `field_type` varchar(50) NOT NULL COMMENT '字段类型（如varchar(100)）',
  `label` varchar(100) NOT NULL COMMENT '前端显示名（抽象名）',
  `is_required` tinyint NOT NULL DEFAULT 0 COMMENT '是否必填：1-是，0-否',
  `form_component` varchar(50) NOT NULL COMMENT '表单组件类型',
  `validate_rule` varchar(500) DEFAULT NULL COMMENT '校验规则（JSON）',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `is_enabled` INT DEFAULT 1 COMMENT '是否启用：1=启用，0=禁用',
  `business_code` varchar(50) NOT NULL DEFAULT 'DEFAULT' COMMENT '业务系统编码',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_table_field` (`table_code`,`field_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽象字段表';

-- 7. 功能节点表
CREATE TABLE IF NOT EXISTS `metadata_function_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `node_code` varchar(50) NOT NULL COMMENT '节点编码',
  `node_name` varchar(100) NOT NULL COMMENT '节点名称（抽象名）',
  `module_code` varchar(50) NOT NULL COMMENT '所属模块编码',
  `node_type` varchar(50) NOT NULL COMMENT '节点类型（列表页/表单页等）',
  `related_table_code` varchar(50) DEFAULT NULL COMMENT '关联表编码',
  `jump_relation` varchar(500) DEFAULT NULL COMMENT '跳转关系（如NODE_001→NODE_002）',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `is_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：1-是，0-否',
  `business_code` varchar(50) NOT NULL DEFAULT 'DEFAULT' COMMENT '业务系统编码',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_module_node` (`module_code`,`node_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='功能节点表';

-- 8. 业务规则表
CREATE TABLE IF NOT EXISTS `metadata_business_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_code` varchar(50) NOT NULL COMMENT '规则编码',
  `module_code` varchar(50) NOT NULL COMMENT '所属模块编码',
  `rule_type` varchar(50) NOT NULL COMMENT '规则类型（流程规则/报表规则等）',
  `rule_content` text NOT NULL COMMENT '规则内容（JSON）',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `business_code` varchar(50) NOT NULL DEFAULT 'DEFAULT' COMMENT '业务系统编码',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_module_rule` (`module_code`,`rule_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务规则表';

-- 9. 表关联关系表
CREATE TABLE IF NOT EXISTS `metadata_table_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `relation_code` varchar(50) NOT NULL COMMENT '关联编码',
  `main_table_code` varchar(50) NOT NULL COMMENT '主表编码',
  `slave_table_code` varchar(50) NOT NULL COMMENT '从表编码',
  `main_field_code` varchar(50) NOT NULL COMMENT '主表关联字段编码',
  `slave_field_code` varchar(50) NOT NULL COMMENT '从表外键字段编码',
  `relation_type` varchar(20) NOT NULL COMMENT '关联类型（ONE_TO_ONE/ONE_TO_MANY）',
  `relation_name` varchar(100) NOT NULL COMMENT '关联名称（抽象名）',
  `description` varchar(500) DEFAULT NULL COMMENT '关联关系描述',
  `business_code` varchar(50) NOT NULL DEFAULT 'DEFAULT' COMMENT '业务系统编码',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_relation_code` (`relation_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表关联关系表';

-- 10. 管理员表
CREATE TABLE IF NOT EXISTS `metadata_admin` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL DEFAULT 'admin' COMMENT '登录用户名',
  `password` varchar(100) NOT NULL COMMENT 'BCrypt加密密码',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- 11. 操作日志表
CREATE TABLE IF NOT EXISTS `metadata_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `operate_user` varchar(50) NOT NULL COMMENT '操作人（admin）',
  `operate_type` varchar(50) NOT NULL COMMENT '操作类型（ADD/EDIT/DELETE/EXPORT）',
  `operate_content` text NOT NULL COMMENT '操作内容（JSON格式，记录配置变更）',
  `operate_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `status` tinyint NOT NULL COMMENT '操作状态：1-成功，0-失败',
  `error_msg` text DEFAULT NULL COMMENT '失败原因',
  PRIMARY KEY (`id`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 初始化默认业务系统
INSERT INTO `metadata_business_system` (`business_code`, `business_name`, `description`, `is_default`) VALUES ('DEFAULT', '默认业务系统', '系统默认业务系统', 1)
ON DUPLICATE KEY UPDATE `business_code`=`business_code`;

-- 初始化默认管理员（密码：123456）
-- 注意：BCrypt加密后的密码值，实际部署时可通过运行InitAdminUtil.main()方法生成新的加密值
-- 或者使用以下命令生成：mvn exec:java
-- 已验证：旧的哈希值不匹配密码123456，已更新为新的正确哈希值
INSERT INTO `metadata_admin` (`username`, `password`) VALUES ('admin', '$2a$10$cOBRcgHmW.4dInuPjvhL4OTZVdX8owOYwcBJ1cx9sFaWhm2Acel/K')
ON DUPLICATE KEY UPDATE `username`=`username`;

-- 初始化默认模块类型
INSERT INTO `metadata_module_type` (`type_code`, `type_name`, `default_nodes`, `description`) VALUES
('DATA_MANAGE', '数据管理型', 'LIST_PAGE,FORM_PAGE,DETAIL_PAGE', '支持CRUD操作的数据管理模块'),
('PROCESS_APPROVE', '流程审批型', 'LIST_PAGE,FORM_PAGE,PROCESS_PAGE', '支持流程审批的模块'),
('STAT_REPORT', '统计报表型', 'LIST_PAGE,REPORT_PAGE', '支持统计报表的模块'),
('BATCH_OPERATE', '批量操作型', 'LIST_PAGE,FORM_PAGE,DETAIL_PAGE,IMPORT_PAGE', '支持CRUD和批量导入的模块')
ON DUPLICATE KEY UPDATE `type_code`=`type_code`;

-- 确保现有数据的is_enabled字段都设置为1
UPDATE metadata_table SET is_enabled = 1 WHERE is_enabled IS NULL;
UPDATE metadata_field SET is_enabled = 1 WHERE is_enabled IS NULL;

-- 更新已存在记录的默认值
UPDATE metadata_module SET sort = IF(sort IS NULL, 1, sort);

