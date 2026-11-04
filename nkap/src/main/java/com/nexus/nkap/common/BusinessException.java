package com.nexus.nkap.common;

/**
 * 自定义业务异常。
 *
 * <p>携带 {@link ResultCode}，由 {@link GlobalExceptionHandler} 统一转换为
 * {@link Result} 响应。业务层校验失败时应抛出本异常而非返回错误码。
 */
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public BusinessException(ResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }
}
