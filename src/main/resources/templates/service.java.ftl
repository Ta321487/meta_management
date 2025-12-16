package ${packageName}.service;

import ${packageName}.common.PageRequest;
import ${packageName}.common.PageResult;
import ${packageName}.entity.${className};
import ${packageName}.mapper.${className}Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
<#if table.pkStrategy == "UUID">
import java.util.UUID;
</#if>

/**
 * ${table.tableName}服务类
 * 业务系统：${businessName}
 */
@Service
public class ${className}Service {

    @Autowired
    private ${className}Mapper ${entityName}Mapper;

    /**
     * 新增
     */
    public void add(${className} ${entityName}) {
        // 验证业务规则
        validateBusinessRules(${entityName});
        ${entityName}Mapper.insert(${entityName});
    }

    /**
     * 更新
     */
    public void update(${className} ${entityName}) {
        // 验证业务规则
        validateBusinessRules(${entityName});
        ${entityName}Mapper.update(${entityName});
    }
    
    /**
     * 验证业务规则
     */
    private void validateBusinessRules(${className} ${entityName}) {
        <#if businessRules?has_content>
        <#list businessRules as rule>
        <#if rule.ruleType == "unique">
        // 单字段唯一性验证
        <#list rule.fields as field>
        // 单字段唯一性验证
        <#assign getterMethod = entityName + ".get" + field.camelCaseName?cap_first + "()" />
        PageRequest pageRequest = new PageRequest();
        pageRequest.getConditions().put("${field.camelCaseName}", ${getterMethod});
        List<${className}> ${entityName}List = ${entityName}Mapper.selectByCondition(pageRequest);
        if (${entityName}List != null && !${entityName}List.isEmpty()) {
            // 更新时排除自身
            if (${entityName}.getId() != null) {
                boolean isDuplicate = ${entityName}List.stream().anyMatch(item -> !item.getId().equals(${entityName}.getId()));
                if (isDuplicate) {
                    throw new RuntimeException("${rule.message}");
                }
            } else {
                throw new RuntimeException("${rule.message}");
            }
        }
        </#list>
        <#elseif rule.ruleType == "unique_combo">
        // 组合字段唯一性验证
        PageRequest pageRequest = new PageRequest();
        <#list rule.fields as field>
        pageRequest.getConditions().put("${field.camelCaseName}", ${entityName}.get${field.camelCaseName?cap_first}());
        </#list>
        List<${className}> ${entityName}List = ${entityName}Mapper.selectByCondition(pageRequest);
        if (${entityName}List != null && !${entityName}List.isEmpty()) {
            // 更新时排除自身
            if (${entityName}.getId() != null) {
                boolean isDuplicate = ${entityName}List.stream().anyMatch(item -> !item.getId().equals(${entityName}.getId()));
                if (isDuplicate) {
                    throw new RuntimeException("${rule.message}");
                }
            } else {
                throw new RuntimeException("${rule.message}");
            }
        }
        </#if>
        </#list>
        </#if>
    }

    /**
     * 删除
     */
    <#if table.pkStrategy == "UUID">
    public void delete(String id) {
        ${entityName}Mapper.deleteById(id);
    }
    
    /**
     * 批量删除
     */
    public void batchDelete(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ${entityName}Mapper.deleteByIds(ids);
    }
    
    /**
     * 根据ID查询
     */
    public ${className} getById(String id) {
        return ${entityName}Mapper.selectById(id);
    }
    <#else>
    public void delete(Long id) {
        ${entityName}Mapper.deleteById(id);
    }
    
    /**
     * 批量删除
     */
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ${entityName}Mapper.deleteByIds(ids);
    }
    
    /**
     * 根据ID查询
     */
    public ${className} getById(Long id) {
        return ${entityName}Mapper.selectById(id);
    }
    </#if>

    /**
     * 查询列表
     */
    public List<${className}> list() {
        return ${entityName}Mapper.selectAll();
    }

    /**
     * 分页查询列表（支持条件查询和排序）
     */
    public PageResult<${className}> page(PageRequest pageRequest) {
        // 查询总数（带条件）
        Long total = ${entityName}Mapper.countByCondition(pageRequest);
        // 分页查询数据（带条件和排序）
        List<${className}> records = ${entityName}Mapper.selectPage(pageRequest);
        return new PageResult<>(total, records);
    }
    
    /**
     * 条件查询列表（不分页）
     */
    public List<${className}> listByCondition(PageRequest pageRequest) {
        return ${entityName}Mapper.selectByCondition(pageRequest);
    }
}

