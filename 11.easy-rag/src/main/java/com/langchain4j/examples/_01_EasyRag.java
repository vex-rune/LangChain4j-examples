package com.langchain4j.examples;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzhv15.BgeSmallZhV15EmbeddingModelFactory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.List;

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
public class _01_EasyRag {

    interface Assistant {
        String chat(String userMessage);
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("请配置环境变量 DEEPSEEK_API_KEY");
            return;
        }

        // 创建 ChatModel
        System.out.println("【创建助手】");
        ChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .build();

        // 个人简历
        System.out.println("【加载个人简历】");
        Document document = FileSystemDocumentLoader.loadDocument("C:\\Users\\Administrator\\Documents\\resume-bw---c092ddc2-2c2e-46ba-b218-a4652951f56c_1.pdf");

        EmbeddingModel embeddingModel = new BgeSmallZhV15EmbeddingModelFactory().create();

        // 向量化
        /// 关键点：
        /// 内置本地 ONNX 量化模型，离线本地 AI，不需要调用外部 API、不需要联网、不需要 key
        /// 是 langchain4j 自带的轻量英文 BGE-small 向量模型，打包在依赖里
        /// EmbeddingStoreIngestor 不传自定义 embeddingModel 时，会通过 Java SPI 机制自动实例化这个本地模型做向量化
        System.out.println("【向量化】");
        /// 作用：创建内存向量库
        /// InMemoryEmbeddingStore：LangChain4j 内置内存型向量存储，数据只存在 JVM 内存，程序关闭全部丢失，无需 Redis/Milvus 等第三方库。
        /// 存储对象 TextSegment：文档切割后的文本片段，包含：原文文本、文本向量、元数据。
        /// 此时只是初始化空容器，还没有任何数据、向量。
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        // 索引
        System.out.println("【索引化】");
        ///mbeddingStoreIngestor.ingest(document, embeddingStore);
        /// 这一行是完整 RAG 文档处理流水线，内部自动串行执行 4 大步骤：
        /// 步骤 1：文档自动分段（Splitter）
        /// 读取传入的完整简历Document，使用默认分段器 DocumentByParagraphSplitter 按段落切割，生成多个TextSegment文本块。
        /// 日志可见：Documents were split into 9 text segments，你的简历切成了 9 段。
        /// 步骤 2：加载嵌入模型、生成向量（Embedding）
        /// 自动加载默认量化向量模型 BgeSmallEnV15QuantizedEmbeddingModel；
        /// 循环遍历 9 个文本片段，调用模型把每一段文字转为浮点型向量数组；
        /// 日志：Starting to embed 9 text segments → Finished embedding 9 text segments。
        /// 步骤 3：向量写入内存库（Store）
        /// 把「文本片段 + 对应向量」成对存入上面初始化的 embeddingStore 内存容器；
        /// 日志：Starting to store 9 text segments into the embedding store。
        /// 步骤 4：构建内存索引
        /// 内存库内部自动构建简易相似度检索索引，后续提问时可以快速余弦相似度匹配最相关片段。
        EmbeddingStoreIngestor.builder().embeddingModel( embeddingModel).embeddingStore(embeddingStore).build().ingest(document );

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

        // 本地的embedding不好用请看 @see _01_EasyRag_MinimaxEmbedding
    }
}
