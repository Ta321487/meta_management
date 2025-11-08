package com.metadata.mapper;

import com.metadata.entity.MetadataModuleTable;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 模块与表关联Mapper
 */
public interface MetadataModuleTableMapper {
    int insert(MetadataModuleTable moduleTable);
    int deleteByModuleCode(String moduleCode);
    int deleteByTableCode(String tableCode);
    int delete(@Param("moduleCode") String moduleCode, @Param("tableCode") String tableCode);
    List<String> selectTableCodesByModuleCode(String moduleCode);
    List<String> selectModuleCodesByTableCode(String tableCode);
}

