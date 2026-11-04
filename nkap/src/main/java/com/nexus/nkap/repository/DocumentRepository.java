package com.nexus.nkap.repository;

import com.nexus.nkap.domain.DocumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 文档数据访问层（document 表）。
 */
@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {

    /**
     * 按空间列出未删除文档（分页）。
     */
    Page<DocumentEntity> findByTenantIdAndSpaceIdAndDeletedFalse(
            UUID tenantId, UUID spaceId, Pageable pageable);

    /**
     * 按内容哈希查询文档（去重依据，跨空间需业务侧校验）。
     */
    Optional<DocumentEntity> findByTenantIdAndSpaceIdAndContentHashAndDeletedFalse(
            UUID tenantId, UUID spaceId, String contentHash);

    /**
     * 按状态批量扫描待处理文档（如 pending/parsing/indexing）。
     */
    List<DocumentEntity> findByTenantIdAndStatusInAndDeletedFalse(UUID tenantId, List<String> statuses);

    /**
     * 查询指定文档（未删除）。
     */
    Optional<DocumentEntity> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    /**
     * 统计某空间下的文档数量。
     */
    long countByTenantIdAndSpaceIdAndDeletedFalse(UUID tenantId, UUID spaceId);
}
