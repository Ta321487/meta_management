package com.metadata.service;

import com.metadata.entity.MetadataBusinessSystem;
import java.util.List;

/**
 * 业务系统服务接口
 */
public interface MetadataBusinessSystemService {
    
    /**
     * 查询所有业务系统
     */
    List<MetadataBusinessSystem> listAll();
    
    /**
     * 根据编码查询业务系统
     */
    MetadataBusinessSystem getByCode(String businessCode);
    
    /**
     * 获取默认业务系统
     */
    MetadataBusinessSystem getDefault();
    
    /**
     * 新增业务系统
     */
    void add(MetadataBusinessSystem businessSystem);
    
    /**
     * 更新业务系统
     */
    void update(MetadataBusinessSystem businessSystem);
    
    /**
     * 删除业务系统
     */
    void delete(Long id);
    
    /**
     * 关联业务系统到模块
     * @param businessCode 业务系统编码
     * @param moduleCodes 模块编码列表
     */
    void associateModules(String businessCode, List<String> moduleCodes);
    
    /**
     * 解除业务系统与模块的关联
     * @param businessCode 业务系统编码
     * @param moduleCodes 模块编码列表
     */
    void disassociateModules(String businessCode, List<String> moduleCodes);
    
    /**
     * 获取业务系统关联的模块列表
     * @param businessCode 业务系统编码
     * @return 模块编码列表
     */
    List<String> getAssociatedModules(String businessCode);
}