package com.metadata.mapper;

import com.metadata.common.PageRequest;
import com.metadata.entity.MetadataOperationLog;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 操作日志Mapper
 */
public interface MetadataOperationLogMapper {
    int insert(MetadataOperationLog log);
    List<MetadataOperationLog> selectAll(@Param("module") String module,
                                         @Param("operateType") String operateType,
                                         @Param("startTime") String startTime,
                                         @Param("endTime") String endTime);
    Long count(@Param("module") String module,
               @Param("operateType") String operateType,
               @Param("startTime") String startTime,
               @Param("endTime") String endTime);
    List<MetadataOperationLog> selectPage(@Param("module") String module,
                                         @Param("operateType") String operateType,
                                         @Param("startTime") String startTime,
                                         @Param("endTime") String endTime,
                                         @Param("pageRequest") PageRequest pageRequest);
}

