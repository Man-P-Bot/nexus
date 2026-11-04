package com.nexus.nkap.ingestion.chunk;

import com.nexus.nkap.domain.DocumentEntity;
import com.nexus.nkap.ingestion.parser.ParsedDocument;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 切分服务：编排 ChunkingStrategy，将解析结果切分为 ChunkPlan。
 */
@Service
public class ChunkingService {

    private static final String DEFAULT_VISIBILITY = "inherit";

    private final ChunkingStrategy chunkingStrategy;

    public ChunkingService(ChunkingStrategy chunkingStrategy) {
        this.chunkingStrategy = chunkingStrategy;
    }

    public List<ChunkPlan> chunk(DocumentEntity document, UUID versionId, ParsedDocument parsed) {
        return chunk(document, versionId, parsed, new ChunkingConfig());
    }

    public List<ChunkPlan> chunk(DocumentEntity document, UUID versionId,
                                 ParsedDocument parsed, ChunkingConfig config) {
        ChunkingStrategy.ChunkContext context = new ChunkingStrategy.ChunkContext(
                document.getId(), versionId, document.getTenantId(), document.getSpaceId(),
                document.getTagIds(), DEFAULT_VISIBILITY);
        return chunkingStrategy.chunk(parsed, config, context);
    }
}