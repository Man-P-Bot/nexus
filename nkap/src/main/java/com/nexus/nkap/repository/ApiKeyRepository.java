package com.nexus.nkap.repository;

import com.nexus.nkap.domain.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * API 密钥数据访问层（api_key 表）。
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID> {

    /**
     * 按租户列出密钥（可选状态过滤）。
     */
    List<ApiKeyEntity> findByTenantId(UUID tenantId);

    /**
     * 按租户 + 状态列出密钥。
     */
    List<ApiKeyEntity> findByTenantIdAndStatus(UUID tenantId, String status);

    /**
     * 按明文前缀 + 租户查询。
     */
    Optional<ApiKeyEntity> findByTenantIdAndKeyPrefix(UUID tenantId, String keyPrefix);

    /**
     * 按哈希值查询（校验 API Key 时使用，哈希不可逆，仅比对）。
     */
    Optional<ApiKeyEntity> findByKeyHash(String keyHash);

    /**
     * 按哈希值判断是否存在。
     */
    boolean existsByKeyHash(String keyHash);

    /**
     * 按租户 + 前缀判断是否存在（创建前唯一性校验）。
     */
    boolean existsByTenantIdAndKeyPrefix(UUID tenantId, String keyPrefix);
}
