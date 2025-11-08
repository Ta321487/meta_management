package com.metadata.mapper;

import com.metadata.entity.MetadataField;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 字段Mapper
 */
public interface MetadataFieldMapper {
    int insert(MetadataField field);
    int update(MetadataField field);
    int deleteById(Long id);
    int deleteByTableCode(String tableCode);
    MetadataField selectById(Long id);
    MetadataField selectByCode(@Param("tableCode") String tableCode, @Param("fieldCode") String fieldCode);
    List<MetadataField> selectByTableCode(String tableCode);
    int countByCode(@Param("tableCode") String tableCode, @Param("fieldCode") String fieldCode);
}

