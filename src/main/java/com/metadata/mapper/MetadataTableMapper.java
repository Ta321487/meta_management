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
    MetadataTable selectByCode(@Param("tableCode") String tableCode, @Param("businessCode") String businessCode);
    
    /**
     * 根据表编码查询表（默认业务系统）
     */
    default MetadataTable selectByCode(String tableCode) {
        return selectByCode(tableCode, "");
    }
    MetadataTable selectById(Long id);
    List<MetadataTable> selectAll(@Param("tableName") String tableName, @Param("businessCode") String businessCode);
    
    /**
     * 查询所有表（默认业务系统）
     */
    default List<MetadataTable> selectAll(@Param("tableName") String tableName) {
        return selectAll(tableName, "");
    }
    
    int countByCode(String tableCode);
    List<MetadataTable> selectByModuleCode(@Param("moduleCode") String moduleCode, @Param("businessCode") String businessCode);
    
    /**
     * 根据模块编码查询表（默认业务系统）
     */
    default List<MetadataTable> selectByModuleCode(String moduleCode) {
        return selectByModuleCode(moduleCode, "");
    }
    
    Long count(@Param("tableName") String tableName, @Param("businessCode") String businessCode);
    
    /**
     * 统计表数量（默认业务系统）
     */
    default Long count(@Param("tableName") String tableName) {
        return count(tableName, "");
    }
    
    List<MetadataTable> selectPage(@Param("tableName") String tableName, 
                                    @Param("businessCode") String businessCode,
                                    @Param("pageRequest") PageRequest pageRequest);
    
    /**
     * 分页查询表（默认业务系统）
     */
    default List<MetadataTable> selectPage(@Param("tableName") String tableName, 
                                    @Param("pageRequest") PageRequest pageRequest) {
        return selectPage(tableName, "", pageRequest);
    }
    
    /**
     * 根据表编码列表查询表
     */
    List<MetadataTable> selectByCodes(@Param("tableCodes") List<String> tableCodes, @Param("businessCode") String businessCode);
    
    /**
     * 根据表编码列表查询表（默认业务系统）
     */
    default List<MetadataTable> selectByCodes(@Param("tableCodes") List<String> tableCodes) {
        return selectByCodes(tableCodes, "");
    }

    /**
     * 表级物理库名与参数一致（忽略大小写、首尾空格）的表数量
     */
    int countByDatabaseName(@Param("databaseName") String databaseName);

    /**
     * 表级物理库名与参数一致（忽略大小写、首尾空格）的表列表
     */
    List<MetadataTable> selectByDatabaseName(@Param("databaseName") String databaseName);

    /**
     * 将指定业务系统下表级物理库名为空的记录写入给定库名。
     *
     * @return 更新行数
     */
    int updateEmptyDatabaseNameByBusinessCode(@Param("businessCode") String businessCode,
                                              @Param("databaseName") String databaseName);
}

