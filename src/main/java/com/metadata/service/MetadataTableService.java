package com.metadata.service;

import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.entity.MetadataTable;

import java.util.List;

/**
 * 表服务接口
 */
public interface MetadataTableService {

    /**
     * 新增表
     */
    void add(MetadataTable table);

    /**
     * 更新表
     */
    void update(MetadataTable table);

    /**
     * 删除表
     */
    void delete(Long id);

    /**
     * 批量删除表
     */
    void batchDelete(List<Long> ids);

    /**
     * 查询表详情
     */
    MetadataTable getByCode(String tableCode);

    /**
     * 查询所有表
     */
    List<MetadataTable> list(String tableName);

    /**
     * 分页查询表
     */
    PageResult<MetadataTable> page(String tableName, PageRequest pageRequest);

    /**
     * 根据模块编码查询表
     */
    List<MetadataTable> listByModuleCode(String moduleCode);
}