-- 创建表：${table.tableName}
-- 表编码：${table.tableCode}
-- 描述：${table.description!""}

CREATE TABLE IF NOT EXISTS `${tableName}` (
<#list fields as field>
  <#-- 对于主键字段，根据策略添加AUTO_INCREMENT -->
  <#if field.formComponent == 'primary_key'>
  `${field.fieldName}` ${field.fieldType}<#if field.isRequired == 1> NOT NULL</#if><#if table.pkStrategy == 'AUTO'> AUTO_INCREMENT</#if> COMMENT '${field.label}'<#if field_has_next>,</#if>
  <#else>
  `${field.fieldName}` ${field.fieldType}<#if field.isRequired == 1> NOT NULL</#if> COMMENT '${field.label}'<#if field_has_next>,</#if>
  </#if>
</#list>
  <#-- 动态生成主键约束 -->
  <#assign primaryKeyFieldName = ''>
  <#list fields as field>
    <#if field.formComponent == 'primary_key'>
      <#assign primaryKeyFieldName = field.fieldName>
      <#break>
    </#if>
  </#list>
  <#if primaryKeyFieldName != ''>
  ,PRIMARY KEY (`${primaryKeyFieldName}`)
  </#if>
  <#-- 添加CHECK约束 -->
  <#if checkConstraints?has_content>
    <#list checkConstraints as constraint>
      ,CONSTRAINT ${constraint}
    </#list>
  </#if>
  
  <#-- 添加UNIQUE约束 -->
  <#if uniqueConstraints?has_content>
    <#list uniqueConstraints as constraint>
      ,${constraint}
    </#list>
  </#if>
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='${table.description!table.tableName}';

