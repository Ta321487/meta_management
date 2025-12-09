package com.metadata.mapper;

import com.metadata.entity.MetadataBusinessSystem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 业务系统Mapper接口
 */
@Mapper
public interface MetadataBusinessSystemMapper {
    
    /**
     * 查询所有业务系统
     */
    List<MetadataBusinessSystem> selectAll();
    
    /**
     * 根据编码查询业务系统
     */
    MetadataBusinessSystem selectByCode(@Param("businessCode") String businessCode);
    
    /**
     * 查询默认业务系统
     */
    MetadataBusinessSystem selectDefault();
    
    /**
     * 新增业务系统
     */
    int insert(MetadataBusinessSystem businessSystem);
    
    /**
     * 更新业务系统
     */
    int update(MetadataBusinessSystem businessSystem);
    
    /**
     * 删除业务系统
     */
    int delete(Long id);
}