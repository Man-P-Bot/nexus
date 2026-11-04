package com.nexus.nkap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 文档版本实体（对应表 {@code document_version}）。
 *
 * <p>保留文档历史版本，支持回滚与增量更新。
 * 依据：《10-数据库设计》v0.3 §3.4。
 */
@Entity
@Table(name = "document_version",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_docver_no",
                columnNames = {"document_id", "version_no"}),
        indexes = @Index(
                name = "idx_docver_tenant_current",
                columnList = "tenant_id, is_current"))
public class DocumentVersionEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    /** 租户（冗余加速过滤） */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** 版本号（从 1 递增） */
    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    /** MinIO 对象键 */
    @Column(name = "object_key", nullable = false)
    private String objectKey;

    /** 该版本处理状态 */
    @Column(name = "status", nullable = false, length = 24)
    private String status;

    /** 是否当前生效版本（列名 is_current） */
    @Column(name = "is_current", nullable = false)
    private boolean current = false;

    @Column(name = "superseded_at")
    private OffsetDateTime supersededAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public OffsetDateTime getSupersededAt() {
        return supersededAt;
    }

    public void setSupersededAt(OffsetDateTime supersededAt) {
        this.supersededAt = supersededAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
