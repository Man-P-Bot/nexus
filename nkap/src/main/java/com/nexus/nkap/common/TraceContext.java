package com.nexus.nkap.common;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * 请求追踪上下文工具。
 *
 * <p>从 SLF4J {@link MDC} 读取 {@code trace_id} / {@code request_id}；
 * 若缺失则生成新的标识，保证每个响应都带有可追溯字段。
 *
 * <p>对应《11 接口设计》§1.4「每请求生成 request_id，写入 MDC 并透传」。
 */
public final class TraceContext {

    /** MDC 键：链路追踪 ID */
    public static final String TRACE_ID = "trace_id";

    /** MDC 键：请求 ID */
    public static final String REQUEST_ID = "request_id";

    private static final String TRACE_PREFIX = "trc";
    private static final String REQUEST_PREFIX = "req";

    private TraceContext() {
    }

    /**
     * 获取链路追踪 ID；缺失时生成 {@code trc_<uuid>}。
     */
    public static String traceId() {
        return getOrGenerate(TRACE_ID, TRACE_PREFIX);
    }

    /**
     * 获取请求 ID；缺失时生成 {@code req_<uuid>}。
     */
    public static String requestId() {
        return getOrGenerate(REQUEST_ID, REQUEST_PREFIX);
    }

    private static String getOrGenerate(String key, String prefix) {
        String value = MDC.get(key);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
