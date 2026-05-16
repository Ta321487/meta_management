package com.metadata.service;

import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.entity.MetadataModuleType;

import java.util.List;

/**
 * 模块类型服务接口
 */
public interface MetadataModuleTypeService {

    /**
     * 查询所有模块类型
     */
    List<MetadataModuleType> listAll();

    /**
     * 条件查询模块类型列表
     */
    List<MetadataModuleType> list(String typeCode, String typeName);

    /**
     * 分页查询模块类型
     */
    PageResult<MetadataModuleType> page(String typeCode, String typeName, PageRequest pageRequest);

    /**
     * 根据编码查询
     */
    MetadataModuleType getByCode(String typeCode);

    /**
     * 新增模块类型
     */
    void add(MetadataModuleType type);

    /**
     * 更新模块类型
     */
    void update(MetadataModuleType type);

    /**
     * 删除模块类型
     */
    void delete(Long id);

    /**
     * 批量删除模块类型
     */
    void batchDelete(List<Long> ids);
}
