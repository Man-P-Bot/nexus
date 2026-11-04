package com.nexus.nkap.ingestion.embedding;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mock Embedding 实现（开发期方案 B 变体）。
 *
 * <p>无 GPU / 无云厂商 API Key 时，用于端到端跑通管线。
 * 返回基于文本哈希的确定性伪随机向量（同一文本恒等向量），便于测试复现；
 * 维度固定为 {@link EmbeddingClient#EMBEDDING_DIM}（1024）。
 *
 * <p>正式实现（TEI / 云端 API）接入后，本类可被 {@code @Profile} 或配置开关替换。
 */
@Service
public class MockEmbeddingService implements EmbeddingClient {

    @Override
    public float[] embed(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> vectors = new ArrayList<>(texts.size());
        for (String text : texts) {
            // 以文本哈希为种子，保证确定性与可复现
            Random random = new Random(text.hashCode());
            float[] vector = new float[EMBEDDING_DIM];
            for (int i = 0; i < vector.length; i++) {
                vector[i] = random.nextFloat();
            }
            vectors.add(vector);
        }
        return vectors;
    }
}
