package com.nexus.nkap.ingestion.embedding;

import java.util.List;

/**
 * Embedding 客户端抽象。
 *
 * <p>通过接口隔离具体实现：MVP 使用 {@link MockEmbeddingService}，
 * 后续切换到 TEI 本地服务或云端 API 时，仅需替换实现（或改一行配置），
 * 上层管线无需改动。
 */
public interface EmbeddingClient {

    /** bge-m3 向量维度（§2.5） */
    int EMBEDDING_DIM = 1024;

    /**
     * 单条文本向量化。
     */
    float[] embed(String text);

    /**
     * 批量文本向量化（§2.5：batch_size=32）。
     */
    List<float[]> embedBatch(List<String> texts);
}
