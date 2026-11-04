package com.nexus.nkap.ingestion.parser;

import com.nexus.nkap.common.BusinessException;
import com.nexus.nkap.common.ResultCode;
import com.nexus.nkap.domain.DocumentEntity;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * PDF 解析器（MVP）。
 *
 * <p>使用 Apache PDFBox 提取正文文本，按页/段落保留换行，并转换为 Markdown 纯文本。
 * 尝试以启发式规则识别大标题作为 {@link TocEntry}；若 PDF 无明确标题结构，toc 返回空列表。
 *
 * <p>对应《09-详细设计》§2.3 中 {@code .pdf → Apache PDFBox（正文）+ Tesseract（扫描页 OCR，留 V1）}。
 */
@Component
public class PdfParser implements DocumentParser {

    private static final Set<String> SUPPORTED = Set.of("pdf");

    @Override
    public boolean supports(String extension) {
        return extension != null && SUPPORTED.contains(extension.toLowerCase(Locale.ROOT));
    }

    @Override
    public ParsedDocument parse(DocumentEntity document, Path file) {
        try (PDDocument pdf = Loader.loadPDF(file.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            // 按阅读顺序排序，尽量还原排版结构
            stripper.setSortByPosition(true);
            String rawText = stripper.getText(pdf);
            return build(rawText);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.DOCUMENT_CONTENT_INVALID,
                    "failed to parse PDF: " + file, e);
        }
    }

    /** 归一化换行、去除分页符，并尝试识别标题。 */
    private ParsedDocument build(String rawText) {
        String text = rawText == null ? "" : rawText;
        // 统一换行，分页符转为换行
        text = text.replace("\r\n", "\n").replace("\r", "\n").replace("\f", "\n");
        String markdown = text.strip();
        return new ParsedDocument(markdown, detectHeadings(markdown));
    }

    /**
     * 启发式标题识别：短行、无句末标点、符合标题特征。
     * 识别不到时返回空列表（不阻塞解析）。
     */
    private List<TocEntry> detectHeadings(String text) {
        List<TocEntry> toc = new ArrayList<>();
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (looksLikeHeading(trimmed)) {
                toc.add(new TocEntry(1, trimmed, null));
            }
        }
        return toc;
    }

    private boolean looksLikeHeading(String line) {
        if (line.length() < 2 || line.length() > 60) {
            return false;
        }
        // 句末标点结尾 → 视为正文
        if (line.matches(".*[。.!?！？;；:：]$")) {
            return false;
        }
        // Markdown 标题
        if (line.matches("^#{1,6}\\s+.*")) {
            return true;
        }
        // 全大写拉丁短行
        if (line.matches("^[A-Z0-9][A-Z0-9\\s/&()\\-]{2,40}$")) {
            return true;
        }
        // 中文短行（无句末标点）
        if (line.matches("^[\\u4e00-\\u9fa5A-Za-z0-9（）()\\s]{2,20}$")) {
            return true;
        }
        return false;
    }
}
