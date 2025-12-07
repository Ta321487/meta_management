package com.metadata.service;

import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.entity.MetadataOperationLog;

import java.util.List;

/**
 * 操作日志服务接口
 */
public interface OperationLogService {

    /**
     * 记录操作日志
     */
    void log(String operateUser, String operateType, String operateContent, Integer status, String errorMsg);

    /**
     * 记录成功操作
     */
    void logSuccess(String operateUser, String operateType, String operateContent);

    /**
     * 记录失败操作
     */
    void logError(String operateUser, String operateType, String operateContent, String errorMsg);

    /**
     * 查询操作日志列表
     */
    List<MetadataOperationLog> list(String operateType, String startTime, String endTime);

    /**
     * 分页查询操作日志列表
     */
    PageResult<MetadataOperationLog> page(String operateType, String startTime, String endTime, PageRequest pageRequest);
}