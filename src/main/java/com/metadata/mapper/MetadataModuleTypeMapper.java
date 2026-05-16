package com.metadata.mapper;

import com.metadata.common.PageRequest;
import com.metadata.entity.MetadataModuleType;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模块类型Mapper
 */
public interface MetadataModuleTypeMapper {
    int insert(MetadataModuleType type);
    int update(MetadataModuleType type);
    int deleteById(Long id);
    int batchDeleteByIds(@Param("ids") List<Long> ids);
    MetadataModuleType selectByCode(String typeCode);
    List<MetadataModuleType> selectAll();
    List<MetadataModuleType> selectByCondition(@Param("typeCode") String typeCode,
                                             @Param("typeName") String typeName);
    Long count(@Param("typeCode") String typeCode, @Param("typeName") String typeName);
    List<MetadataModuleType> selectPage(@Param("typeCode") String typeCode,
                                        @Param("typeName") String typeName,
                                        @Param("pageRequest") PageRequest pageRequest);
}
