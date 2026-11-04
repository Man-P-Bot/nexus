package com.nexus.nkap.ingestion.parser;

import com.nexus.nkap.common.BusinessException;
import com.nexus.nkap.common.ResultCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * 文档解析器工厂：按扩展名路由到具体 {@link DocumentParser} 实现。
 *
 * <p>自动收集 Spring 容器中的所有 {@code DocumentParser} Bean，实现「新增格式只需注册实现」的开闭原则。
 */
@Component
public class DocumentParserFactory {

    private final List<DocumentParser> parsers;

    public DocumentParserFactory(List<DocumentParser> parsers) {
        this.parsers = parsers;
    }

    /**
     * 获取支持指定扩展名的解析器。
     *
     * @param extension 扩展名（不含点）
     * @return 匹配的解析器
     * @throws BusinessException 无任何解析器支持该扩展名时抛出
     */
    public DocumentParser getParser(String extension) {
        String ext = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        return parsers.stream()
                .filter(parser -> parser.supports(ext))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.DOCUMENT_CONTENT_INVALID,
                        "no parser supports extension: " + extension));
    }
}
