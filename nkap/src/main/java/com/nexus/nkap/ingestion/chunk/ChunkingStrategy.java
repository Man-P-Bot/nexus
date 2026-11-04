package com.nexus.nkap.ingestion.chunk;

import com.nexus.nkap.ingestion.parser.ParsedDocument;

import java.util.List;
import java.util.UUID;

/**
 * 切分策略接口（策略模式）。
 */
public interface ChunkingStrategy {

    /**
     * 切分上下文：承载文档元数据，供切分结果继承。
     */
    record ChunkContext(UUID documentId, UUID versionId, UUID tenantId, UUID spaceId,
                        List<UUID> aclTags, String visibility) {
    }

    /**
     * 将解析结果切分为片段计划。
     */
    List<ChunkPlan> chunk(ParsedDocument parsed, ChunkingConfig config, ChunkContext context);
}