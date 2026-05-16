package com.metadata.common;

import lombok.Data;

/**
 * SQL执行请求类
 */
@Data
public class SqlExecuteRequest {
    /**
     * 要执行的SQL语句
     */
    private String sql;

    /**
     * 目标 MySQL 库名（catalog），与 JDBC 连接库名一致，如 demo_erp。
     * SQL 执行页必填；为空时回退到应用主数据源（元数据库 metadata_db）。
     */
    private String targetCatalog;
}