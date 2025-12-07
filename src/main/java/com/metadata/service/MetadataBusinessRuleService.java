package com.metadata.service;

import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.entity.MetadataBusinessRule;

import java.util.List;

/**
 * 业务规则服务接口
 */
public interface MetadataBusinessRuleService {

    /**
     * 新增规则
     */
    void add(MetadataBusinessRule rule);

    /**
     * 更新规则
     */
    void update(MetadataBusinessRule rule);

    /**
     * 删除规则
     */
    void delete(Long id);

    /**
     * 查询模块的所有规则
     */
    List<MetadataBusinessRule> listByModuleCode(String moduleCode);

    /**
     * 分页查询模块的规则
     */
    PageResult<MetadataBusinessRule> pageByModuleCode(String moduleCode, PageRequest pageRequest);
}