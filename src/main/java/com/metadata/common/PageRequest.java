package com.metadata.common;

import lombok.Data;
import java.util.Map;

/**
 * 分页请求
 */
@Data
public class PageRequest {
    /**
     * 当前页码，从1开始
     */
    private Integer current = 1;
    
    /**
     * 每页大小
     */
    private Integer size = 10;
    
    /**
     * 排序字段（如：id, createTime）
     */
    private String orderBy;
    
    /**
     * 排序方式（ASC/DESC）
     */
    private String orderDirection = "DESC";
    
    /**
     * 查询条件（Map格式，key为字段名，value为查询值）
     */
    private Map<String, Object> conditions;

    /**
     * 获取偏移量
     */
    public Integer getOffset() {
        if (current == null || current < 1) {
            current = 1;
        }
        if (size == null || size < 1) {
            size = 10;
        }
        return (current - 1) * size;
    }
    
    /**
     * 获取排序SQL片段
     */
    public String getOrderByClause() {
        if (orderBy == null || orderBy.trim().isEmpty()) {
            return "ORDER BY id DESC";
        }
        String direction = "DESC".equalsIgnoreCase(orderDirection) ? "DESC" : "ASC";
        return "ORDER BY " + orderBy + " " + direction;
    }
}

