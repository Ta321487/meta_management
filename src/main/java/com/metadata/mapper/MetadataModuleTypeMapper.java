package com.metadata.mapper;

import com.metadata.entity.MetadataModuleType;
import java.util.List;

/**
 * 模块类型Mapper
 */
public interface MetadataModuleTypeMapper {
    int insert(MetadataModuleType type);
    int update(MetadataModuleType type);
    int deleteById(Long id);
    MetadataModuleType selectByCode(String typeCode);
    List<MetadataModuleType> selectAll();
}

