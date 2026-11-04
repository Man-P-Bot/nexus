package com.nexus.nkap.ingestion;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class MinioStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    private final MinioClient minioClient;
    private final String bucket;

    public MinioStorageService(MinioClient minioClient, @Value("${minio.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    @PostConstruct
    public void ensureBucket() {
        int retries = 10;
        while (retries > 0) {
            try {
                boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if (!found) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("MinIO bucket '{}' created", bucket);
                } else {
                    log.info("MinIO bucket '{}' already exists", bucket);
                }
                return;
            } catch (Exception e) {
                retries--;
                log.warn("MinIO not ready, retrying in 2s... ({} retries left)", retries);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
            }
        }
        log.error("Failed to ensure MinIO bucket after retries, application may not work properly.");
    }

    public String buildObjectKey(UUID tenantId, UUID spaceId, UUID documentId, String filename) {
        return String.format("tenant/%s/space/%s/doc/%s/raw/%s", tenantId, spaceId, documentId, filename);
    }

    public void putObject(String objectKey, Path file, long size, String contentType) {
        String ct = (contentType == null || contentType.isBlank()) ? "application/octet-stream" : contentType;
        try (InputStream in = Files.newInputStream(file)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(in, size, -1)
                    .contentType(ct)
                    .build());
            log.info("MinIO object written: {} ({})", objectKey, ct);
        } catch (Exception e) {
            throw new IllegalStateException("MinIO upload failed: " + objectKey, e);
        }
    }

    /**
     * 从 MinIO 下载对象到本地临时文件。
     */
    public Path downloadToTemp(String objectKey, String suffix) {
        Path tempFile;
        try {
            tempFile = Files.createTempFile("nkap-download-", "." + suffix);
        } catch (IOException e) {
            throw new IllegalStateException("failed to create temp file for: " + objectKey, e);
        }
        try (InputStream in = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .build());
             OutputStream out = Files.newOutputStream(tempFile)) {
            in.transferTo(out);
        } catch (Exception e) {
            deleteQuietly(tempFile);
            throw new IllegalStateException("MinIO download failed: " + objectKey, e);
        }
        return tempFile;
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}