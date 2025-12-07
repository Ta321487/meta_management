package com.metadata.service.strategy;

import com.metadata.service.dto.SqlResult;
import com.metadata.service.constant.SqlConstants;
import com.metadata.service.OperationLogService;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * SELECT语句执行策略
 */
public class SelectStrategy implements SqlStrategy {
    
    private final JdbcTemplate jdbcTemplate;
    private final OperationLogService logService;
    
    public SelectStrategy(DataSource dataSource, OperationLogService logService) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.logService = logService;
    }
    
    @Override
    public SqlResult execute(String sql, Connection connection, Statement statement) throws Exception {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        
        // 记录日志
        logService.logSuccess("admin", SqlConstants.LOG_MODULE_SQL_QUERY, 
            "执行查询SQL: " + sql.substring(0, Math.min(100, sql.length())));
        
        return SqlResult.successQuery(rows, "执行成功");
    }
    
    @Override
    public String getSqlType() {
        return SqlConstants.SQL_TYPE_SELECT;
    }
    
    @Override
    public boolean supports(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        String upperSql = sql.toUpperCase().trim();
        return upperSql.startsWith(SqlConstants.SQL_TYPE_SELECT);
    }
}
