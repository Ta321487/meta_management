package com.metadata.service.dto;

import java.util.List;

/**
 * 多条SQL执行结果DTO
 */
public class SqlMultipleResult {
    private boolean success;
    private String message;
    private List<SqlResult> results;
    private int totalCount;
    private int successCount;
    private int failCount;
    
    // 默认构造器
    public SqlMultipleResult() {
    }
    
    // 完整构造器
    public SqlMultipleResult(boolean success, String message, List<SqlResult> results, 
                            int totalCount, int successCount, int failCount) {
        this.success = success;
        this.message = message;
        this.results = results;
        this.totalCount = totalCount;
        this.successCount = successCount;
        this.failCount = failCount;
    }
    
    // 静态工厂方法
    public static SqlMultipleResult build(List<SqlResult> results) {
        SqlMultipleResult multipleResult = new SqlMultipleResult();
        multipleResult.setResults(results);
        multipleResult.setTotalCount(results != null ? results.size() : 0);
        
        if (results != null) {
            int successCount = 0;
            for (SqlResult result : results) {
                if (result.isSuccess()) {
                    successCount++;
                }
            }
            multipleResult.setSuccessCount(successCount);
            multipleResult.setFailCount(results.size() - successCount);
            multipleResult.setSuccess(multipleResult.getFailCount() == 0);
        }
        
        multipleResult.setMessage(String.format("共执行%d条SQL，成功%d条，失败%d条", 
            multipleResult.getTotalCount(), multipleResult.getSuccessCount(), multipleResult.getFailCount()));
        
        return multipleResult;
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
    
    public List<SqlResult> getResults() {
        return results;
    }
    
    public void setResults(List<SqlResult> results) {
        this.results = results;
    }
    
    public int getTotalCount() {
        return totalCount;
    }
    
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
    
    public int getSuccessCount() {
        return successCount;
    }
    
    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }
    
    public int getFailCount() {
        return failCount;
    }
    
    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }
}
