package com.nexus.nkap.modules.retrieval;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class RetrievalService {

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String SILICONFLOW_API_KEY = System.getenv("SILICONFLOW_API_KEY");
    private static final String EMBEDDING_URL = "https://api.siliconflow.cn/v1/embeddings";

    public RetrievalService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RetrievalResult> search(String query, UUID tenantId, UUID spaceId, int limit) {
        if (limit <= 0) limit = 5;

        String sql = """
                SELECT id, document_id, heading_path, text
                FROM chunk
                WHERE tenant_id = CAST(? AS uuid)
                  AND space_id = CAST(? AS uuid)
                  AND status = 'active'
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT ?
                """;

        // 🚀 调用真实 API 获取查询向量
        float[] queryVector = getRealEmbedding(query);
        String vectorString = IntStream.range(0, queryVector.length)
                .mapToObj(i -> String.valueOf(queryVector[i]))
                .collect(Collectors.joining(",", "[", "]"));

        return jdbcTemplate.query(sql, (rs, rowNum) -> new RetrievalResult(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("document_id"),
                rs.getString("heading_path"),
                rs.getString("text")
        ), tenantId.toString(), spaceId.toString(), vectorString, limit);
    }

    private float[] getRealEmbedding(String text) {
        try {
            Map<String, Object> request = Map.of(
                    "model", "BAAI/bge-m3",
                    "input", text
            );
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(SILICONFLOW_API_KEY);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            org.springframework.http.HttpEntity<Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(request, headers);
            org.springframework.http.ResponseEntity<Map> response = restTemplate.postForEntity(EMBEDDING_URL, entity, Map.class);

            Map<String, Object> data = (Map<String, Object>) ((List) response.getBody().get("data")).get(0);
            List<Double> embeddingList = (List<Double>) data.get("embedding");

            float[] vector = new float[embeddingList.size()];
            for (int i = 0; i < embeddingList.size(); i++) {
                vector[i] = embeddingList.get(i).floatValue();
            }
            return vector;
        } catch (Exception e) {
            throw new RuntimeException("Embedding API failed", e);
        }
    }
}