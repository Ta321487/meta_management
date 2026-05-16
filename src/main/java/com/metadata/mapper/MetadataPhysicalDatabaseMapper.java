package com.metadata.mapper;

import com.metadata.entity.MetadataPhysicalDatabase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MetadataPhysicalDatabaseMapper {

    List<MetadataPhysicalDatabase> selectAll(@Param("includeDisabled") Boolean includeDisabled);

    MetadataPhysicalDatabase selectById(@Param("id") Long id);

    MetadataPhysicalDatabase selectByCatalogName(@Param("catalogName") String catalogName);

    int insert(MetadataPhysicalDatabase row);

    int update(MetadataPhysicalDatabase row);

    int deleteById(@Param("id") Long id);

    int countByCatalogName(@Param("catalogName") String catalogName);
}
