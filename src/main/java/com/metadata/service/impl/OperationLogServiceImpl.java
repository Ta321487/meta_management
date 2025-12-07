package com.metadata.service.impl;

import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.entity.MetadataOperationLog;
import com.metadata.mapper.MetadataOperationLogMapper;
import com.metadata.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志服务实现
 */
@Service
public class OperationLogServiceImpl implements OperationLogService {

    @Autowired
    private MetadataOperationLogMapper logMapper;

    /**
     * 记录操作日志
     */
    @Override
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
    @Override
    public void logSuccess(String operateUser, String operateType, String operateContent) {
        log(operateUser, operateType, operateContent, 1, null);
    }

    /**
     * 记录失败操作
     */
    @Override
    public void logError(String operateUser, String operateType, String operateContent, String errorMsg) {
        log(operateUser, operateType, operateContent, 0, errorMsg);
    }

    /**
     * 查询操作日志列表
     */
    @Override
    public List<MetadataOperationLog> list(String operateType, String startTime, String endTime) {
        return logMapper.selectAll(null, operateType, startTime, endTime);
    }

    /**
     * 分页查询操作日志列表
     */
    @Override
    public PageResult<MetadataOperationLog> page(String operateType, String startTime, String endTime, PageRequest pageRequest) {
        Long total = logMapper.count(null, operateType, startTime, endTime);
        List<MetadataOperationLog> records = logMapper.selectPage(null, operateType, startTime, endTime, pageRequest);
        return new PageResult<>(total, records);
    }
}