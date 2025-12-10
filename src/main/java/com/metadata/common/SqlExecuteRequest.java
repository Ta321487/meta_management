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
}