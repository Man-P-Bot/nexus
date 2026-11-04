package com.nexus.nkap.repository;

import com.nexus.nkap.domain.SpaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 空间数据访问层（space 表）。
 */
@Repository
public interface SpaceRepository extends JpaRepository<SpaceEntity, UUID> {

    /**
     * 按租户列出未删除空间（用户可见空间列表核心查询）。
     */
    List<SpaceEntity> findByTenantIdAndDeletedFalse(UUID tenantId);

    /**
     * 按租户 + 可见性列出空间。
     */
    List<SpaceEntity> findByTenantIdAndVisibilityAndDeletedFalse(UUID tenantId, String visibility);

    /**
     * 查询指定空间（未删除）。
     */
    Optional<SpaceEntity> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    /**
     * 统计某租户下的空间数量。
     */
    long countByTenantIdAndDeletedFalse(UUID tenantId);
}
