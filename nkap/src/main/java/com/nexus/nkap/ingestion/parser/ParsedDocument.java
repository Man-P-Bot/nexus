package com.nexus.nkap.ingestion.parser;

import java.util.List;

/**
 * 文档解析结果。
 *
 * <p>解析器统一输出 Markdown 结构化文本与目录（toc），供切分阶段使用。
 * 依据《09-详细设计》§2.3：保留标题（{@code #}）、表格、图片占位（{@code ![](minio-key)}）。
 *
 * @param markdown Markdown 结构化文本
 * @param toc      目录层级条目列表
 */
public record ParsedDocument(String markdown, List<TocEntry> toc) {

    /**
     * 便捷构造：无目录的纯文本解析结果。
     */
    public static ParsedDocument of(String markdown) {
        return new ParsedDocument(markdown, List.of());
    }
}
