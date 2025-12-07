package com.metadata.service.dto;

import java.util.List;
import java.util.Map;

/**
 * SQL执行结果DTO
 */
public class SqlResult {
    private boolean success;
    private String message;
    private List<Map<String, Object>> data;
    private int affectedRows;
    private int rowCount;
    private String error;
    private String originalError;
    private String sql;
    
    // 默认构造器
    public SqlResult() {
    }
    
    // 完整构造器
    public SqlResult(boolean success, String message, List<Map<String, Object>> data, int affectedRows, int rowCount, 
                     String error, String originalError, String sql) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.affectedRows = affectedRows;
        this.rowCount = rowCount;
        this.error = error;
        this.originalError = originalError;
        this.sql = sql;
    }
    
    // 成功构造器（查询）
    public static SqlResult successQuery(List<Map<String, Object>> data, String message) {
        SqlResult result = new SqlResult();
        result.setSuccess(true);
        result.setMessage(message);
        result.setData(data);
        result.setRowCount(data != null ? data.size() : 0);
        return result;
    }
    
    // 成功构造器（更新）
    public static SqlResult successUpdate(int affectedRows, String message) {
        SqlResult result = new SqlResult();
        result.setSuccess(true);
        result.setMessage(message);
        result.setAffectedRows(affectedRows);
        return result;
    }
    
    // 失败构造器
    public static SqlResult failure(String message, String error, String originalError) {
        SqlResult result = new SqlResult();
        result.setSuccess(false);
        result.setMessage(message);
        result.setError(error);
        result.setOriginalError(originalError);
        return result;
    }
    
    // getter和setter方法
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public List<Map<String, Object>> getData() {
        return data;
    }
    
    public void setData(List<Map<String, Object>> data) {
        this.data = data;
    }
    
    public int getAffectedRows() {
        return affectedRows;
    }
    
    public void setAffectedRows(int affectedRows) {
        this.affectedRows = affectedRows;
    }
    
    public int getRowCount() {
        return rowCount;
    }
    
    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getOriginalError() {
        return originalError;
    }
    
    public void setOriginalError(String originalError) {
        this.originalError = originalError;
    }
    
    public String getSql() {
        return sql;
    }
    
    public void setSql(String sql) {
        this.sql = sql;
    }
}
