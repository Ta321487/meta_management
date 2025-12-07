package com.metadata.service.strategy;

import com.metadata.service.OperationLogService;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL类型策略工厂
 * 管理不同类型SQL语句的执行策略
 */
@Component
public class SqlTypeStrategyFactory {
    
    private final List<SqlStrategy> strategies = new ArrayList<>();
    
    /**
     * 构造函数，初始化所有策略
     */
    public SqlTypeStrategyFactory(DataSource dataSource, OperationLogService logService) {
        // 初始化各种SQL策略
        strategies.add(new SelectStrategy(dataSource, logService));
        strategies.add(new DmlStrategy(logService));
        strategies.add(new DdlStrategy(logService));
    }
    
    /**
     * 根据SQL语句获取对应的执行策略
     * @param sql SQL语句
     * @return 对应的SQL执行策略，如果没有找到则返回null
     */
    public SqlStrategy getStrategy(String sql) {
        for (SqlStrategy strategy : strategies) {
            if (strategy.supports(sql)) {
                return strategy;
            }
        }
        return null;
    }
    
    /**
     * 添加新的SQL执行策略
     * @param strategy SQL执行策略
     */
    public void addStrategy(SqlStrategy strategy) {
        strategies.add(strategy);
    }
    
    /**
     * 获取所有支持的SQL执行策略
     * @return SQL执行策略列表
     */
    public List<SqlStrategy> getAllStrategies() {
        return new ArrayList<>(strategies);
    }
}
