package com.nexus.nkap.modules.retrieval;

import java.util.UUID;

/**
 * 检索结果（单条命中片段）。
 *
 * @param chunkId     片段 ID
 * @param documentId  所属文档 ID
 * @param headingPath 所属标题路径
 * @param text        片段正文
 */
public record RetrievalResult(UUID chunkId, UUID documentId, String headingPath, String text) {
}
