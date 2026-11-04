-- =============================================================================
-- V2__init_mvp_tables.sql
-- MVP 核心表结构（8 张）：
--   tenant / space / document / document_version /
--   chunk / conversation / message / api_key
-- 依据：《10-数据库设计》v0.3 §3（表结构）与 §4（索引）
-- 规范：表名/字段名小写下划线；每表含 id(UUID)/tenant_id/created_at/updated_at/is_deleted
-- 说明：DDL 由 Flyway 管理，JPA 仅映射、禁用 ddl-auto=update（设计 §6）
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 扩展（幂等）：若 V1 未生效则在此兜底
-- -----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS vector;   -- pgvector：chunk.embedding 向量列
CREATE EXTENSION IF NOT EXISTS pg_trgm;  -- pg_trgm：chunk.text GIN 模糊检索（设计 §1 扩展清单）

-- -----------------------------------------------------------------------------
-- 1. tenant 租户（多租户隔离单元，根表，无 tenant_id）
-- -----------------------------------------------------------------------------
CREATE TABLE tenant (
    id            UUID         PRIMARY KEY,
    code          VARCHAR(32)  NOT NULL,               -- 租户编码
    name          VARCHAR(128) NOT NULL,               -- 租户名称
    status        VARCHAR(16)  NOT NULL DEFAULT 'active',  -- active/suspended/deleted
    plan          VARCHAR(16)  NOT NULL DEFAULT 'standard',-- 套餐（影响配额）
    max_documents INT          NOT NULL DEFAULT 10000,     -- 文档上限
    max_qps       INT          NOT NULL DEFAULT 50,       -- 检索 QPS 上限
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_tenant_code UNIQUE (code)
);
CREATE INDEX idx_tenant_status ON tenant (status);

-- -----------------------------------------------------------------------------
-- 2. space 空间（知识库空间，权限与检索边界）
--    注：created_by 引用 user 表，user 不在本批 MVP 表内，暂不建 FK，待补 user 表后再加
-- -----------------------------------------------------------------------------
CREATE TABLE space (
    id          UUID         PRIMARY KEY,
    tenant_id   UUID         NOT NULL,
    name        VARCHAR(128) NOT NULL,
    description TEXT,
    visibility  VARCHAR(16)  NOT NULL DEFAULT 'private', -- public/private
    tag_ids     UUID[]       NOT NULL DEFAULT '{}',      -- 空间标签
    created_by  UUID,                                     -- FK→user（暂缓）
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    is_deleted  BOOLEAN      NOT NULL DEFAULT false,
    CONSTRAINT fk_space_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);
CREATE INDEX idx_space_tenant_visibility ON space (tenant_id, visibility);
CREATE INDEX idx_space_tenant_deleted   ON space (tenant_id, is_deleted);

-- -----------------------------------------------------------------------------
-- 3. document 文档（元数据主记录，聚合版本与切分）
--    注：created_by 引用 user 表，暂不建 FK
-- -----------------------------------------------------------------------------
CREATE TABLE document (
    id              UUID         PRIMARY KEY,
    tenant_id       UUID         NOT NULL,
    space_id        UUID         NOT NULL,
    title           VARCHAR(512) NOT NULL,
    source_type     VARCHAR(32)  NOT NULL,               -- upload/url/api/...
    source_url      TEXT,
    content_hash    CHAR(64)     NOT NULL,               -- SHA-256，去重依据
    status          VARCHAR(24)  NOT NULL DEFAULT 'pending', -- pending/parsing/indexing/ready/failed/quarantined
    current_version INT          NOT NULL DEFAULT 1,
    tag_ids         UUID[]       NOT NULL DEFAULT '{}',
    created_by      UUID,                                -- FK→user（暂缓）
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    is_deleted      BOOLEAN      NOT NULL DEFAULT false,
    CONSTRAINT fk_document_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT fk_document_space  FOREIGN KEY (space_id)  REFERENCES space (id),
    CONSTRAINT uk_document_hash   UNIQUE (tenant_id, space_id, content_hash)
);
CREATE INDEX idx_document_tenant_space_status ON document (tenant_id, space_id, status);

-- -----------------------------------------------------------------------------
-- 4. document_version 文档版本（保留历史版本，支持回滚与增量更新）
-- -----------------------------------------------------------------------------
CREATE TABLE document_version (
    id            UUID         PRIMARY KEY,
    document_id   UUID         NOT NULL,
    tenant_id     UUID         NOT NULL,                 -- 冗余加速过滤
    version_no    INT          NOT NULL,
    content_hash  CHAR(64)     NOT NULL,
    object_key    TEXT         NOT NULL,                 -- MinIO 对象键
    status        VARCHAR(24)  NOT NULL,
    is_current    BOOLEAN      NOT NULL DEFAULT false,
    superseded_at TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_docver_document FOREIGN KEY (document_id) REFERENCES document (id),
    CONSTRAINT fk_docver_tenant   FOREIGN KEY (tenant_id)   REFERENCES tenant (id),
    CONSTRAINT uk_docver_no       UNIQUE (document_id, version_no)
);
CREATE INDEX idx_docver_tenant_current ON document_version (tenant_id, is_current);

