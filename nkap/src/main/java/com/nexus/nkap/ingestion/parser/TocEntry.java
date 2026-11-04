package com.nexus.nkap.ingestion.parser;

/**
 * 文档目录条目。
 *
 * <p>描述 Markdown 中的标题层级，供切分阶段构建 {@code heading_path}。
 * 依据《09-详细设计》§2.3「输出 toc（目录层级）供切分使用」。
 *
 * @param level   标题层级（1 = H1，6 = H6）
 * @param title   标题文本
 * @param anchor  标题锚点（用于前端跳转，可为空）
 */
public record TocEntry(int level, String title, String anchor) {
}
