package ${packageName}.service;

import ${packageName}.common.PageRequest;
import ${packageName}.common.PageResult;
import ${packageName}.entity.${className};

import java.util.List;

/**
 * ${table.tableName}服务接口
 * 业务系统：${businessName}
 */
public interface ${className}Service {

    /**
     * 新增
     */
    void add(${className} ${entityName});

    /**
     * 更新
     */
    void update(${className} ${entityName});

    /**
     * 删除
     */
    <#if table.pkStrategy == "UUID">
    void delete(String id);
    
    /**
     * 批量删除
     */
    void batchDelete(List<String> ids);
    
    /**
     * 根据ID查询
     */
    ${className} getById(String id);
    <#else>
    void delete(Long id);
    
    /**
     * 批量删除
     */
    void batchDelete(List<Long> ids);
    
    /**
     * 根据ID查询
     */
    ${className} getById(Long id);
    </#if>

    /**
     * 查询列表
     */
    List<${className}> list();

    /**
     * 分页查询列表（支持条件查询和排序）
     */
    PageResult<${className}> page(PageRequest pageRequest);
    
    /**
     * 条件查询列表（不分页）
     */
    List<${className}> listByCondition(PageRequest pageRequest);
}