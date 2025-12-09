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
    List<MetadataModule> list(String moduleName, String moduleType, String businessCode, Integer status);

    /**
     * 分页查询模块
     */
    PageResult<MetadataModule> page(String moduleName, String moduleType, String businessCode, Integer status, PageRequest pageRequest);

    /**
     * 启用/禁用模块
     */
    void updateStatus(Long id, Integer status);
    
    /**
     * 更新模块的业务系统
     * @param moduleCode 模块编码
     * @param businessCode 业务系统编码
     */
    void updateModuleBusinessSystem(String moduleCode, String businessCode);
    
    /**
     * 批量更新模块的业务系统
     * @param moduleCodes 模块编码列表
     * @param businessCode 业务系统编码
     */
    void batchUpdateModuleBusinessSystem(List<String> moduleCodes, String businessCode);
    
    /**
     * 获取模块列表
     * @param moduleCodes 模块编码列表
     * @return 模块列表
     */
    List<MetadataModule> getModulesByCodes(List<String> moduleCodes);
}