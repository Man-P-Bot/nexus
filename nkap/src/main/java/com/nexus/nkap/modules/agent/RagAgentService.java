package com.nexus.nkap.modules.agent;

import com.nexus.nkap.modules.retrieval.RetrievalResult;
import com.nexus.nkap.modules.retrieval.RetrievalService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RagAgentService {

    private final RetrievalService retrievalService;
    private final ChatClient chatClient;

    public RagAgentService(RetrievalService retrievalService, ChatModel chatModel) {
        this.retrievalService = retrievalService;
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    public String chat(String query, UUID tenantId, UUID spaceId) {
        System.out.println("====== Agent 收到的参数 ======");
        System.out.println("query = " + query);
        System.out.println("tenantId = " + tenantId);
        System.out.println("spaceId = " + spaceId);
        System.out.println("=============================");

        // 1. 检索相关片段（取前5条）
        List<RetrievalResult> chunks = retrievalService.search(query, tenantId, spaceId, 5);

        System.out.println("检索到的 chunk 数量: " + chunks.size());

        if (chunks.isEmpty()) {
            return "抱歉，知识库中未找到相关内容";
        }

        // 2. 拼接上下文，并清洗掉 BOM 等不可见特殊字符
        String context = chunks.stream()
                .map(RetrievalResult::text)
                // 清洗掉可能存在的 BOM 头和其他不可见控制字符
                .map(text -> text.replace("\uFEFF", "").replaceAll("[\\p{Cf}]", "").trim())
                .collect(Collectors.joining("\n\n---\n\n"));

        // 3. 构造更加正向引导的 System Prompt
        String systemPrompt = """
                你是一个专业的企业知识库助手。
                请根据以下【参考资料】直接回答用户的问题。
                请注意：即使参考资料看起来不够详细，只要其中包含了用户询问的关键信息，请务必将其提取出来回答用户，不要轻易说“不知道”。
                
                【参考资料】：
                """ + context;

        System.out.println("====== 最终发给大模型的 System Prompt ======");
        System.out.println(systemPrompt);
        System.out.println("============================================");

        // 4. 调用大模型生成回答
        return chatClient.prompt()
                .system(systemPrompt)
                .user(query)
                .call()
                .content();
    }
}