package com.metadata.controller;

import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.common.Result;
import com.metadata.entity.MetadataOperationLog;
import com.metadata.service.OperationLogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 操作日志控制器
 */
@RestController
@RequestMapping("/api/operationLog")
@Tag(name = "操作日志管理", description = "操作日志相关API")
public class OperationLogController {

    @Autowired
    private OperationLogService logService;

    /**
     * 查询操作日志列表
     */
    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) String operateType,
                          @RequestParam(required = false) String startTime,
                          @RequestParam(required = false) String endTime,
                          @RequestParam(required = false) Integer current,
                          @RequestParam(required = false) Integer size) {
        try {
            // 如果传入了分页参数，使用分页查询
            if (current != null && size != null) {
                PageRequest pageRequest = new PageRequest();
                pageRequest.setCurrent(current);
                pageRequest.setSize(size);
                PageResult<MetadataOperationLog> pageResult = logService.page(operateType, startTime, endTime, pageRequest);
                return Result.success(pageResult);
            }
            // 否则使用非分页查询（兼容旧接口）
            List<MetadataOperationLog> list = logService.list(operateType, startTime, endTime);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

