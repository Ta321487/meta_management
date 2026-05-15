package com.metadata.common;

import com.metadata.common.codes.ApiMessages;
import com.metadata.common.codes.AppErrorCodes;
import lombok.Data;
import java.io.Serializable;

/**
 * 统一响应结果
 */
@Data
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 响应码：200 成功；其余为应用层四位业务码（见 com.metadata.common.codes.AppErrorCodes）
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(AppErrorCodes.SUCCESS, ApiMessages.OPERATION_SUCCESS, null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(AppErrorCodes.SUCCESS, ApiMessages.OPERATION_SUCCESS, data);
    }

    /**
     * 成功响应（自定义消息和数据）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(AppErrorCodes.SUCCESS, message, data);
    }

    /**
     * 失败响应（默认 9999 系统内部错误码）
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(AppErrorCodes.SYSTEM_INTERNAL, message, null);
    }

    /**
     * 失败响应（自定义错误码）
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
    
    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return AppErrorCodes.isSuccess(code);
    }
    
    /**
     * 判断是否失败
     */
    public boolean isError() {
        return !isSuccess();
    }
}

