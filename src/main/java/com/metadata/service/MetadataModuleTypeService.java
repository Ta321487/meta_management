package com.metadata.service;

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
}