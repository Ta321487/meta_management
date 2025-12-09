package com.metadata.mapper;

import com.metadata.common.PageRequest;
import com.metadata.entity.MetadataTableRelation;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 表关联关系Mapper
 */
public interface MetadataTableRelationMapper {
    int insert(MetadataTableRelation relation);
    int update(MetadataTableRelation relation);
    int deleteById(Long id);
    MetadataTableRelation selectByCode(@Param("relationCode") String relationCode, @Param("businessCode") String businessCode);
    default MetadataTableRelation selectByCode(@Param("relationCode") String relationCode) {
        return selectByCode(relationCode, "DEFAULT");
    }
    List<MetadataTableRelation> selectByMainTableCode(@Param("mainTableCode") String mainTableCode, @Param("businessCode") String businessCode);
    default List<MetadataTableRelation> selectByMainTableCode(@Param("mainTableCode") String mainTableCode) {
        return selectByMainTableCode(mainTableCode, "DEFAULT");
    }
    List<MetadataTableRelation> selectBySlaveTableCode(@Param("slaveTableCode") String slaveTableCode, @Param("businessCode") String businessCode);
    default List<MetadataTableRelation> selectBySlaveTableCode(@Param("slaveTableCode") String slaveTableCode) {
        return selectBySlaveTableCode(slaveTableCode, "DEFAULT");
    }
    List<MetadataTableRelation> selectAll(@Param("businessCode") String businessCode);
    default List<MetadataTableRelation> selectAll() {
        return selectAll(null);
    }
    int countByCode(@Param("relationCode") String relationCode, @Param("businessCode") String businessCode);
    default int countByCode(@Param("relationCode") String relationCode) {
        return countByCode(relationCode, "DEFAULT");
    }
    Long count(@Param("businessCode") String businessCode);
    default Long count() {
        return count(null);
    }
    List<MetadataTableRelation> selectPage(@Param("pageRequest") PageRequest pageRequest, @Param("businessCode") String businessCode);
    default List<MetadataTableRelation> selectPage(@Param("pageRequest") PageRequest pageRequest) {
        return selectPage(pageRequest, null);
    }
    
    /**
     * 根据ID查询关联关系
     * @param id 关联关系ID
     * @return 关联关系对象
     */
    MetadataTableRelation selectById(@Param("id") Long id);
}

