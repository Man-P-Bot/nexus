package com.nexus.nkap.ingestion.chunk;

import com.nexus.nkap.common.BusinessException;
import com.nexus.nkap.common.ResultCode;
import com.nexus.nkap.ingestion.parser.ParsedDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 标题感知切分策略（heading_aware）。
 */
@Component
public class HeadingAwareChunkingStrategy implements ChunkingStrategy {

    /** 语义区块：标题路径 + 区块正文。 */
    private record Section(String headingPath, String content) {
    }

    @Override
    public List<ChunkPlan> chunk(ParsedDocument parsed, ChunkingConfig config, ChunkContext context) {
        String markdown = parsed == null ? null : parsed.markdown();
        if (markdown == null || markdown.isBlank()) {
            throw new BusinessException(ResultCode.DOCUMENT_CONTENT_INVALID, "document content is empty");
        }

        String text = markdown.replace("\r\n", "\n").replace("\r", "\n");

        List<Section> sections = splitIntoSections(text);
        List<ChunkPlan> plans = new ArrayList<>();
        int seqNo = 1;

        for (Section section : sections) {
            String body = section.content().strip();
            if (body.isEmpty()) {
                continue;
            }
            for (String piece : splitByLength(body, config)) {
                plans.add(new ChunkPlan(UUID.randomUUID(), context.documentId(), context.versionId(),
                        context.tenantId(), context.spaceId(), seqNo++,
                        section.headingPath(), piece, piece.length(),
                        context.aclTags(), context.visibility()));
            }
        }

        // 兜底：整篇无标题且无有效片段时，按长度硬切
        if (plans.isEmpty()) {
            for (String piece : splitByLength(text.strip(), config)) {
                plans.add(new ChunkPlan(UUID.randomUUID(), context.documentId(), context.versionId(),
                        context.tenantId(), context.spaceId(), seqNo++,
                        null, piece, piece.length(), context.aclTags(), context.visibility()));
            }
        }

        return plans;
    }

    /** 按标题（H1~H6）将文本切分为语义区块，记录每个区块所属标题。 */
    private List<Section> splitIntoSections(String text) {
        List<Section> sections = new ArrayList<>();
        int start = 0;
        String currentHeading = null;
        int n = text.length();
        int i = 0;

        while (i < n) {
            boolean atLineStart = (i == 0) || (text.charAt(i - 1) == '\n');
            if (atLineStart) {
                int lineEnd = text.indexOf('\n', i);
                if (lineEnd == -1) {
                    lineEnd = n;
                }
                String line = text.substring(i, lineEnd).strip();
                if (isHeading(line)) {
                    String content = text.substring(start, i);
                    if (!content.isBlank()) {
                        sections.add(new Section(currentHeading, content));
                    }
                    currentHeading = line.replaceFirst("^#+\\s+", "").strip();
                    start = lineEnd + 1;
                    i = lineEnd + 1;
                    continue;
                }
            }
            i++;
        }

        String content = text.substring(start);
        if (!content.isBlank()) {
            sections.add(new Section(currentHeading, content));
        }
        return sections;
    }

    private boolean isHeading(String line) {
        return line.matches("^#{1,6}\\s+\\S.*");
    }

    /** 对区块正文按 chunkSize/overlap 滑窗切分；超长段落先按段落切再硬切。 */
    private List<String> splitByLength(String body, ChunkingConfig config) {
        int chunkSize = config.getChunkSize();
        int overlap = config.getOverlap();
        int maxChunkSize = config.getMaxChunkSize();

        if (body.length() <= maxChunkSize) {
            return List.of(body);
        }

        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : body.split("\n", -1)) {
            String para = paragraph.strip();
            if (para.isEmpty()) {
                continue;
            }

            if (para.length() > maxChunkSize) {
                flush(result, current, overlap);
                result.addAll(hardCut(para, chunkSize, overlap));
                continue;
            }

            if (current.length() > 0 && current.length() + para.length() + 1 > chunkSize) {
                flush(result, current, overlap);
            }
            if (current.length() > 0) {
                current.append('\n');
            }
            current.append(para);
        }

        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result;
    }

    /** 将当前累积片段入列，并按 overlap 保留尾部供下一片段重叠。 */
    private void flush(List<String> result, StringBuilder current, int overlap) {
        if (current.length() == 0) {
            return;
        }
        String previous = current.toString();
        result.add(previous);

        int overlapStart = Math.max(0, previous.length() - overlap);
        current.setLength(0);
        if (overlapStart < previous.length()) {
            current.append(previous, overlapStart, previous.length());
        }
    }

    /** 对超长文本做定长硬切，相邻片段重叠 overlap 字符。 */
    private List<String> hardCut(String text, int chunkSize, int overlap) {
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + chunkSize);
            result.add(text.substring(start, end));
            if (end >= text.length()) {
                break;
            }
            start = end - overlap;
        }
        return result;
    }
}