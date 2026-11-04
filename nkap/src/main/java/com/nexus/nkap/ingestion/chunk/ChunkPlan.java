package com.nexus.nkap.ingestion.chunk;

import java.util.List;
import java.util.UUID;

/**
 * 切分计划：一个 chunk 的完整描述。
 *
 * <p>切分阶段产出后，可直接映射为 {@code ChunkEntity} 落库。
 * 元数据继承文档的 {@code tenant_id} / {@code space_id} / {@code acl_tags} / {@code visibility}（§2.4）。
 *
 * @param chunkId    片段主键（切分阶段生成，落库时使用）
 * @param documentId 所属文档 ID
 * @param versionId  所属版本 ID
 * @param tenantId   租户 ID
 * @param spaceId    空间 ID
 * @param seqNo      片段序号（从 1 递增）
 * @param headingPath 所属标题路径
 * @param text       片段正文
 * @param tokenLen   Token 长度
 * @param aclTags    标签级权限（逻辑外键集合）
 * @param visibility 可见性（inherit / public / private）
 */
public record ChunkPlan(UUID chunkId,
                        UUID documentId,
                        UUID versionId,
                        UUID tenantId,
                        UUID spaceId,
                        int seqNo,
                        String headingPath,
                        String text,
                        int tokenLen,
                        List<UUID> aclTags,
                        String visibility) {
}
