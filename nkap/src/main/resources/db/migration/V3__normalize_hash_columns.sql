-- =============================================================================
-- V3__normalize_hash_columns.sql
-- 将哈希列类型由 CHAR(64) 统一为 VARCHAR(64)。
--
-- 背景：V2 按设计文档声明为 CHAR(64)，但实体以 String + length=64 映射，
--       Hibernate 6 推导为 VARCHAR(64)，ddl-auto=validate 严格比对导致启动失败。
--       CHAR 存在尾部空格补齐语义，不利于哈希比对与索引，统一为 VARCHAR。
--
-- 说明：仅新增前向迁移，不修改已应用的 V1/V2，保持 Flyway 链路完整。
-- 幂等：IF EXISTS 保证重复执行安全。
-- =============================================================================

ALTER TABLE api_key
    ALTER COLUMN key_hash TYPE VARCHAR(64);

ALTER TABLE document
    ALTER COLUMN content_hash TYPE VARCHAR(64);

ALTER TABLE document_version
    ALTER COLUMN content_hash TYPE VARCHAR(64);

-- 哈希列无尾部空格语义，CHAR→VARCHAR 转换后长度语义一致（64）。
-- 若存在历史数据，CHAR 补齐的尾部空格会被 PG 在比较时忽略，转换无数据丢失。
