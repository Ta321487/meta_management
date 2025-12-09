package com.metadata.mapper;

import com.metadata.common.PageRequest;
import com.metadata.entity.MetadataBusinessRule;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 业务规则Mapper
 */
public interface MetadataBusinessRuleMapper {
    int insert(MetadataBusinessRule rule);
    int update(MetadataBusinessRule rule);
    int deleteById(Long id);
    int deleteByModuleCode(String moduleCode);
    MetadataBusinessRule selectByCode(@Param("moduleCode") String moduleCode, @Param("ruleCode") String ruleCode, @Param("businessCode") String businessCode);
    
    /**
     * 根据模块编码和规则编码查询业务规则（默认业务系统）
     */
    default MetadataBusinessRule selectByCode(@Param("moduleCode") String moduleCode, @Param("ruleCode") String ruleCode) {
        return selectByCode(moduleCode, ruleCode, "DEFAULT");
    }
    List<MetadataBusinessRule> selectByModuleCode(@Param("moduleCode") String moduleCode, @Param("businessCode") String businessCode);
    
    /**
     * 根据模块编码查询业务规则（默认业务系统）
     */
    default List<MetadataBusinessRule> selectByModuleCode(String moduleCode) {
        return selectByModuleCode(moduleCode, "DEFAULT");
    }
    
    Long countByModuleCode(@Param("moduleCode") String moduleCode, @Param("businessCode") String businessCode);
    
    /**
     * 根据模块编码统计业务规则数量（默认业务系统）
     */
    default Long countByModuleCode(String moduleCode) {
        return countByModuleCode(moduleCode, "DEFAULT");
    }
    
    List<MetadataBusinessRule> selectPageByModuleCode(@Param("moduleCode") String moduleCode, 
                                                      @Param("businessCode") String businessCode,
                                                      @Param("pageRequest") PageRequest pageRequest);
    
    /**
     * 分页查询模块的业务规则（默认业务系统）
     */
    default List<MetadataBusinessRule> selectPageByModuleCode(@Param("moduleCode") String moduleCode, 
                                                      @Param("pageRequest") PageRequest pageRequest) {
        return selectPageByModuleCode(moduleCode, "DEFAULT", pageRequest);
    }
}

