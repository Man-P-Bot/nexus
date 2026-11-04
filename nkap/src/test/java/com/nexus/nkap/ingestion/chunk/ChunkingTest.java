package com.nexus.nkap.ingestion.chunk;

import com.nexus.nkap.domain.DocumentEntity;
import com.nexus.nkap.ingestion.parser.ParsedDocument;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ChunkingTest {

    @Test
    void testHeadingAwareChunking() {
        HeadingAwareChunkingStrategy strategy = new HeadingAwareChunkingStrategy();
        ChunkingConfig config = new ChunkingConfig();

        // 构造一个极端文本：包含多级标题，以及一段超长无换行文本
        String markdown = """
                # 星枢 Nexus 员工手册
                欢迎来到星枢。这是第一章的内容。
                ## 考勤制度
                每天早上 9 点上班。
                ### 请假流程
                请假需要提前申请。填写 OA 系统。
                ## 超长段落测试
                这里是硬切测试。""" + "A".repeat(1200) + """
                
                ## 最后一节
                测试结束。
                """;

        ParsedDocument parsed = new ParsedDocument(markdown, null);
        DocumentEntity doc = new DocumentEntity();
        doc.setId(UUID.randomUUID());
        doc.setTenantId(UUID.randomUUID());
        doc.setSpaceId(UUID.randomUUID());
        doc.setTagIds(List.of());

        // 执行切分
        List<ChunkPlan> chunks = strategy.chunk(parsed, config,
                new ChunkingStrategy.ChunkContext(doc.getId(), UUID.randomUUID(), doc.getTenantId(), doc.getSpaceId(), doc.getTagIds(), "inherit"));

        // 打印验证
        System.out.println("总切片数: " + chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            ChunkPlan chunk = chunks.get(i);
            System.out.printf("--- Chunk %d [标题: %s] [长度: %d] ---\n", i+1, chunk.headingPath(), chunk.text().length());
            System.out.println(chunk.text().substring(0, Math.min(50, chunk.text().length())) + "...\n");
        }

        // 断言
        assertFalse(chunks.isEmpty(), "切片不能为空");
        // 验证超长段落 1200 字，在 chunkSize=500, overlap=80 下，应该切出至少 3 块
        long longChunkCount = chunks.stream().filter(c -> "超长段落测试".equals(c.headingPath())).count();
        assertTrue(longChunkCount >= 3, "1200字的超长段落应该被硬切为至少3块");

        // 验证 overlap 存在（检查相邻块是否有重叠的文字）
        // 由于这里全是 A，不好直观断言 overlap，但通过打印长度可以看出一块大概在 500 左右
    }
}