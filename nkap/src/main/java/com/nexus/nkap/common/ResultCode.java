package com.nexus.nkap.common;

import org.springframework.http.HttpStatus;

/**
 * 统一错误码枚举。
 *
 * <p>严格对齐《11 接口设计》§1.3 错误码规范。
 * 每个枚举同时携带 HTTP 状态码，便于全局异常处理器直接映射响应。
 */
public enum ResultCode {

    /** 成功 */
    SUCCESS(0, "ok", HttpStatus.OK),

    /** 请求参数非法 */
    BAD_REQUEST(40001, "request parameter invalid", HttpStatus.BAD_REQUEST),

    /** 未认证 / 令牌失效 */
    UNAUTHORIZED(40101, "unauthorized or token expired", HttpStatus.UNAUTHORIZED),

    /** API Key 无效或过期 */
    API_KEY_INVALID(40102, "api key invalid or expired", HttpStatus.UNAUTHORIZED),

    /** 无权限 */
    FORBIDDEN(40301, "forbidden", HttpStatus.FORBIDDEN),

    /** 跨租户越权 */
    TENANT_FORBIDDEN(40302, "cross-tenant access denied", HttpStatus.FORBIDDEN),

    /** 需人工审批 */
    APPROVAL_REQUIRED(40303, "manual approval required", HttpStatus.FORBIDDEN),

    /** 资源不存在 */
    NOT_FOUND(40401, "resource not found", HttpStatus.NOT_FOUND),

    /** 资源冲突（如重复上传） */
    CONFLICT(40901, "resource conflict", HttpStatus.CONFLICT),

    /** 版本冲突（乐观锁） */
    VERSION_CONFLICT(40902, "version conflict", HttpStatus.CONFLICT),

    /** 语义校验失败 */
    SEMANTIC_VALIDATION_FAILED(42201, "semantic validation failed", HttpStatus.UNPROCESSABLE_ENTITY),

    /** 文档解析或内容不合规 */
    DOCUMENT_CONTENT_INVALID(42202, "document parse or content invalid", HttpStatus.UNPROCESSABLE_ENTITY),

    /** 触发限流 */
    RATE_LIMITED(42901, "rate limited", HttpStatus.TOO_MANY_REQUESTS),

    /** 配额不足 */
    QUOTA_EXCEEDED(42902, "quota exceeded", HttpStatus.TOO_MANY_REQUESTS),

    /** 服务内部错误 */
    INTERNAL_ERROR(50001, "internal server error", HttpStatus.INTERNAL_SERVER_ERROR),

    /** 依赖服务不可用（LLM / MCP / 向量库） */
    DEPENDENCY_UNAVAILABLE(50301, "dependency service unavailable", HttpStatus.SERVICE_UNAVAILABLE);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ResultCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
