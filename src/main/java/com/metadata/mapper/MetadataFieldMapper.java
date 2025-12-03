package com.metadata.mapper;

import com.metadata.entity.MetadataField;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

/**
 * 字段Mapper
 */
public interface MetadataFieldMapper {
    int insert(MetadataField field);
    int update(MetadataField field);
    int deleteById(Long id);
    int deleteByTableCode(String tableCode);
    int batchDelete(@Param("ids") List<Long> ids);
    MetadataField selectById(Long id);
    MetadataField selectByCode(@Param("tableCode") String tableCode, @Param("fieldCode") String fieldCode);
    List<MetadataField> selectByTableCode(String tableCode);
    int countByCode(@Param("tableCode") String tableCode, @Param("fieldCode") String fieldCode);
    Long countByTableCode(String tableCode);
    List<MetadataField> selectPageByTableCode(@Param("tableCode") String tableCode, @Param("pageRequest") com.metadata.common.PageRequest pageRequest);
    
    /**
     * 获取表的约束级别
     */
    List<Map<String, Object>> selectConstraintLevels(@Param("tableName") String tableName);
    
    /**
     * 获取表的所有类型约束
     */
    List<Map<String, Object>> selectAllConstraints(@Param("tableName") String tableName);
}

