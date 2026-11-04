package com.nexus.nkap.common;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 统一 API 响应结构。
 *
 * <p>严格对齐《11 接口设计》§1.2：
 * <pre>
 * { "code": 0, "message": "ok", "data": {}, "trace_id": "trc_9f8a7b6c5d4e", "request_id": "req_01HZX9K2" }
 * </pre>
 *
 * <p>{@code trace_id} / {@code request_id} 使用 snake_case 输出，与文档一致；
 * 字段值优先取自 {@link TraceContext}（MDC），缺失时自动生成。
 *
 * @param <T> 业务数据类型
 */
public class Result<T> {

    /** 业务错误码，0 表示成功 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    /** 链路追踪 ID（JSON 字段名 trace_id） */
    @JsonProperty("trace_id")
    private String traceId;

    /** 请求 ID（JSON 字段名 request_id） */
    @JsonProperty("request_id")
    private String requestId;

    public Result() {
    }

    public Result(int code, String message, T data, String traceId, String requestId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = traceId;
        this.requestId = requestId;
    }

    /**
     * 成功响应（无数据）。
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 成功响应（带数据）。
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(),
                ResultCode.SUCCESS.getMessage(), data,
                TraceContext.traceId(), TraceContext.requestId());
    }

    /**
     * 失败响应（使用枚举默认消息）。
     */
    public static <T> Result<T> failure(ResultCode resultCode) {
        return failure(resultCode, resultCode.getMessage());
    }

    /**
     * 失败响应（自定义消息）。
     */
    public static <T> Result<T> failure(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null,
                TraceContext.traceId(), TraceContext.requestId());
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
