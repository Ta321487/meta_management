package ${packageName}.entity;

import lombok.Data;
<#if hasDate>
import java.time.LocalDateTime;
</#if>
<#if hasDecimal>
import java.math.BigDecimal;
</#if>
<#if table.pkStrategy == "UUID">
import java.util.UUID;
import com.baomidou.mybatisplus.annotation.TableField;
</#if>
<#-- 添加关联实体导入 -->
<#list fields as field>
<#if field.isForeignKey!false>
import ${packageName}.entity.${field.relatedTableClassName};
</#if>
</#list>

/**
 * ${table.tableName}实体类
 * 表编码：${table.tableCode}
 * 业务系统：${businessName}
 */
@Data
public class ${className} {
<#list fields as field>
    /**
     * ${field.field.label}
     */
    <#if field.field.formComponent == "primary_key" && table.pkStrategy == "UUID">
    @TableField("uuid")
    private UUID ${field.camelCaseName};
    <#else>
    private ${field.javaType} ${field.camelCaseName};
    </#if>
</#list>
    <#if table.pkStrategy == "UUID">
    <#-- 无参构造函数，自动生成UUID主键 -->
    <#assign primaryKeyCamelCaseName = "">
    <#list fields as field>
        <#if field.field.formComponent == "primary_key">
            <#assign primaryKeyCamelCaseName = field.camelCaseName>
            <#break>
        </#if>
    </#list>
    <#if primaryKeyCamelCaseName != "">
    /**
     * 无参构造函数，自动生成UUID主键
     */
    public ${className}() {
        this.${primaryKeyCamelCaseName} = UUID.randomUUID();
    }
    </#if>
    </#if>
    
    <#-- 添加关联实体 -->
    <#list fields as field>
    <#if field.isForeignKey!false>
    /**
     * ${field.field.label}关联实体
     */
    private ${field.relatedTableClassName} ${field.relatedTableCamelCaseName};
    </#if>
    </#list>
}

