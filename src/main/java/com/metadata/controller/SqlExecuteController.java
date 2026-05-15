package com.metadata.controller;

import com.metadata.common.Result;
import com.metadata.common.SqlExecuteRequest;
import com.metadata.common.codes.ApiMessages;
import com.metadata.common.codes.AppErrorCodes;
import com.metadata.exception.BizException;
import com.metadata.service.SqlExecuteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * SQL执行控制器
 */
@RestController
@RequestMapping("/api/sql")
@Tag(name = "SQL执行管理", description = "SQL执行相关API")
public class SqlExecuteController {

    @Autowired
    private SqlExecuteService sqlExecuteService;

    /**
     * 执行SQL语句
     */
    @PostMapping("/execute")
    public Result<?> executeSql(@RequestBody SqlExecuteRequest request) {
        String sql = request.getSql();
        if (sql == null || sql.trim().isEmpty()) {
            throw BizException.of(AppErrorCodes.SQL_TEXT_EMPTY, ApiMessages.SQL_REQUIRED);
        }
        Map<String, Object> result = sqlExecuteService.executeSql(sql);
        return Result.success(result);
    }

    /**
     * 执行多条SQL语句
     */
    @PostMapping("/executeMultiple")
    public Result<?> executeMultipleSql(@RequestBody SqlExecuteRequest request) {
        String sqls = request.getSql();
        if (sqls == null || sqls.trim().isEmpty()) {
            throw BizException.of(AppErrorCodes.SQL_TEXT_EMPTY, ApiMessages.SQL_REQUIRED);
        }
        Map<String, Object> result = sqlExecuteService.executeMultipleSql(sqls);
        return Result.success(result);
    }
}
