package com.nexus.nkap.repository;

import com.nexus.nkap.domain.DocumentVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 文档版本数据访问层（document_version 表）。
 */
@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersionEntity, UUID> {

    /**
     * 取文档当前生效版本（is_current = true）。
     */
    Optional<DocumentVersionEntity> findByDocumentIdAndCurrentTrue(UUID documentId);

    /**
     * 按文档列出全部版本，按版本号升序。
     */
    List<DocumentVersionEntity> findByDocumentIdOrderByVersionNoAsc(UUID documentId);

    /**
     * 按文档列出当前生效版本（兼容历史数据无标记的情况，取版本号最大）。
     */
    Optional<DocumentVersionEntity> findFirstByDocumentIdOrderByVersionNoDesc(UUID documentId);

    /**
     * 按文档 + 版本号查询。
     */
    Optional<DocumentVersionEntity> findByDocumentIdAndVersionNo(UUID documentId, Integer versionNo);

    /**
     * 按文档 + 版本号判断是否存在。
     */
    boolean existsByDocumentIdAndVersionNo(UUID documentId, Integer versionNo);
}
