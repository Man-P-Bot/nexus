package com.nexus.nkap.ingestion;

import com.nexus.nkap.common.BusinessException;
import com.nexus.nkap.common.ResultCode;
import com.nexus.nkap.domain.DocumentEntity;
import com.nexus.nkap.domain.DocumentVersionEntity;
import com.nexus.nkap.ingestion.dto.UploadResponse;
import com.nexus.nkap.repository.DocumentRepository;
import com.nexus.nkap.repository.DocumentVersionRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    private static final long MAX_FILE_SIZE = 200L * 1024 * 1024;
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("pdf", "txt", "md");

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final MinioStorageService minioStorage;
    private final IngestionPipeline ingestionPipeline;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    public IngestionService(DocumentRepository documentRepository,
                            DocumentVersionRepository documentVersionRepository,
                            MinioStorageService minioStorage,
                            IngestionPipeline ingestionPipeline,
                            EntityManager entityManager,
                            PlatformTransactionManager transactionManager) {
        this.documentRepository = documentRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.minioStorage = minioStorage;
        this.ingestionPipeline = ingestionPipeline;
        this.entityManager = entityManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public UploadResponse upload(Path tempFile, String filename, String contentType,
                                 UUID tenantId, UUID spaceId) {
        String extension = extractExtension(filename);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "unsupported file type: " + extension + " (MVP supports pdf/txt/md)");
        }

        long size;
        try {
            size = Files.size(tempFile);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "cannot read uploaded file", e);
        }
        if (size > MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "file too large, max " + (MAX_FILE_SIZE / 1024 / 1024) + " MB");
        }

        try {
            String contentHash = sha256(tempFile);

            Optional<DocumentEntity> existing = documentRepository
                    .findByTenantIdAndSpaceIdAndContentHashAndDeletedFalse(tenantId, spaceId, contentHash);
            if (existing.isPresent()) {
                DocumentEntity doc = existing.get();
                UUID versionId = documentVersionRepository
                        .findByDocumentIdAndCurrentTrue(doc.getId())
                        .map(DocumentVersionEntity::getId)
                        .orElse(null);
                log.info("Duplicate upload detected: document={}", doc.getId());
                return new UploadResponse(doc.getId(), versionId, true);
            }

            UUID documentId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();
            String objectKey = minioStorage.buildObjectKey(tenantId, spaceId, documentId, filename);
            minioStorage.putObject(objectKey, tempFile, size, contentType);

            DocumentEntity document = new DocumentEntity();
            document.setId(documentId);
            document.setTenantId(tenantId);
            document.setSpaceId(spaceId);
            document.setTitle(filename);
            document.setSourceType("upload");
            document.setContentHash(contentHash);
            document.setStatus("pending");
            document.setCurrentVersion(1);

            DocumentVersionEntity version = new DocumentVersionEntity();
            version.setId(versionId);
            version.setDocumentId(documentId);
            version.setTenantId(tenantId);
            version.setVersionNo(1);
            version.setContentHash(contentHash);
            version.setObjectKey(objectKey);
            version.setStatus("pending");
            version.setCurrent(true);

            transactionTemplate.executeWithoutResult(status -> {
                entityManager.persist(document);
                entityManager.flush();
                entityManager.clear();
                entityManager.persist(version);
            });

            log.info("Document {} uploaded, version {} stored as {}", documentId, versionId, objectKey);

            // 🚀 同步调用解析管线（生产环境应改为 MQ 异步）
            try {
                log.info("Starting synchronous ingestion pipeline for document {}", documentId);
                ingestionPipeline.process(documentId, versionId);
            } catch (Exception e) {
                log.error("Pipeline failed, but upload succeeded", e);
            }

            return new UploadResponse(documentId, versionId, false);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "failed to process upload", e);
        } catch (RuntimeException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "upload failed: " + e.getMessage(), e);
        }
    }

    private String extractExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx + 1).toLowerCase(Locale.ROOT) : "";
    }

    private String sha256(Path path) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}