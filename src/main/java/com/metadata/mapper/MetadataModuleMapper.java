package com.metadata.mapper;

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
    MetadataModule selectByCode(String moduleCode);
    MetadataModule selectById(Long id);
    List<MetadataModule> selectAll(@Param("moduleName") String moduleName, 
                                    @Param("moduleType") String moduleType,
                                    @Param("status") Integer status);
    int countByCode(String moduleCode);
}

