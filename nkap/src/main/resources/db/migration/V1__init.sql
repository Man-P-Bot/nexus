-- Step 0 占位迁移：仅启用 pgvector 扩展。
-- 具体表结构在 Step 1 按《10 数据库设计》补充。

CREATE EXTENSION IF NOT EXISTS vector;
