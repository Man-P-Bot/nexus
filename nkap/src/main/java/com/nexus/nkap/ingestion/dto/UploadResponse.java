package com.nexus.nkap.ingestion.dto;

import java.util.UUID;

/**
 * 上传接口响应。
 *
 * @param documentId 文档 ID
 * @param versionId  版本 ID（重复上传时为当前版本 ID，可能为 null）
 * @param duplicated 是否命中重复（同 space 内 content_hash 相同）
 */
public record UploadResponse(UUID documentId, UUID versionId, boolean duplicated) {
}
