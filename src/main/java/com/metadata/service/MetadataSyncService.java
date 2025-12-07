package com.metadata.service;

import java.sql.Connection;

/**
 * 元数据同步服务接口
 * 负责将数据库表结构、字段和外键同步到元数据系统
 */
public interface MetadataSyncService {
    
    /**
     * 从 CREATE TABLE 或 ALTER TABLE 语句中提取表名
     */
    String extractTableName(String sql);
    
    /**
     * 同步数据库表字段到元数据系统
     */
    void syncTableFields(String tableName, Connection connection) throws Exception;
    
    /**
     * 工具方法：转换为表名（下划线）
     */
    String convertToTableName(String code);
    
    /**
     * 同步数据库表外键到元数据关联关系系统（内部方法）
     * @return 创建的关联关系记录数
     */
    int syncTableForeignKeys(String tableName, Connection connection) throws Exception;
}