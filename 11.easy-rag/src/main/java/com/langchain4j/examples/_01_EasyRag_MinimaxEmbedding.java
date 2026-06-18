package com.langchain4j.examples;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

/**
 * RAG 概述示例
 * <p>
 * langchain4j RAG 完整流程：
 * ┌────────────────────────────────────────────────────────┐
 * │  1. DocumentLoader    - 加载文档                      │
 * │  2. DocumentSplitter - 分割文档成小块                  │
 * │  3. EmbeddingModel  - 向量化                        │
 * │  4. EmbeddingStore  - 存储向量                      │
 * │  5. ContentRetriever - 检索相关文档                  │
 * │  6. AiService      - LLM 生成回答                  │
 * └────────────────────────────────────────────────────────┘
 * <p>
 * 相关模块：
 * - langchain4j: 核心模块（Document, AiServices）
 * - langchain4j-open-ai: OpenAI 集成
 * - langchain4j-easy-rag: Easy RAG 辅助（DocumentSplitterFactory）
 * - langchain4j-embeddings-*: Embedding 模型（需单独添加）
 * - langchain4j-store-embedding-*: 向量存储（需单独添加）
 */
public class _01_EasyRag_MinimaxEmbedding {

    interface Assistant {
        String chat(String userMessage);
    }

    public static void main(String[] args) {
        String deepseekApiKey = System.getenv("DEEPSEEK_API_KEY");
        if (deepseekApiKey == null || deepseekApiKey.isBlank()) {
            System.err.println("请配置环境变量 DEEPSEEK_API_KEY");
            return;
        }

        String minimaxApiKey = System.getenv("MINIMAX_API_KEY");
        if (minimaxApiKey == null || minimaxApiKey.isBlank()) {
            System.err.println("请配置环境变量 MINIMAX_API_KEY");
            return;
        }

        String minimaxGroupId = System.getenv("MINIMAX_GROUP_ID");
        if (minimaxGroupId == null || minimaxGroupId.isBlank()) {
            System.err.println("请配置环境变量 MINIMAX_GROUP_ID");
            return;
        }

        // 创建 ChatModel
        System.out.println("【创建助手】");
        ChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(deepseekApiKey)
                .modelName("deepseek-chat")
                .build();

        EmbeddingModel embeddingModel = new MinimaxEmbeddingModel(minimaxApiKey, minimaxGroupId);

        Response<Embedding> response = embeddingModel.embed("雷鸣");

        // 个人简历
        System.out.println("【加载个人简历】");
        Document document = FileSystemDocumentLoader.loadDocument("C:\\Users\\Administrator\\Documents\\resume-bw---c092ddc2-2c2e-46ba-b218-a4652951f56c_1.pdf");

        // 向量化
        System.out.println("【向量化】");
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
        ingestor.ingest(document);

        // 定义 AiService
        System.out.println("【创建助手】");
        interface Assistant {
            @SystemMessage("""
                    你现在所处年份是2026年，计算年龄必须用2026减去出生年份，不允许假设其他年份。
                    根据简历信息回答用户问题，计算年龄严格以2026年为准。
                    """)
            @UserMessage("{userMessage}")
            String chat(String userMessage);
        }

        // 专门构建否则  Conflict: multiple embedding models have been found in the classpath. Please explicitly specify the one you wish to use.
        // 原因是引入了, langchain4j-embeddings-bge-small-zh-v15, 是模型变多了
        EmbeddingStoreContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        // 创建 AiService
        System.out.println("【创建助手】");
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                // 聊天记录
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                // 检索相关文档
                .contentRetriever(retriever)
                .build();

        System.out.println("【提问】");
        String answer = assistant.chat("雷鸣多大年龄?");

        System.out.println("【回答】");
        System.out.println("AI: " + answer);

    }
}
