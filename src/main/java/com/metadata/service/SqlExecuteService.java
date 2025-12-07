package com.metadata.service;

import java.util.Map;

/**
 * SQL执行服务接口
 */
public interface SqlExecuteService {

    /**
     * 执行SQL语句（对外接口，禁止危险操作）
     * @param sql SQL语句
     * @return 执行结果
     */
    Map<String, Object> executeSql(String sql);
    
    /**
     * 执行SQL语句（支持跳过安全检查）
     * @param sql SQL语句
     * @param skipSafetyCheck 是否跳过安全检查
     * @return 执行结果
     */
    Map<String, Object> executeSql(String sql, boolean skipSafetyCheck);

    /**
     * 执行DROP TABLE语句（仅供内部服务调用）
     * @param tableName 表名
     * @return 执行结果
     */
    Map<String, Object> executeDropTable(String tableName);

    /**
     * 执行多条SQL语句（用分号分隔）
     * @param sqls SQL语句（用分号分隔）
     * @return 执行结果
     */
    Map<String, Object> executeMultipleSql(String sqls);

    /**
     * 同步数据库表外键到元数据关联关系系统（公共方法，供外部调用）
     * @param tableCode 表编码，如果为null则同步所有表
     * @return 同步结果
     */
    Map<String, Object> syncForeignKeys(String tableCode);
}