package com.metadata.controller;

import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.common.Result;
import com.metadata.entity.MetadataOperationLog;
import com.metadata.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @GetMapping("/list")
    @Operation(summary = "查询操作日志", description = "按模块、操作类型、时间范围查询；传入 current、size 时分页返回")
    public Result<?> list(
            @Parameter(description = "模块名称") @RequestParam(required = false) String module,
            @Parameter(description = "操作类型") @RequestParam(required = false) String operateType,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime,
            @Parameter(description = "当前页码") @RequestParam(required = false) Integer current,
            @Parameter(description = "每页条数") @RequestParam(required = false) Integer size) {
        if (current != null && size != null) {
            PageRequest pageRequest = new PageRequest();
            pageRequest.setCurrent(current);
            pageRequest.setSize(size);
            PageResult<MetadataOperationLog> pageResult = logService.page(module, operateType, startTime, endTime, pageRequest);
            return Result.success(pageResult);
        }
        List<MetadataOperationLog> list = logService.list(module, operateType, startTime, endTime);
        return Result.success(list);
    }
}
