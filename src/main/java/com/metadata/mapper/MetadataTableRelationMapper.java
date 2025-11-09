package com.metadata.mapper;

import com.metadata.entity.MetadataTableRelation;
import java.util.List;

/**
 * 表关联关系Mapper
 */
public interface MetadataTableRelationMapper {
    int insert(MetadataTableRelation relation);
    int update(MetadataTableRelation relation);
    int deleteById(Long id);
    MetadataTableRelation selectByCode(String relationCode);
    List<MetadataTableRelation> selectByMainTableCode(String mainTableCode);
    List<MetadataTableRelation> selectBySlaveTableCode(String slaveTableCode);
    List<MetadataTableRelation> selectAll();
    int countByCode(String relationCode);
}

