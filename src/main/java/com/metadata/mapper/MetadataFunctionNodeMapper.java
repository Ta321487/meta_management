package com.metadata.mapper;

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
    int updateSort(@Param("id") Long id, @Param("sort") Integer sort);
}

