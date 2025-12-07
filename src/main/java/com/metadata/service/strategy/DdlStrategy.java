package com.metadata.service.strategy;

import com.metadata.service.dto.SqlResult;
import com.metadata.service.constant.SqlConstants;
import com.metadata.service.OperationLogService;
import java.sql.Connection;
import java.sql.Statement;

/**
 * DDL语句执行策略
 * 处理CREATE、ALTER、DROP等数据定义语句
 */
public class DdlStrategy implements SqlStrategy {
    
    private final OperationLogService logService;
    
    public DdlStrategy(OperationLogService logService) {
        this.logService = logService;
    }
    
    @Override
    public SqlResult execute(String sql, Connection connection, Statement statement) throws Exception {
        int affectedRows = statement.executeUpdate(sql);
        
        // 记录日志
        logService.logSuccess("admin", SqlConstants.LOG_MODULE_SQL_EXECUTE, 
            "执行SQL: " + sql.substring(0, Math.min(100, sql.length())) + "，影响行数: " + affectedRows);
        
        return SqlResult.successUpdate(affectedRows, "执行成功");
    }
    
    @Override
    public String getSqlType() {
        return "DDL";
    }
    
    @Override
    public boolean supports(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        String upperSql = sql.toUpperCase().trim();
        return upperSql.startsWith(SqlConstants.SQL_TYPE_CREATE) ||
               upperSql.startsWith(SqlConstants.SQL_TYPE_ALTER) ||
               upperSql.startsWith(SqlConstants.SQL_TYPE_DROP) ||
               upperSql.startsWith(SqlConstants.SQL_TYPE_TRUNCATE) ||
               upperSql.startsWith(SqlConstants.SQL_TYPE_RENAME);
    }
}
