-- 创建表：${table.tableName}
-- 表编码：${table.tableCode}
-- 描述：${table.description!""}

CREATE TABLE IF NOT EXISTS `${tableName}` (
<#list fields as field>
  `${field.field.fieldName}` ${field.field.fieldType}<#if field.field.isRequired == 1> NOT NULL</#if><#if field.field.fieldName == "id" && table.pkStrategy == "AUTO"> AUTO_INCREMENT</#if> COMMENT '${field.field.label}',
</#list>
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='${table.tableName}';

