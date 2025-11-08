package com.metadata.controller;

import com.metadata.common.Result;
import com.metadata.service.SqlExecuteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * SQL执行控制器
 */
@RestController
@RequestMapping("/api/sql")
public class SqlExecuteController {

    @Autowired
    private SqlExecuteService sqlExecuteService;

    /**
     * 执行SQL语句
     */
    @PostMapping("/execute")
    public Result<?> executeSql(@RequestBody Map<String, String> params) {
        try {
            String sql = params.get("sql");
            if (sql == null || sql.trim().isEmpty()) {
                return Result.error("SQL语句不能为空");
            }
            
            Map<String, Object> result = sqlExecuteService.executeSql(sql);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("执行失败: " + e.getMessage());
        }
    }

    /**
     * 执行多条SQL语句
     */
    @PostMapping("/executeMultiple")
    public Result<?> executeMultipleSql(@RequestBody Map<String, String> params) {
        try {
            String sqls = params.get("sql");
            if (sqls == null || sqls.trim().isEmpty()) {
                return Result.error("SQL语句不能为空");
            }
            
            Map<String, Object> result = sqlExecuteService.executeMultipleSql(sqls);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("执行失败: " + e.getMessage());
        }
    }
}

