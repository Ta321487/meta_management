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
    <#if field.field.fieldName == "id" && table.pkStrategy == "UUID">
    @TableField("uuid")
    private UUID id;
    <#else>
    private ${field.javaType} ${field.camelCaseName};
    </#if>
</#list>
    <#if table.pkStrategy == "UUID">
    /**
     * 无参构造函数，自动生成UUID主键
     */
    public ${className}() {
        this.id = UUID.randomUUID();
    }
    </#if>
}

