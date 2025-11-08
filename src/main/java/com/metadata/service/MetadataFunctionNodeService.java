package com.metadata.service;

import com.alibaba.fastjson2.JSON;
import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.entity.MetadataFunctionNode;
import com.metadata.mapper.MetadataFunctionNodeMapper;
import com.metadata.util.CodeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 功能节点服务
 */
@Service
public class MetadataFunctionNodeService {

    @Autowired
    private MetadataFunctionNodeMapper nodeMapper;

    @Autowired
    private OperationLogService logService;

    /**
     * 新增节点
     */
    @Transactional
    public void add(MetadataFunctionNode node) {
        if (!CodeValidator.isValidCode(node.getNodeCode())) {
            throw new RuntimeException("节点编码格式不正确");
        }
        node.setIsEnabled(1);
        nodeMapper.insert(node);
        logService.logSuccess("admin", "ADD", "新增功能节点：" + JSON.toJSONString(node));
    }

    /**
     * 更新节点
     */
    @Transactional
    public void update(MetadataFunctionNode node) {
        MetadataFunctionNode existing = nodeMapper.selectByCode(node.getModuleCode(), node.getNodeCode());
        if (existing == null) {
            throw new RuntimeException("节点不存在");
        }
        node.setId(existing.getId());
        nodeMapper.update(node);
        logService.logSuccess("admin", "EDIT", "更新功能节点：" + JSON.toJSONString(node));
    }

    /**
     * 删除节点
     */
    @Transactional
    public void delete(Long id) {
        nodeMapper.deleteById(id);
        logService.logSuccess("admin", "DELETE", "删除功能节点ID：" + id);
    }

    /**
     * 查询模块的所有节点
     */
    public List<MetadataFunctionNode> listByModuleCode(String moduleCode) {
        return nodeMapper.selectByModuleCode(moduleCode);
    }

    /**
     * 分页查询模块的节点
     */
    public PageResult<MetadataFunctionNode> pageByModuleCode(String moduleCode, PageRequest pageRequest) {
        Long total = nodeMapper.countByModuleCode(moduleCode);
        List<MetadataFunctionNode> records = nodeMapper.selectPageByModuleCode(moduleCode, pageRequest);
        return new PageResult<>(total, records);
    }

    /**
     * 更新节点排序
     */
    @Transactional
    public void updateSort(Long id, Integer sort) {
        nodeMapper.updateSort(id, sort);
    }
}

