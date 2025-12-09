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
    List<MetadataTable> list(String tableName, String businessCode);

    /**
     * 分页查询表
     */
    PageResult<MetadataTable> page(String tableName, String businessCode, PageRequest pageRequest);

    /**
     * 根据模块编码查询表
     */
    List<MetadataTable> listByModuleCode(String moduleCode, String businessCode);
    
    /**
     * 根据模块编码查询表（默认业务系统）
     */
    default List<MetadataTable> listByModuleCode(String moduleCode) {
        return listByModuleCode(moduleCode, "");
    }
    
    /**
     * 更新表的业务系统
     * @param tableCode 表编码
     * @param businessCode 业务系统编码
     */
    void updateTableBusinessSystem(String tableCode, String businessCode);
    
    /**
     * 批量更新表的业务系统
     * @param tableCodes 表编码列表
     * @param businessCode 业务系统编码
     */
    void batchUpdateTableBusinessSystem(List<String> tableCodes, String businessCode);
    
    /**
     * 批量获取表列表
     * @param tableCodes 表编码列表
     * @return 表列表
     */
    List<MetadataTable> getTablesByCodes(List<String> tableCodes);
}