package com.nexus.nkap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 租户实体（对应表 {@code tenant}）。
 *
 * <p>多租户隔离单元，企业级数据边界。根表，无 {@code tenant_id}。
 * 依据：《10-数据库设计》v0.3 §3.1。
 */
@Entity
@Table(name = "tenant",
        uniqueConstraints = @UniqueConstraint(name = "uk_tenant_code", columnNames = "code"))
public class TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    /** 租户编码 */
    @Column(name = "code", nullable = false, length = 32)
    private String code;

    /** 租户名称 */
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /** active / suspended / deleted */
    @Column(name = "status", nullable = false, length = 16)
    private String status = "active";

    /** 套餐（影响配额） */
    @Column(name = "plan", nullable = false, length = 16)
    private String plan = "standard";

    /** 文档上限 */
    @Column(name = "max_documents", nullable = false)
    private Integer maxDocuments = 10000;

    /** 检索 QPS 上限 */
    @Column(name = "max_qps", nullable = false)
    private Integer maxQps = 50;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public Integer getMaxDocuments() {
        return maxDocuments;
    }

    public void setMaxDocuments(Integer maxDocuments) {
        this.maxDocuments = maxDocuments;
    }

    public Integer getMaxQps() {
        return maxQps;
    }

    public void setMaxQps(Integer maxQps) {
        this.maxQps = maxQps;
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
}
