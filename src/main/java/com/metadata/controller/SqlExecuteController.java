package com.metadata.controller;

import com.metadata.common.Result;
import com.metadata.common.SqlExecuteMultipleResultPayload;
import com.metadata.common.SqlExecuteRequest;
import com.metadata.common.SqlExecuteSingleResultPayload;
import com.metadata.common.codes.ApiMessages;
import com.metadata.common.codes.AppErrorCodes;
import com.metadata.exception.BizException;
import com.metadata.service.MySqlPhysicalCatalogService;
import com.metadata.service.SqlExecuteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * SQL执行控制器
 */
@RestController
@RequestMapping("/api/sql")
@Tag(name = "SQL执行管理", description = "SQL执行相关API")
public class SqlExecuteController {

    @Autowired
    private SqlExecuteService sqlExecuteService;

    @Autowired
    private MySqlPhysicalCatalogService mySqlPhysicalCatalogService;

    /**
     * 执行SQL语句
     */
    @PostMapping("/execute")
    @Operation(summary = "执行单条 SQL", description = "执行一条 SQL，返回是否成功、影响行数或查询结果集等")
    public Result<SqlExecuteSingleResultPayload> executeSql(@RequestBody SqlExecuteRequest request) {
        String sql = request.getSql();
        if (sql == null || sql.trim().isEmpty()) {
            throw BizException.of(AppErrorCodes.SQL_TEXT_EMPTY, ApiMessages.SQL_REQUIRED);
        }
        String catalog = resolveTargetCatalog(request.getTargetCatalog());
        return Result.success(SqlExecuteSingleResultPayload.fromMap(
                sqlExecuteService.executeSql(sql, false, catalog)));
    }

    /**
     * 执行多条SQL语句
     */
    @PostMapping("/executeMultiple")
    @Operation(summary = "批量执行 SQL", description = "按分号分隔执行多条 SQL，返回每条明细及汇总统计")
    public Result<SqlExecuteMultipleResultPayload> executeMultipleSql(@RequestBody SqlExecuteRequest request) {
        String sqls = request.getSql();
        if (sqls == null || sqls.trim().isEmpty()) {
            throw BizException.of(AppErrorCodes.SQL_TEXT_EMPTY, ApiMessages.SQL_REQUIRED);
        }
        String catalog = resolveTargetCatalog(request.getTargetCatalog());
        return Result.success(SqlExecuteMultipleResultPayload.fromMap(
                sqlExecuteService.executeMultipleSql(sqls, catalog)));
    }

    private String resolveTargetCatalog(String targetCatalog) {
        if (!StringUtils.hasText(targetCatalog)) {
            throw BizException.of(AppErrorCodes.SQL_TARGET_CATALOG_REQUIRED, ApiMessages.SQL_TARGET_CATALOG_REQUIRED);
        }
        String catalog = targetCatalog.trim();
        if (!mySqlPhysicalCatalogService.isValidCatalogName(catalog)) {
            throw BizException.of(AppErrorCodes.SQL_TARGET_CATALOG_REQUIRED,
                    "库名不符合安全规则：仅允许字母、数字、下划线、美元符号，长度 1–64");
        }
        return catalog;
    }
}
