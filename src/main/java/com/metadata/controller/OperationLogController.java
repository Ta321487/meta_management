package com.metadata.controller;

import com.metadata.common.Result;
import com.metadata.entity.MetadataOperationLog;
import com.metadata.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 操作日志控制器
 */
@RestController
@RequestMapping("/api/operationLog")
public class OperationLogController {

    @Autowired
    private OperationLogService logService;

    /**
     * 查询操作日志列表
     */
    @GetMapping("/list")
    public Result<List<MetadataOperationLog>> list(@RequestParam(required = false) String operateType,
                                                    @RequestParam(required = false) String startTime,
                                                    @RequestParam(required = false) String endTime) {
        try {
            List<MetadataOperationLog> list = logService.list(operateType, startTime, endTime);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

