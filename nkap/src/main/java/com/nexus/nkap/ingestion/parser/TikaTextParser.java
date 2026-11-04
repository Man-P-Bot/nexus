package com.nexus.nkap.ingestion.parser;

import com.nexus.nkap.common.BusinessException;
import com.nexus.nkap.common.ResultCode;
import com.nexus.nkap.domain.DocumentEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 纯文本 / Markdown 解析器（MVP）。
 *
 * <p>处理 {@code .txt} 与 {@code .md}：以 UTF-8 读取并保留段落换行。
 * Markdown 会提取 {@code #} 标题作为目录（toc）；纯文本无标题结构时 toc 为空。
 *
 * <p>对应《09-详细设计》§2.3 中 {@code .html/.md/.txt → Apache Tika}；
 * MVP 仅覆盖文本与 Markdown，直接读取文本内容，HTML 留 V1。
 */
@Component
public class TikaTextParser implements DocumentParser {

    private static final Set<String> SUPPORTED = Set.of("txt", "md");

    @Override
    public boolean supports(String extension) {
        return extension != null && SUPPORTED.contains(extension.toLowerCase(Locale.ROOT));
    }

    @Override
    public ParsedDocument parse(DocumentEntity document, Path file) {
        try {
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            String markdown = normalize(raw);
            return new ParsedDocument(markdown, extractToc(markdown));
        } catch (IOException e) {
            throw new BusinessException(ResultCode.DOCUMENT_CONTENT_INVALID,
                    "failed to read text file: " + file, e);
        }
    }

    /** 统一换行符并去除首尾空白，保留段落结构。 */
    private String normalize(String raw) {
        String text = raw == null ? "" : raw.replace("\r\n", "\n").replace("\r", "\n");
        return text.strip();
    }

    /** 提取 Markdown {@code #} 标题作为目录条目（txt 无标题，返回空列表）。 */
    private List<TocEntry> extractToc(String markdown) {
        List<TocEntry> toc = new ArrayList<>();
        for (String line : markdown.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.charAt(0) != '#') {
                continue;
            }
            int level = 0;
            while (level < trimmed.length() && trimmed.charAt(level) == '#') {
                level++;
            }
            level = Math.min(level, 6);
            String title = trimmed.substring(level).trim();
            if (!title.isEmpty()) {
                toc.add(new TocEntry(level, title, null));
            }
        }
        return toc;
    }
}
