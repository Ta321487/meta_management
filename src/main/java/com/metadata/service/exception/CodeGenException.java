package com.metadata.service.exception;

/**
 * 代码生成异常类
 */
public class CodeGenException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 错误代码
     */
    private String errorCode;
    
    /**
     * 构造方法
     * @param message 错误信息
     */
    public CodeGenException(String message) {
        super(message);
    }
    
    /**
     * 构造方法
     * @param message 错误信息
     * @param cause 异常原因
     */
    public CodeGenException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 构造方法
     * @param errorCode 错误代码
     * @param message 错误信息
     */
    public CodeGenException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * 构造方法
     * @param errorCode 错误代码
     * @param message 错误信息
     * @param cause 异常原因
     */
    public CodeGenException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * 获取错误代码
     * @return 错误代码
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * 设置错误代码
     * @param errorCode 错误代码
     */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}