package com.nexus.nkap.ingestion.parser;

import com.nexus.nkap.domain.DocumentEntity;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DocumentParser 解析效果验证测试。
 */
class DocumentParserTest {

    private static final Path RESOURCES = Path.of("src", "test", "resources");
    private static final String EN_TEXT = "Hello from Nexus ingestion test.";

    private DocumentEntity document() {
        DocumentEntity document = new DocumentEntity();
        document.setId(UUID.randomUUID());
        document.setTitle("test");
        return document;
    }

    private void print(String label, String markdown) {
        String preview = markdown.length() <= 200 ? markdown : markdown.substring(0, 200) + " ...";
        System.out.println("===== " + label + " (first 200 chars) =====");
        System.out.println(preview);
        System.out.println();
    }

    @Test
    void tikaTextParserParsesTxt() {
        ParsedDocument result = new TikaTextParser().parse(document(), RESOURCES.resolve("test.txt"));
        String markdown = result.markdown();

        assertFalse(markdown.isBlank(), "markdown 不应为空");
        assertTrue(markdown.contains(EN_TEXT), "应包含写入的英文文本");

        print("TikaTextParser(txt) markdown", markdown);
        System.out.println("toc size = " + result.toc().size());
    }

    @Test
    void pdfParserParsesPdf() {
        ParsedDocument result = new PdfParser().parse(document(), RESOURCES.resolve("test.pdf"));
        String markdown = result.markdown();

        assertFalse(markdown.isBlank(), "markdown 不应为空");
        assertTrue(markdown.contains(EN_TEXT), "应包含写入的英文文本");

        print("PdfParser(pdf) markdown", markdown);
        System.out.println("toc size = " + result.toc().size());
    }
}