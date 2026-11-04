package com.nexus.nkap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chunk", indexes = {
        @Index(name = "idx_chunk_tenant_space_doc_status", columnList = "tenant_id, space_id, document_id, status"),
        @Index(name = "idx_chunk_tenant_space_status", columnList = "tenant_id, space_id, status"),
        @Index(name = "idx_chunk_tenant_space_version", columnList = "tenant_id, space_id, version_id")
})
public class ChunkEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "version_id", nullable = false)
    private UUID versionId;

    @Column(name = "seq_no", nullable = false)
    private Integer seqNo;

    @Column(name = "heading_path", length = 512)
    private String headingPath;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "token_len", nullable = false)
    private Integer tokenLen;

    /**
     * 向量字段（1024维）。
     * 使用 @ColumnTransformer 告诉 Hibernate 写库时自动 CAST 为 vector。
     */
    @Column(name = "embedding", columnDefinition = "vector(1024)")
    @ColumnTransformer(write = "CAST(? AS vector)")
    private String embeddingString;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "acl_tags", columnDefinition = "uuid[]", nullable = false)
    private List<UUID> aclTags;

    @Column(name = "visibility", nullable = false, length = 16)
    private String visibility;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // --- Getters and Setters ---
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getSpaceId() { return spaceId; }
    public void setSpaceId(UUID spaceId) { this.spaceId = spaceId; }
    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
    public UUID getVersionId() { return versionId; }
    public void setVersionId(UUID versionId) { this.versionId = versionId; }
    public Integer getSeqNo() { return seqNo; }
    public void setSeqNo(Integer seqNo) { this.seqNo = seqNo; }
    public String getHeadingPath() { return headingPath; }
    public void setHeadingPath(String headingPath) { this.headingPath = headingPath; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Integer getTokenLen() { return tokenLen; }
    public void setTokenLen(Integer tokenLen) { this.tokenLen = tokenLen; }
    public String getEmbeddingString() { return embeddingString; }
    public void setEmbeddingString(String embeddingString) { this.embeddingString = embeddingString; }
    public List<UUID> getAclTags() { return aclTags; }
    public void setAclTags(List<UUID> aclTags) { this.aclTags = aclTags; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}