package com.langchain4j.examples;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * LangChain4j AI 服务 - 流式传输、聊天记忆进阶示例
 * 
 * 本示例演示：
 * 1. StreamingChatModel 流式传输
 * 2. 聊天记忆 Provider（多用户）
 * 
 * 注意：TokenStream 和 @Tool 功能在 LangChain4j 1.3.0 中需要更高版本
 * 
 * @see <a href="https://langchain4j.cn/tutorials/ai-services.html">AI 服务</a>
 */
public class AiServicesStreamingAndToolsExample {

    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("请设置环境变量 DEEPSEEK_API_KEY");
            return;
        }

        // 流式模型
        StreamingChatModel streamingModel = OpenAiStreamingChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .build();

        // 普通模型
        dev.langchain4j.model.chat.ChatModel model = dev.langchain4j.model.openai.OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .build();

        // 示例1：StreamingChatModel 流式传输
        example1_streamingChatModel(streamingModel);

        // 示例2：聊天记忆 Provider（多用户）
        example2_chatMemoryProvider(model);
    }

    /**
     * 示例1：StreamingChatModel 流式传输
     * 使用 StreamingChatResponseHandler 处理流式响应
     */
    static void example1_streamingChatModel(StreamingChatModel model) throws InterruptedException {
        System.out.println("\n=== 示例1：StreamingChatModel 流式传输 ===");

        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder fullResponse = new StringBuilder();

        System.out.println("用户: 给我讲一个程序员笑话");

        model.chat("给我讲一个程序员笑话", new StreamingChatResponseHandler() {
            
            @Override
            public void onPartialResponse(String partialResponse) {
                System.out.print(partialResponse);
                fullResponse.append(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                System.out.println("\n\n流式传输完成！");
                if (completeResponse.metadata().tokenUsage() != null) {
                    System.out.println("Token使用量: " + completeResponse.metadata().tokenUsage());
                }
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                System.err.println("\n错误: " + error.getMessage());
                latch.countDown();
            }
        });

        latch.await(60, TimeUnit.SECONDS);
    }

    /**
     * 示例2：聊天记忆 Provider
     * 为每个用户维护独立的聊天记忆
     */
    static void example2_chatMemoryProvider(dev.langchain4j.model.chat.ChatModel model) {
        System.out.println("\n=== 示例2：聊天记忆 Provider ===");

        interface Chatbot {
            String chat(@dev.langchain4j.service.MemoryId int memoryId, 
                       @dev.langchain4j.service.UserMessage String message);
        }

        Chatbot chatbot = AiServices.builder(Chatbot.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();

        // 用户 A 的对话
        System.out.println("\n--- 用户 A ---");
        System.out.println("用户A: 你好，我叫小明");
        String r1 = chatbot.chat(1, "你好，我叫小明");
        System.out.println("AI: " + r1);

        System.out.println("用户A: 我叫什么名字？");
        String r2 = chatbot.chat(1, "我叫什么名字？");
        System.out.println("AI: " + r2);

        // 用户 B 的对话（独立的记忆）
        System.out.println("\n--- 用户 B ---");
        System.out.println("用户B: 你好，我叫小红");
        String r3 = chatbot.chat(2, "你好，我叫小红");
        System.out.println("AI: " + r3);

        System.out.println("用户B: 我叫什么名字？");
        String r4 = chatbot.chat(2, "我叫什么名字？");
        System.out.println("AI: " + r4);

        // 继续用户 A 的对话
        System.out.println("\n--- 用户 A 继续 ---");
        System.out.println("用户A: 今天天气怎么样？");
        String r5 = chatbot.chat(1, "今天天气怎么样？");
        System.out.println("AI: " + r5);
    }
}