package com.nexus.nkap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文档实体（对应表 {@code document}）。
 *
 * <p>文档元数据主记录，聚合版本与切分。
 * 依据：《10-数据库设计》v0.3 §3.3。
 */
@Entity
@Table(name = "document",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_document_hash",
                columnNames = {"tenant_id", "space_id", "content_hash"}),
        indexes = @Index(
                name = "idx_document_tenant_space_status",
                columnList = "tenant_id, space_id, status"))
public class DocumentEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "title", nullable = false, length = 512)
    private String title;

    /** upload / url / api / ... */
    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(name = "source_url")
    private String sourceUrl;

    /** SHA-256，去重依据 */
    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    /** pending / parsing / indexing / ready / failed / quarantined */
    @Column(name = "status", nullable = false, length = 24)
    private String status = "pending";

    @Column(name = "current_version", nullable = false)
    private Integer currentVersion = 1;

    /** 文档标签（逻辑外键集合，对应 uuid[]） */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tag_ids", nullable = false, columnDefinition = "uuid[]")
    private List<UUID> tagIds = new ArrayList<>();

    /** 上传人（逻辑外键 → user，暂未建 FK） */
    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** 软删除 */
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(UUID spaceId) {
        this.spaceId = spaceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(Integer currentVersion) {
        this.currentVersion = currentVersion;
    }

    public List<UUID> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<UUID> tagIds) {
        this.tagIds = tagIds;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
