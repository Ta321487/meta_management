package com.metadata.common;

import lombok.Data;

/**
 * 删除约束请求类
 */
@Data
public class DeleteConstraintRequest {
    /**
     * 字段ID
     */
    private Long id;
    /**
     * 约束名称
     */
    private String constraintName;
    /**
     * 表名
     */
    private String tableName;
    /**
     * 表编码
     */
    private String tableCode;
    /**
     * 字段名
     */
    private String fieldName;
}