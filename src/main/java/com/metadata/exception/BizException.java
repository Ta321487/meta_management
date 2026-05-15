package com.metadata.exception;

import com.metadata.common.codes.AppErrorCodes;

/**
 * 可携带应用业务码的异常，由 {@link GlobalExceptionHandler} 转为统一 {@link com.metadata.common.Result}。
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static BizException of(int code, String message) {
        return new BizException(code, message);
    }

    public static BizException of(int code, String message, Throwable cause) {
        return new BizException(code, message, cause);
    }

    public static BizException badRequest(String message) {
        return new BizException(AppErrorCodes.COMMON_BAD_REQUEST, message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(AppErrorCodes.AUTH_SESSION_REQUIRED, message);
    }

    /** 通用内部错误（未分类异常、代码生成失败等） */
    public static BizException internal(String message) {
        return new BizException(AppErrorCodes.SYSTEM_INTERNAL, message);
    }

    public static BizException internal(String message, Throwable cause) {
        return new BizException(AppErrorCodes.SYSTEM_INTERNAL, message, cause);
    }
}
