package com.nexus.nkap.ingestion;

import com.nexus.nkap.common.BusinessException;
import com.nexus.nkap.common.ResultCode;
import com.nexus.nkap.domain.ChunkEntity;
import com.nexus.nkap.domain.DocumentEntity;
import com.nexus.nkap.domain.DocumentVersionEntity;
import com.nexus.nkap.ingestion.chunk.ChunkPlan;
import com.nexus.nkap.ingestion.chunk.ChunkingService;
import com.nexus.nkap.ingestion.parser.DocumentParser;
import com.nexus.nkap.ingestion.parser.DocumentParserFactory;
import com.nexus.nkap.ingestion.parser.ParsedDocument;
import com.nexus.nkap.repository.ChunkRepository;
import com.nexus.nkap.repository.DocumentRepository;
import com.nexus.nkap.repository.DocumentVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class IngestionPipeline {

    private static final Logger log = LoggerFactory.getLogger(IngestionPipeline.class);
    private static final String STATUS_READY = "ready";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_ACTIVE = "active";

    private final MinioStorageService minioStorage;
    private final DocumentParserFactory parserFactory;
    private final ChunkingService chunkingService;
    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final ChunkRepository chunkRepository;
    private final TransactionTemplate transactionTemplate;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String SILICONFLOW_API_KEY = System.getenv("SILICONFLOW_API_KEY");
    private static final String EMBEDDING_URL = "https://api.siliconflow.cn/v1/embeddings";

    public IngestionPipeline(MinioStorageService minioStorage,
                             DocumentParserFactory parserFactory,
                             ChunkingService chunkingService,
                             DocumentRepository documentRepository,
                             DocumentVersionRepository documentVersionRepository,
                             ChunkRepository chunkRepository,
                             PlatformTransactionManager transactionManager) {
        this.minioStorage = minioStorage;
        this.parserFactory = parserFactory;
        this.chunkingService = chunkingService;
        this.documentRepository = documentRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.chunkRepository = chunkRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public int process(UUID documentId, UUID versionId) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "document not found: " + documentId));
        DocumentVersionEntity version = documentVersionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "version not found: " + versionId));

        String extension = extensionOf(version.getObjectKey());
        Path tempFile = null;
        try {
            tempFile = minioStorage.downloadToTemp(version.getObjectKey(), extension);
            DocumentParser parser = parserFactory.getParser(extension);
            ParsedDocument parsed = parser.parse(document, tempFile);
            List<ChunkPlan> plans = chunkingService.chunk(document, versionId, parsed);

            List<ChunkEntity> chunks = new ArrayList<>();
            for (ChunkPlan plan : plans) {
                ChunkEntity chunk = toChunkEntity(document, versionId, plan);
                // 🚀 真实 API 请求
                float[] vector = getRealEmbedding(plan.text());
                String vectorString = IntStream.range(0, vector.length)
                        .mapToObj(i -> String.valueOf(vector[i]))
                        .collect(Collectors.joining(",", "[", "]"));
                chunk.setEmbeddingString(vectorString);
                chunks.add(chunk);
            }

            return transactionTemplate.execute(status -> {
                chunkRepository.deleteByDocumentIdAndStatus(documentId, STATUS_ACTIVE);
                chunkRepository.saveAllChunks(chunks);
                document.setStatus(STATUS_READY);
                version.setStatus(STATUS_READY);
                documentRepository.save(document);
                documentVersionRepository.save(version);
                log.info("Ingestion pipeline completed with REAL embeddings for document={}, chunks={}", documentId, chunks.size());
                return chunks.size();
            });
        } catch (BusinessException e) {
            markFailed(documentId, versionId, e.getMessage());
            throw e;
        } catch (Exception e) {
            markFailed(documentId, versionId, e.getMessage());
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "ingestion pipeline failed", e);
        } finally {
            deleteQuietly(tempFile);
        }
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
            log.error("Failed to call SiliconFlow Embedding API", e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "Embedding API failed");
        }
    }

    private ChunkEntity toChunkEntity(DocumentEntity document, UUID versionId, ChunkPlan plan) {
        ChunkEntity chunk = new ChunkEntity();
        chunk.setId(plan.chunkId());
        chunk.setTenantId(plan.tenantId());
        chunk.setSpaceId(plan.spaceId());
        chunk.setDocumentId(plan.documentId());
        chunk.setVersionId(versionId);
        chunk.setSeqNo(plan.seqNo());
        chunk.setHeadingPath(plan.headingPath());
        chunk.setText(plan.text());
        chunk.setTokenLen(plan.tokenLen());
        chunk.setAclTags(plan.aclTags());
        chunk.setVisibility(plan.visibility());
        chunk.setStatus(STATUS_ACTIVE);
        return chunk;
    }

    private void markFailed(UUID documentId, UUID versionId, String error) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                documentRepository.findById(documentId).ifPresent(doc -> {
                    doc.setStatus(STATUS_FAILED);
                    documentRepository.save(doc);
                });
                documentVersionRepository.findById(versionId).ifPresent(ver -> {
                    ver.setStatus(STATUS_FAILED);
                    documentVersionRepository.save(ver);
                });
            });
            log.warn("Ingestion pipeline failed for document={}, version={}: {}", documentId, versionId, error);
        } catch (Exception ex) {
            log.error("Failed to mark document {} as failed", documentId, ex);
        }
    }

    private String extensionOf(String objectKey) {
        int idx = objectKey.lastIndexOf('.');
        return idx >= 0 ? objectKey.substring(idx + 1).toLowerCase(Locale.ROOT) : "";
    }

    private void deleteQuietly(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
            }
        }
    }
}