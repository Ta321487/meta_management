package com.metadata.service;

import com.metadata.entity.MetadataOperationLog;
import com.metadata.mapper.MetadataOperationLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志服务
 */
@Service
public class OperationLogService {

    @Autowired
    private MetadataOperationLogMapper logMapper;

    /**
     * 记录操作日志
     */
    public void log(String operateUser, String operateType, String operateContent, Integer status, String errorMsg) {
        MetadataOperationLog log = new MetadataOperationLog();
        log.setOperateUser(operateUser);
        log.setOperateType(operateType);
        log.setOperateContent(operateContent);
        log.setStatus(status);
        log.setErrorMsg(errorMsg);
        logMapper.insert(log);
    }

    /**
     * 记录成功操作
     */
    public void logSuccess(String operateUser, String operateType, String operateContent) {
        log(operateUser, operateType, operateContent, 1, null);
    }

    /**
     * 记录失败操作
     */
    public void logError(String operateUser, String operateType, String operateContent, String errorMsg) {
        log(operateUser, operateType, operateContent, 0, errorMsg);
    }

    /**
     * 查询操作日志列表
     */
    public List<MetadataOperationLog> list(String operateType, String startTime, String endTime) {
        return logMapper.selectAll(null, operateType, startTime, endTime);
    }
}

