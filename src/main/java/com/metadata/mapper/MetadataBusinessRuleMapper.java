package com.metadata.mapper;

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
    MetadataBusinessRule selectByCode(@Param("moduleCode") String moduleCode, @Param("ruleCode") String ruleCode);
    List<MetadataBusinessRule> selectByModuleCode(String moduleCode);
}

