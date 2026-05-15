package com.metadata.exception;

import com.metadata.common.Result;
import com.metadata.common.codes.ApiMessages;
import com.metadata.common.codes.AppErrorCodes;
import com.metadata.service.exception.CodeGenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 将异常统一转为 {@link Result}，避免控制器分散 try/catch。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<?> handleBizException(BizException ex) {
        return Result.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(CodeGenException.class)
    public Result<?> handleCodeGenException(CodeGenException ex) {
        return Result.error(AppErrorCodes.CODEGEN_FAILED, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgument(IllegalArgumentException ex) {
        return Result.error(AppErrorCodes.COMMON_BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(m -> m != null && !m.isEmpty())
                .findFirst()
                .orElse(ApiMessages.BAD_REQUEST);
        return Result.error(AppErrorCodes.COMMON_BAD_REQUEST, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolation(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return Result.error(AppErrorCodes.COMMON_BAD_REQUEST, msg.isEmpty() ? ApiMessages.BAD_REQUEST : msg);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingParam(MissingServletRequestParameterException ex) {
        return Result.error(AppErrorCodes.COMMON_BAD_REQUEST, "缺少参数: " + ex.getParameterName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleNotReadable(HttpMessageNotReadableException ex) {
        return Result.error(AppErrorCodes.COMMON_BAD_REQUEST, ApiMessages.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<?> handleDataIntegrity(DataIntegrityViolationException ex) {
        Throwable root = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause() : ex;
        String msg = root.getMessage() != null ? root.getMessage() : "";
        if (msg.contains("Duplicate entry") || msg.contains("uk_table_field")) {
            return Result.error(AppErrorCodes.FIELD_CODE_DUPLICATE, ApiMessages.FIELD_CODE_EXISTS);
        }
        return Result.error(AppErrorCodes.COMMON_DB_CONSTRAINT, msg.isEmpty() ? ApiMessages.REQUEST_FAILED : msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        String message = ex.getMessage();
        if (message == null || message.isEmpty()) {
            message = ApiMessages.INTERNAL_ERROR;
        }
        return Result.error(AppErrorCodes.SYSTEM_INTERNAL, message);
    }
}
