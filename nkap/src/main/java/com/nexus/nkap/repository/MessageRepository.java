package com.nexus.nkap.repository;

import com.nexus.nkap.domain.MessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 消息数据访问层（message 表）。
 */
@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {

    /**
     * 取会话最近 N 条消息（按时间正序返回，由调用方截取）。
     */
    List<MessageEntity> findByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    /**
     * 取会话全部消息（按时间升序）。
     */
    List<MessageEntity> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    /**
     * 会话消息分页查询。
     */
    Page<MessageEntity> findByConversationId(UUID conversationId, Pageable pageable);

    /**
     * 按会话统计消息数。
     */
    long countByConversationId(UUID conversationId);

    /**
     * 按 trace 查全链路（Langfuse 对账）。
     */
    List<MessageEntity> findByTenantIdAndTraceId(UUID tenantId, String traceId);
}
