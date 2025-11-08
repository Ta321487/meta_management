package ${packageName}.entity;

import lombok.Data;
<#if hasDate>
import java.time.LocalDateTime;
</#if>
<#if hasDecimal>
import java.math.BigDecimal;
</#if>

/**
 * ${table.tableName}实体类
 * 表编码：${table.tableCode}
 */
@Data
public class ${className} {
<#list fields as field>
    /**
     * ${field.field.label}
     */
    private ${field.javaType} ${field.camelCaseName};
</#list>
}

