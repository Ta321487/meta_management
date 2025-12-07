package com.metadata.service.impl;

import com.alibaba.fastjson2.JSON;
import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.entity.MetadataBusinessRule;
import com.metadata.mapper.MetadataBusinessRuleMapper;
import com.metadata.service.MetadataBusinessRuleService;
import com.metadata.service.OperationLogService;
import com.metadata.util.CodeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 业务规则服务实现
 */
@Service
public class MetadataBusinessRuleServiceImpl implements MetadataBusinessRuleService {

    @Autowired
    private MetadataBusinessRuleMapper ruleMapper;

    @Autowired
    private OperationLogService logService;

    /**
     * 新增规则
     */
    @Override
    @Transactional
    public void add(MetadataBusinessRule rule) {
        if (!CodeValidator.isValidCode(rule.getRuleCode())) {
            throw new RuntimeException("规则编码格式不正确");
        }
        ruleMapper.insert(rule);
        logService.logSuccess("admin", "ADD", "新增业务规则：" + JSON.toJSONString(rule));
    }

    /**
     * 更新规则
     */
    @Override
    @Transactional
    public void update(MetadataBusinessRule rule) {
        MetadataBusinessRule existing = ruleMapper.selectByCode(rule.getModuleCode(), rule.getRuleCode());
        if (existing == null) {
            throw new RuntimeException("规则不存在");
        }
        rule.setId(existing.getId());
        ruleMapper.update(rule);
        logService.logSuccess("admin", "EDIT", "更新业务规则：" + JSON.toJSONString(rule));
    }

    /**
     * 删除规则
     */
    @Override
    @Transactional
    public void delete(Long id) {
        ruleMapper.deleteById(id);
        logService.logSuccess("admin", "DELETE", "删除业务规则ID：" + id);
    }

    /**
     * 查询模块的所有规则
     */
    @Override
    public List<MetadataBusinessRule> listByModuleCode(String moduleCode) {
        return ruleMapper.selectByModuleCode(moduleCode);
    }

    /**
     * 分页查询模块的规则
     */
    @Override
    public PageResult<MetadataBusinessRule> pageByModuleCode(String moduleCode, PageRequest pageRequest) {
        Long total = ruleMapper.countByModuleCode(moduleCode);
        List<MetadataBusinessRule> records = ruleMapper.selectPageByModuleCode(moduleCode, pageRequest);
        return new PageResult<>(total, records);
    }
}