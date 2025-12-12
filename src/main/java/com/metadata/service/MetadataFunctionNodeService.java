package com.metadata.service;

import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.entity.MetadataFunctionNode;

import java.util.List;

/**
 * 功能节点服务接口
 */
public interface MetadataFunctionNodeService {

    /**
     * 新增节点
     */
    void add(MetadataFunctionNode node);

    /**
     * 更新节点
     */
    void update(MetadataFunctionNode node);

    /**
     * 删除节点
     */
    void delete(Long id);

    /**
     * 查询模块的所有节点
     */
    List<MetadataFunctionNode> listByModuleCode(String moduleCode);
    
    /**
     * 查询模块的所有节点（支持业务系统）
     */
    List<MetadataFunctionNode> listByModuleCodeAndBusinessCode(String moduleCode, String businessCode);

    /**
     * 分页查询模块的节点
     */
    PageResult<MetadataFunctionNode> pageByModuleCode(String moduleCode, PageRequest pageRequest);
    
    /**
     * 分页查询模块的节点（支持业务系统）
     */
    PageResult<MetadataFunctionNode> pageByModuleCodeAndBusinessCode(String moduleCode, String businessCode, PageRequest pageRequest);

    /**
     * 更新节点排序
     */
    void updateSort(Long id, Integer sort);
}