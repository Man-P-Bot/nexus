package com.nexus.nkap.ingestion.chunk;

import java.util.List;

/**
 * 切分参数配置。
 *
 * <p>默认值严格对齐《09-详细设计》§2.4：
 * <ul>
 *   <li>{@code chunk_size = 500}，{@code overlap = 80}</li>
 *   <li>{@code min_chunk_size = 80}（尾部短块并入前块）</li>
 *   <li>{@code max_chunk_size = 650}（超长段落硬切）</li>
 *   <li>{@code separator} 优先按标题层级，其次段落，最后按长度</li>
 *   <li>{@code keep_title_path = true}，{@code chunk_strategy = heading_aware}</li>
 * </ul>
 *
 * <p>使用普通 POJO 而非 record，以支持 Builder 风格的可选覆盖。
 */
public class ChunkingConfig {

    /** 目标片段长度（字符） */
    private int chunkSize = 500;

    /** 相邻片段重叠字符数 */
    private int overlap = 80;

    /** 低于该长度的尾部片段合并到前块 */
    private int minChunkSize = 80;

    /** 超长段落硬切上限 */
    private int maxChunkSize = 650;

    /** 分隔符优先级：标题层级 → 段落 → 换行 → 空格 */
    private List<String> separators = List.of("\n## ", "\n### ", "\n\n", "\n", " ");

    /** 每个片段是否携带所属标题路径 */
    private boolean keepTitlePath = true;

    /** 切分策略标识 */
    private String strategy = "heading_aware";

    public int getChunkSize() {
        return chunkSize;
    }

    public ChunkingConfig setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
        return this;
    }

    public int getOverlap() {
        return overlap;
    }

    public ChunkingConfig setOverlap(int overlap) {
        this.overlap = overlap;
        return this;
    }

    public int getMinChunkSize() {
        return minChunkSize;
    }

    public ChunkingConfig setMinChunkSize(int minChunkSize) {
        this.minChunkSize = minChunkSize;
        return this;
    }

    public int getMaxChunkSize() {
        return maxChunkSize;
    }

    public ChunkingConfig setMaxChunkSize(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
        return this;
    }

    public List<String> getSeparators() {
        return separators;
    }

    public ChunkingConfig setSeparators(List<String> separators) {
        this.separators = separators;
        return this;
    }

    public boolean isKeepTitlePath() {
        return keepTitlePath;
    }

    public ChunkingConfig setKeepTitlePath(boolean keepTitlePath) {
        this.keepTitlePath = keepTitlePath;
        return this;
    }

    public String getStrategy() {
        return strategy;
    }

    public ChunkingConfig setStrategy(String strategy) {
        this.strategy = strategy;
        return this;
    }
}
