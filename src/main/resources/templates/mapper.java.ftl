package ${packageName}.mapper;

import ${packageName}.common.PageRequest;
import ${packageName}.entity.${className};
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ${table.tableName}Mapper接口
 */
@Mapper
public interface ${className}Mapper {

    /**
     * 新增
     */
    int insert(${className} ${entityName});

    /**
     * 更新
     */
    int update(${className} ${entityName});

    /**
     * 根据ID删除
     */
    <#if table.pkStrategy == "UUID">
    int deleteById(String id);
    <#else>
    int deleteById(Long id);
    </#if>

    /**
     * 批量删除
     */
    <#if table.pkStrategy == "UUID">
    int deleteByIds(List<String> ids);
    <#else>
    int deleteByIds(List<Long> ids);
    </#if>

    /**
     * 根据ID查询
     */
    <#if table.pkStrategy == "UUID">
    ${className} selectById(String id);
    <#else>
    ${className} selectById(Long id);
    </#if>

    /**
     * 查询所有
     */
    List<${className}> selectAll();

    /**
     * 查询总数
     */
    Long count();
    
    /**
     * 条件查询总数
     */
    Long countByCondition(PageRequest pageRequest);

    /**
     * 分页查询（支持条件查询和排序）
     */
    List<${className}> selectPage(PageRequest pageRequest);
    
    /**
     * 条件查询（不分页，支持排序）
     */
    List<${className}> selectByCondition(PageRequest pageRequest);
}

