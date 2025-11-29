-- 创建表：${table.tableName}
-- 表编码：${table.tableCode}
-- 描述：${table.description!""}

CREATE TABLE IF NOT EXISTS `${tableName}` (
<#list fields as field>
  <#-- 对于主键字段，根据策略使用不同的列名 -->
  <#if field.fieldName == 'id'>
    <#if table.pkStrategy == 'UUID'>
    `uuid` ${field.fieldType}<#if field.isRequired == 1> NOT NULL</#if> COMMENT '${field.label}'<#if field_has_next>,</#if>
    <#else>
    `id` ${field.fieldType}<#if field.isRequired == 1> NOT NULL</#if><#if table.pkStrategy == 'AUTO'> AUTO_INCREMENT</#if> COMMENT '${field.label}'<#if field_has_next>,</#if>
    </#if>
  <#else>
  `${field.fieldName}` ${field.fieldType}<#if field.isRequired == 1> NOT NULL</#if> COMMENT '${field.label}'<#if field_has_next>,</#if>
  </#if>
</#list>
  <#if table.pkStrategy == 'UUID'>
  ,PRIMARY KEY (`uuid`)
  <#else>
  ,PRIMARY KEY (`id`)
  </#if>
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='${table.description!table.tableName}';

