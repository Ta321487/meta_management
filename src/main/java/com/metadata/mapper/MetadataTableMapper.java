package com.metadata.mapper;

import com.metadata.common.PageRequest;
import com.metadata.entity.MetadataTable;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 表Mapper
 */
public interface MetadataTableMapper {
    int insert(MetadataTable table);
    int update(MetadataTable table);
    int deleteById(Long id);
    int batchDelete(@Param("ids") List<Long> ids);
    MetadataTable selectByCode(String tableCode);
    MetadataTable selectById(Long id);
    List<MetadataTable> selectAll(@Param("tableName") String tableName);
    int countByCode(String tableCode);
    List<MetadataTable> selectByModuleCode(String moduleCode);
    Long count(@Param("tableName") String tableName);
    List<MetadataTable> selectPage(@Param("tableName") String tableName, 
                                    @Param("pageRequest") PageRequest pageRequest);
}

