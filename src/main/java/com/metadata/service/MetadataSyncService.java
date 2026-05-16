package com.metadata.service;

import java.sql.Connection;
import java.util.Map;

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
     * 同步数据库表字段到元数据系统（连接须指向表所在物理库）
     */
    void syncTableFields(String tableName, Connection connection) throws Exception;

    /**
     * 同步字段并登记/更新元数据表（指定业务系统与物理库名）
     */
    void syncTableFields(String tableName, Connection connection, String businessCode, String databaseName) throws Exception;

    /**
     * 仅将物理表中尚未登记到元数据的列写入元数据（不 ALTER 物理表、不更新已有元数据、不删除元数据侧多余字段）。
     *
     * @param tableName  物理表名
     * @param tableCode  逻辑表编码（元数据）
     */
    Map<String, Object> syncMissingFieldsFromPhysical(String tableName, String tableCode, Connection connection,
            String businessCode, String databaseName) throws Exception;

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