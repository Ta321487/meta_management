package com.metadata.mapper;

import com.metadata.common.PageRequest;
import com.metadata.entity.MetadataFunctionNode;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 功能节点Mapper
 */
public interface MetadataFunctionNodeMapper {
    int insert(MetadataFunctionNode node);
    int update(MetadataFunctionNode node);
    int deleteById(Long id);
    int deleteByModuleCode(String moduleCode);
    MetadataFunctionNode selectByCode(@Param("moduleCode") String moduleCode, @Param("nodeCode") String nodeCode);
    List<MetadataFunctionNode> selectByModuleCode(String moduleCode);
    List<MetadataFunctionNode> selectByRelatedTableCode(@Param("relatedTableCode") String relatedTableCode);
    int updateSort(@Param("id") Long id, @Param("sort") Integer sort);
    Long countByModuleCode(String moduleCode);
    List<MetadataFunctionNode> selectPageByModuleCode(@Param("moduleCode") String moduleCode, 
                                                       @Param("pageRequest") PageRequest pageRequest);
}

