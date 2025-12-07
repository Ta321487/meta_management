package com.metadata.service.strategy;

import com.metadata.service.dto.SqlResult;

import java.sql.Connection;
import java.sql.Statement;

/**
 * SQL执行策略接口
 * 定义不同类型SQL语句的执行策略
 */
public interface SqlStrategy {
    
    /**
     * 执行SQL语句
     * @param sql SQL语句
     * @param connection 数据库连接
     * @param statement Statement对象
     * @return SQL执行结果
     * @throws Exception 执行过程中可能抛出的异常
     */
    SqlResult execute(String sql, Connection connection, Statement statement) throws Exception;
    
    /**
     * 获取当前策略支持的SQL类型
     * @return SQL类型
     */
    String getSqlType();
    
    /**
     * 判断当前策略是否支持给定的SQL语句
     * @param sql SQL语句
     * @return 是否支持
     */
    boolean supports(String sql);
}
