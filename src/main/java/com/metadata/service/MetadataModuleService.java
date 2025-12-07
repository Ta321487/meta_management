package com.metadata.service;

import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.entity.MetadataModule;

import java.util.List;

/**
 * 模块服务接口
 */
public interface MetadataModuleService {

    /**
     * 新增模块
     */
    void add(MetadataModule module, List<String> tableCodes);

    /**
     * 更新模块
     */
    void update(MetadataModule module, List<String> tableCodes);

    /**
     * 删除模块
     */
    void delete(Long id);

    /**
     * 批量删除模块
     */
    void batchDelete(List<Long> ids);

    /**
     * 查询模块详情
     */
    MetadataModule getByCode(String moduleCode);

    /**
     * 查询所有模块
     */
    List<MetadataModule> list(String moduleName, String moduleType, Integer status);

    /**
     * 分页查询模块
     */
    PageResult<MetadataModule> page(String moduleName, String moduleType, Integer status, PageRequest pageRequest);

    /**
     * 启用/禁用模块
     */
    void updateStatus(Long id, Integer status);
}