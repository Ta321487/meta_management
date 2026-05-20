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
    MetadataField selectByCode(@Param("tableCode") String tableCode, @Param("fieldCode") String fieldCode, @Param("businessCode") String businessCode);
    
    /**
     * 根据表编码和字段编码查询字段（默认业务系统）
     */
    default MetadataField selectByCode(@Param("tableCode") String tableCode, @Param("fieldCode") String fieldCode) {
        return selectByCode(tableCode, fieldCode, "");
    }
    List<MetadataField> selectByTableCode(@Param("tableCode") String tableCode,
                                         @Param("businessCode") String businessCode,
                                         @Param("includeDisabled") Boolean includeDisabled);

    default List<MetadataField> selectByTableCode(String tableCode, String businessCode) {
        return selectByTableCode(tableCode, businessCode, null);
    }

    /**
     * 查询表的所有字段（默认业务系统）
     */
    default List<MetadataField> selectByTableCode(String tableCode) {
        return selectByTableCode(tableCode, "", null);
    }
    
    int countByCode(@Param("tableCode") String tableCode, @Param("fieldCode") String fieldCode, @Param("businessCode") String businessCode);

    /**
     * 按表+字段编码统计（与 uk_table_field 一致，不按 business_code 过滤）。
     */
    int countByTableAndFieldCode(@Param("tableCode") String tableCode, @Param("fieldCode") String fieldCode);
    
    /**
     * 根据表编码和字段编码统计字段数量（默认业务系统）
     */
    default int countByCode(@Param("tableCode") String tableCode, @Param("fieldCode") String fieldCode) {
        return countByCode(tableCode, fieldCode, "");
    }
    
    Long countByTableCode(@Param("tableCode") String tableCode,
                          @Param("businessCode") String businessCode,
                          @Param("includeDisabled") Boolean includeDisabled);

    default Long countByTableCode(@Param("tableCode") String tableCode, @Param("businessCode") String businessCode) {
        return countByTableCode(tableCode, businessCode, null);
    }

    /**
     * 根据表编码统计字段数量（默认业务系统）
     */
    default Long countByTableCode(@Param("tableCode") String tableCode) {
        return countByTableCode(tableCode, "", null);
    }

    List<MetadataField> selectPageByTableCode(@Param("tableCode") String tableCode,
                                             @Param("businessCode") String businessCode,
                                             @Param("pageRequest") com.metadata.common.PageRequest pageRequest,
                                             @Param("includeDisabled") Boolean includeDisabled);

    default List<MetadataField> selectPageByTableCode(@Param("tableCode") String tableCode,
                                             @Param("businessCode") String businessCode,
                                             @Param("pageRequest") com.metadata.common.PageRequest pageRequest) {
        return selectPageByTableCode(tableCode, businessCode, pageRequest, null);
    }

    /**
     * 分页查询表的字段（默认业务系统）
     */
    default List<MetadataField> selectPageByTableCode(@Param("tableCode") String tableCode,
                                             @Param("pageRequest") com.metadata.common.PageRequest pageRequest) {
        return selectPageByTableCode(tableCode, "", pageRequest, null);
    }
    
    /**
     * 获取表的约束级别
     */
    List<Map<String, Object>> selectConstraintLevels(@Param("tableName") String tableName);
    
    /**
     * 获取表的所有类型约束
     */
    List<Map<String, Object>> selectAllConstraints(@Param("tableName") String tableName, @Param("tableSchema") String tableSchema);

    /**
     * 获取表的所有类型约束（当前连接库）
     */
    default List<Map<String, Object>> selectAllConstraints(String tableName) {
        return selectAllConstraints(tableName, null);
    }
    
    /**
     * 根据表编码更新字段的业务系统
     */
    int updateFieldsBusinessSystemByTable(@Param("tableCode") String tableCode, @Param("businessCode") String businessCode);
    
    /**
     * 批量根据表编码更新字段的业务系统
     */
    int batchUpdateFieldsBusinessSystem(@Param("tableCodes") List<String> tableCodes, @Param("businessCode") String businessCode);
}