-- -----------------------------------------------------------------------------
-- 5. chunk 切分块（最小可检索单元，承载向量列与权限快照）
-- -----------------------------------------------------------------------------
CREATE TABLE chunk (
    id           UUID         PRIMARY KEY,
    tenant_id    UUID         NOT NULL,
    space_id     UUID         NOT NULL,
    document_id  UUID         NOT NULL,
    version_id   UUID         NOT NULL,
    seq_no       INT          NOT NULL,
    heading_path VARCHAR(512),
    text         TEXT         NOT NULL,
    token_len    INT          NOT NULL,
    embedding    vector(1024),                          -- bge-m3 向量（pgvector）
    acl_tags     UUID[]       NOT NULL DEFAULT '{}',    -- 标签级权限
    visibility   VARCHAR(16)  NOT NULL DEFAULT 'inherit',
    status       VARCHAR(16)  NOT NULL DEFAULT 'active', -- active/superseded/deleted
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_chunk_tenant   FOREIGN KEY (tenant_id)   REFERENCES tenant (id),
    CONSTRAINT fk_chunk_space    FOREIGN KEY (space_id)    REFERENCES space (id),
    CONSTRAINT fk_chunk_document FOREIGN KEY (document_id) REFERENCES document (id),
    CONSTRAINT fk_chunk_version  FOREIGN KEY (version_id)  REFERENCES document_version (id)
);
-- 过滤字段索引（设计 §4.2，均建索引保证谓词下推）
CREATE INDEX idx_chunk_tenant_space_doc_status ON chunk (tenant_id, space_id, document_id, status);
CREATE INDEX idx_chunk_tenant_space_status     ON chunk (tenant_id, space_id, status);
CREATE INDEX idx_chunk_tenant_space_version    ON chunk (tenant_id, space_id, version_id);
CREATE INDEX idx_chunk_acl_tags_gin            ON chunk USING gin (acl_tags);
-- 向量 ANN 索引（设计 §4.1，主索引 HNSW，余弦距离）
CREATE INDEX idx_chunk_embedding_hnsw ON chunk USING hnsw (embedding vector_cosine_ops);
-- 全文模糊检索（设计 §4.3，MVP 可用 pg_trgm+GIN 兜底）
CREATE INDEX idx_chunk_text_trgm ON chunk USING gin (text gin_trgm_ops);

-- -----------------------------------------------------------------------------
-- 6. conversation 会话（短期记忆与检索缓存）
--    注：user_id 引用 user 表，暂不建 FK
-- -----------------------------------------------------------------------------
CREATE TABLE conversation (
    id            UUID         PRIMARY KEY,
    tenant_id     UUID         NOT NULL,
    space_id      UUID,                                 -- 可空：跨空间
    user_id       UUID         NOT NULL,                 -- FK→user（暂缓）
    title         VARCHAR(256),
    status        VARCHAR(16)  NOT NULL DEFAULT 'active', -- active/archived/closed
    summary       TEXT,
    message_count INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    is_deleted    BOOLEAN      NOT NULL DEFAULT false,
    CONSTRAINT fk_conversation_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT fk_conversation_space  FOREIGN KEY (space_id)  REFERENCES space (id)
);
CREATE INDEX idx_conversation_tenant_user ON conversation (tenant_id, user_id, updated_at DESC);

-- -----------------------------------------------------------------------------
-- 7. message 消息（会话内单条提问/回答）
-- -----------------------------------------------------------------------------
CREATE TABLE message (
    id              UUID         PRIMARY KEY,
    tenant_id       UUID         NOT NULL,
    conversation_id UUID         NOT NULL,
    role            VARCHAR(16)  NOT NULL,               -- user/assistant/system/tool
    content         TEXT         NOT NULL,
    tool_calls      JSONB,
    tool_call_id    VARCHAR(64),
    citations       JSONB,                               -- 引用 ID 快照
    tokens_in       INT,
    tokens_out      INT,
    model           VARCHAR(64),
    trace_id        VARCHAR(64),                         -- Langfuse trace
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_message_tenant       FOREIGN KEY (tenant_id)       REFERENCES tenant (id),
    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversation (id)
);
CREATE INDEX idx_message_conversation_created ON message (conversation_id, created_at);
CREATE INDEX idx_message_tenant_created      ON message (tenant_id, created_at);

-- -----------------------------------------------------------------------------
-- 8. api_key API 密钥（开放平台调用凭证）
--    注：created_by 引用 user 表，暂不建 FK
-- -----------------------------------------------------------------------------
CREATE TABLE api_key (
    id           UUID         PRIMARY KEY,
    tenant_id    UUID         NOT NULL,
    name         VARCHAR(128) NOT NULL,
    key_prefix   VARCHAR(16)  NOT NULL,                 -- 明文前缀（展示用）
    key_hash     CHAR(64)     NOT NULL,                 -- SHA-256 哈希
    scopes       TEXT[]       NOT NULL DEFAULT '{}',    -- 授权范围
    expires_at   TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,
    status       VARCHAR(16)  NOT NULL DEFAULT 'active',-- active/revoked
    created_by   UUID,                                  -- FK→user（暂缓）
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_apikey_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT uk_apikey_prefix UNIQUE (tenant_id, key_prefix)
);
CREATE INDEX idx_apikey_hash ON api_key (key_hash);

-- =============================================================================
-- 迁移完成：MVP 8 张核心表已就绪
-- 后续（非 MVP）：user/role/user_role/space_member/document_acl/audit_log/
--               message_citation/citation_snippet/tool/tool_grant/feedback/
--               eval_record/memory/task_job/tag/tag_grant
-- =============================================================================
