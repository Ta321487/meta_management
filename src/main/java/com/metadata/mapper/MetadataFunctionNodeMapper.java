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
    MetadataFunctionNode selectByCode(@Param("moduleCode") String moduleCode, @Param("nodeCode") String nodeCode, @Param("businessCode") String businessCode);
    
    /**
     * 根据模块编码和节点编码查询功能节点（默认业务系统）
     */
    default MetadataFunctionNode selectByCode(@Param("moduleCode") String moduleCode, @Param("nodeCode") String nodeCode) {
        return selectByCode(moduleCode, nodeCode, "DEFAULT");
    }
    List<MetadataFunctionNode> selectByModuleCode(@Param("moduleCode") String moduleCode, @Param("businessCode") String businessCode);
    
    /**
     * 根据模块编码查询功能节点（默认业务系统）
     */
    default List<MetadataFunctionNode> selectByModuleCode(String moduleCode) {
        return selectByModuleCode(moduleCode, "DEFAULT");
    }
    
    List<MetadataFunctionNode> selectByRelatedTableCode(@Param("relatedTableCode") String relatedTableCode, @Param("businessCode") String businessCode);
    
    /**
     * 根据关联表编码查询功能节点（默认业务系统）
     */
    default List<MetadataFunctionNode> selectByRelatedTableCode(@Param("relatedTableCode") String relatedTableCode) {
        return selectByRelatedTableCode(relatedTableCode, "DEFAULT");
    }
    
    int updateSort(@Param("id") Long id, @Param("sort") Integer sort);
    Long countByModuleCode(@Param("moduleCode") String moduleCode, @Param("businessCode") String businessCode);
    
    /**
     * 根据模块编码统计功能节点数量（默认业务系统）
     */
    default Long countByModuleCode(String moduleCode) {
        return countByModuleCode(moduleCode, "DEFAULT");
    }
    
    List<MetadataFunctionNode> selectPageByModuleCode(@Param("moduleCode") String moduleCode, 
                                                       @Param("businessCode") String businessCode,
                                                       @Param("pageRequest") PageRequest pageRequest);
    
    /**
     * 分页查询模块的功能节点（默认业务系统）
     */
    default List<MetadataFunctionNode> selectPageByModuleCode(@Param("moduleCode") String moduleCode, 
                                                       @Param("pageRequest") PageRequest pageRequest) {
        return selectPageByModuleCode(moduleCode, "DEFAULT", pageRequest);
    }
}

