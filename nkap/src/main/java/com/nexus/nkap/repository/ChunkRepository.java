package com.nexus.nkap.repository;

import com.nexus.nkap.domain.ChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChunkRepository extends JpaRepository<ChunkEntity, UUID> {

    /**
     * 批量保存切片。
     */
    default void saveAllChunks(List<ChunkEntity> chunks) {
        saveAll(chunks);
    }

    /**
     * 按文档逻辑删除 active 切片（重新索引前清理）。
     */
    @Modifying
    @Query("DELETE FROM ChunkEntity c WHERE c.documentId = :documentId AND c.status = :status")
    void deleteByDocumentIdAndStatus(@Param("documentId") UUID documentId, @Param("status") String status);

    List<ChunkEntity> findByDocumentIdAndStatusOrderBySeqNoAsc(UUID documentId, String status);

    long countByDocumentIdAndStatus(UUID documentId, String status);
}