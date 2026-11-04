package com.nexus.nkap.repository;

import com.nexus.nkap.domain.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 会话数据访问层（conversation 表）。
 */
@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {

    /**
     * 用户会话列表（按最近更新倒序，未删除）。
     */
    List<ConversationEntity> findByTenantIdAndUserIdAndDeletedFalseOrderByUpdatedAtDesc(
            UUID tenantId, UUID userId);

    /**
     * 按空间取活跃会话（未删除）。
     */
    List<ConversationEntity> findByTenantIdAndSpaceIdAndStatusAndDeletedFalse(
            UUID tenantId, UUID spaceId, String status);

    /**
     * 查询指定会话（未删除）。
     */
    Optional<ConversationEntity> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    /**
     * 按用户统计活跃会话数。
     */
    long countByTenantIdAndUserIdAndDeletedFalse(UUID tenantId, UUID userId);
}
