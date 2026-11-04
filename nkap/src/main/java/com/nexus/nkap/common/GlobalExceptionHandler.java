package com.nexus.nkap.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;

/**
 * 全局异常处理器。
 *
 * <p>使用 {@code @RestControllerAdvice} 统一捕获异常并转换为 {@link Result}，
 * 确保任何接口的异常响应结构一致（对齐《11 接口设计》§1.2 / §1.3）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理自定义业务异常，返回其自身的错误码与 HTTP 状态。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException ex) {
        ResultCode resultCode = ex.getResultCode();
        log.warn("Business exception: code={}, message={}", resultCode.getCode(), ex.getMessage());
        return ResponseEntity.status(resultCode.getHttpStatus())
                .body(Result.failure(resultCode, ex.getMessage()));
    }

    /**
     * 处理 WebFlux 请求参数/校验类异常（HTTP 400）。
     */
    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<Result<Void>> handleBadRequest(ServerWebInputException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(ResultCode.BAD_REQUEST.getHttpStatus())
                .body(Result.failure(ResultCode.BAD_REQUEST));
    }

    /**
     * 兜底处理未捕获的系统异常（HTTP 500）。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(ResultCode.INTERNAL_ERROR.getHttpStatus())
                .body(Result.failure(ResultCode.INTERNAL_ERROR));
    }
}
