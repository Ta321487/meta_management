package com.metadata.service.impl;

import com.alibaba.fastjson2.JSON;
import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.common.codes.AppErrorCodes;
import com.metadata.exception.BizException;
import com.metadata.entity.MetadataFunctionNode;
import com.metadata.mapper.MetadataFunctionNodeMapper;
import com.metadata.service.MetadataFunctionNodeService;
import com.metadata.service.MetadataModuleService;
import com.metadata.service.OperationLogService;
import com.metadata.util.CodeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 功能节点服务实现
 */
@Service
public class MetadataFunctionNodeServiceImpl implements MetadataFunctionNodeService {

    @Autowired
    private MetadataFunctionNodeMapper nodeMapper;

    @Autowired
    private MetadataModuleService moduleService;

    @Autowired
    private OperationLogService logService;

    /**
     * 新增节点
     */
    @Override
    @Transactional
    public void add(MetadataFunctionNode node) {
        node.setNodeCode(CodeValidator.normalizeCode(node.getNodeCode()));
        if (!CodeValidator.isValidCode(node.getNodeCode())) {
            throw BizException.of(AppErrorCodes.FUNCTION_NODE_CODE_INVALID, "节点编码格式不正确");
        }
        node.setIsEnabled(1);
        nodeMapper.insert(node);
        logService.logSuccess("admin", "ADD", "新增功能节点：" + JSON.toJSONString(node));
    }

    /**
     * 更新节点
     */
    @Override
    @Transactional
    public void update(MetadataFunctionNode node) {
        MetadataFunctionNode existing = nodeMapper.selectByCode(node.getModuleCode(), node.getNodeCode(), node.getBusinessCode());
        if (existing == null) {
            throw BizException.of(AppErrorCodes.FUNCTION_NODE_NOT_FOUND, "节点不存在");
        }
        node.setId(existing.getId());
        nodeMapper.update(node);
        logService.logSuccess("admin", "EDIT", "更新功能节点：" + JSON.toJSONString(node));
    }

    /**
     * 删除节点
     */
    @Override
    @Transactional
    public void delete(Long id) {
        nodeMapper.deleteById(id);
        logService.logSuccess("admin", "DELETE", "删除功能节点ID：" + id);
    }

    /**
     * 查询模块的所有节点
     */
    @Override
    public List<MetadataFunctionNode> listByModuleCode(String moduleCode) {
        return nodeMapper.selectByModuleCode(moduleCode);
    }

    /**
     * 查询模块的所有节点（支持业务系统）
     */
    @Override
    public List<MetadataFunctionNode> listByModuleCodeAndBusinessCode(String moduleCode, String businessCode) {
        return nodeMapper.selectByModuleCode(moduleCode, businessCode);
    }

    /**
     * 分页查询模块的节点
     */
    @Override
    public PageResult<MetadataFunctionNode> pageByModuleCode(String moduleCode, PageRequest pageRequest) {
        Long total = nodeMapper.countByModuleCode(moduleCode);
        List<MetadataFunctionNode> records = nodeMapper.selectPageByModuleCode(moduleCode, pageRequest);
        return new PageResult<>(total, records);
    }

    /**
     * 分页查询模块的节点（支持业务系统）
     */
    @Override
    public PageResult<MetadataFunctionNode> pageByModuleCodeAndBusinessCode(String moduleCode, String businessCode, PageRequest pageRequest) {
        Long total = nodeMapper.countByModuleCode(moduleCode, businessCode);
        List<MetadataFunctionNode> records = nodeMapper.selectPageByModuleCode(moduleCode, businessCode, pageRequest);
        return new PageResult<>(total, records);
    }

    /**
     * 更新节点排序
     */
    @Override
    @Transactional
    public void updateSort(Long id, Integer sort) {
        nodeMapper.updateSort(id, sort);
    }

    /**
     * 批量删除节点
     */
    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        nodeMapper.batchDeleteByIds(ids);
        logService.logSuccess("admin", "DELETE", "批量删除功能节点ID：" + ids.toString());
    }
}