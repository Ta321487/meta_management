package com.metadata.mapper;

import com.metadata.common.PageRequest;
import com.metadata.entity.MetadataModule;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 模块Mapper
 */
public interface MetadataModuleMapper {
    int insert(MetadataModule module);
    int update(MetadataModule module);
    int deleteById(Long id);
    int batchDelete(@Param("ids") List<Long> ids);
    MetadataModule selectByCode(@Param("moduleCode") String moduleCode, @Param("businessCode") String businessCode);
    
    /**
     * 根据模块编码查询模块（默认业务系统）
     */
    default MetadataModule selectByCode(@Param("moduleCode") String moduleCode) {
        return selectByCode(moduleCode, "");
    }
    MetadataModule selectById(Long id);
    List<MetadataModule> selectAll(@Param("moduleName") String moduleName, 
                                    @Param("moduleType") String moduleType,
                                    @Param("status") Integer status,
                                    @Param("businessCode") String businessCode);
    
    /**
     * 查询所有模块（默认业务系统）
     */
    default List<MetadataModule> selectAll(@Param("moduleName") String moduleName, 
                                    @Param("moduleType") String moduleType,
                                    @Param("status") Integer status) {
        return selectAll(moduleName, moduleType, status, "");
    }
    
    /**
     * 查询所有模块（无参数，默认业务系统）
     */
    default List<MetadataModule> selectAll() {
        return selectAll(null, null, null, "");
    }
    
    int countByCode(@Param("moduleCode") String moduleCode, @Param("businessCode") String businessCode);
    
    /**
     * 根据模块编码统计模块数量（默认业务系统）
     */
    default int countByCode(@Param("moduleCode") String moduleCode) {
        return countByCode(moduleCode, "");
    }
    
    Long count(@Param("moduleName") String moduleName, 
               @Param("moduleType") String moduleType,
               @Param("status") Integer status,
               @Param("businessCode") String businessCode);
    
    /**
     * 统计模块数量（默认业务系统）
     */
    default Long count(@Param("moduleName") String moduleName, 
               @Param("moduleType") String moduleType,
               @Param("status") Integer status) {
        return count(moduleName, moduleType, status, "");
    }
    
    List<MetadataModule> selectPage(@Param("moduleName") String moduleName, 
                                    @Param("moduleType") String moduleType,
                                    @Param("status") Integer status,
                                    @Param("businessCode") String businessCode,
                                    @Param("pageRequest") PageRequest pageRequest);
    
    /**
     * 分页查询模块（默认业务系统）
     */
    default List<MetadataModule> selectPage(@Param("moduleName") String moduleName, 
                                    @Param("moduleType") String moduleType,
                                    @Param("status") Integer status,
                                    @Param("pageRequest") PageRequest pageRequest) {
        return selectPage(moduleName, moduleType, status, "", pageRequest);
    }
    
    /**
     * 根据模块编码列表查询模块
     */
    List<MetadataModule> selectByCodes(@Param("moduleCodes") List<String> moduleCodes);
    
    /**
     * 根据业务系统编码查询模块
     */
    List<MetadataModule> selectByBusinessCode(@Param("businessCode") String businessCode);
}

