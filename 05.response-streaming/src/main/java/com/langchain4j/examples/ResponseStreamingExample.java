package com.langchain4j.examples;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static dev.langchain4j.model.LambdaStreamingResponseHandler.onPartialResponse;

/**
 * LangChain4j 响应流式传输示例
 * 
 * 本示例演示：
 * 1. StreamingChatModel 流式响应
 * 2. 使用 StreamingChatResponseHandler 处理响应
 * 3. 使用 LambdaStreamingResponseHandler 简化流式处理
 * 
 * @see <a href="https://langchain4j.cn/tutorials/response-streaming.html">响应流式传输</a>
 */
public class ResponseStreamingExample {

    public static void main(String[] args) throws InterruptedException {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("请设置环境变量 DEEPSEEK_API_KEY");
            return;
        }

        StreamingChatModel model = OpenAiStreamingChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .build();

        // 示例1：使用 LambdaStreamingResponseHandler（推荐）
        example1_lambdaHandler(model);

        // 示例2：使用 StreamingChatResponseHandler
        example2_fullHandler(model);
    }

    /**
     * 示例1：使用 LambdaStreamingResponseHandler（推荐）
     * 最简洁的流式响应方式
     */
    static void example1_lambdaHandler(StreamingChatModel model) throws InterruptedException {
        System.out.println("\n=== 示例1：LambdaStreamingResponseHandler ===");
        System.out.println("用户: 请给我讲一个关于Java的故事");

        CountDownLatch latch = new CountDownLatch(1);

        // 使用 lambda 表达式处理流式响应
        model.chat("请给我讲一个关于Java的故事", onPartialResponse(System.out::print));

        // 等待响应完成
        latch.await(60, TimeUnit.SECONDS);
        System.out.println("\n\n流式响应完成！");
    }

    /**
     * 示例2：使用 StreamingChatResponseHandler
     * 提供更详细的控制和回调
     */
    static void example2_fullHandler(StreamingChatModel model) throws InterruptedException {
        System.out.println("\n=== 示例2：StreamingChatResponseHandler ===");
        System.out.println("用户: 解释一下什么是多态");

        CountDownLatch latch = new CountDownLatch(1);

        model.chat("解释一下什么是多态", new dev.langchain4j.model.chat.response.StreamingChatResponseHandler() {
            
            // 收集所有部分响应
            StringBuilder fullResponse = new StringBuilder();

            @Override
            public void onPartialResponse(String partialResponse) {
                // 实时打印部分响应（不清除之前的输出）
                System.out.print(partialResponse);
                fullResponse.append(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                // 响应完成后的回调
                System.out.println("\n\n--- 完整响应 ---");
                System.out.println("AI: " + fullResponse);
                System.out.println("Token使用量: " + completeResponse.metadata().tokenUsage());
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                System.err.println("\n错误: " + error.getMessage());
                latch.countDown();
            }
        });

        // 等待响应完成
        latch.await(60, TimeUnit.SECONDS);
    }
}