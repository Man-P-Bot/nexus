package com.nexus.nkap.ingestion.parser;

import com.nexus.nkap.domain.DocumentEntity;

import java.nio.file.Path;

/**
 * 文档解析器接口。
 *
 * <p>解析能力统一由本接口抽象，新增格式只需扩展实现（策略模式）。
 * 依据《09-详细设计》§2.3：解析产出统一为 Markdown 结构化文本 + 目录（toc）。
 *
 * <p>解析失败应抛出 {@link com.nexus.nkap.common.BusinessException}
 * （错误码 {@link com.nexus.nkap.common.ResultCode#DOCUMENT_CONTENT_INVALID}），
 * 由上层管线标记文档为 {@code failed}。
 */
public interface DocumentParser {

    /**
     * 是否支持指定扩展名（不含点，小写）。
     *
     * @param extension 扩展名，如 {@code "pdf"}
     * @return 支持返回 true
     */
    boolean supports(String extension);

    /**
     * 将文件解析为统一的 Markdown 结构化文本与目录。
     *
     * @param document 文档元数据（可用于日志与上下文）
     * @param file     已落盘的原始文件路径
     * @return 解析结果（markdown + toc）
     */
    ParsedDocument parse(DocumentEntity document, Path file);
}
