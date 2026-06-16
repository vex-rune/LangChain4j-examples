package com.langchain4j.examples;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * LangChain4j 聊天与语言模型示例
 * 
 * 本示例演示：
 * 1. ChatMessage 的类型（UserMessage, AiMessage, SystemMessage）
 * 2. 多个 ChatMessage（多轮对话）
 * 
 * @see <a href="https://langchain4j.cn/tutorials/chat-and-language-models.html">聊天与语言模型</a>
 */
public class ChatAndLanguageModelsExample {

    public static void main(String[] args) {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("请设置环境变量 DEEPSEEK_API_KEY");
            return;
        }

        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .build();

        // 示例1：简单聊天
        example1_simpleChat(model);

        // 示例2：使用 UserMessage 和 ChatResponse
        example2_userMessage(model);

        // 示例3：多轮对话
        example3_multiTurn(model);

        // 示例4：带 SystemMessage
        example4_systemMessage(model);
    }

    /**
     * 示例1：简单聊天
     */
    static void example1_simpleChat(ChatModel model) {
        System.out.println("\n=== 示例1：简单聊天 ===");
        String answer = model.chat("请用一句话介绍自己");
        System.out.println("AI: " + answer);
    }

    /**
     * 示例2：使用 UserMessage 和 ChatResponse
     */
    static void example2_userMessage(ChatModel model) {
        System.out.println("\n=== 示例2：UserMessage 和 ChatResponse ===");
        UserMessage userMessage = UserMessage.from("你好");

        ChatResponse response = model.chat(userMessage);
        System.out.println("AI: " + response.aiMessage().text());
        System.out.println("Token 使用量: " + response.metadata().tokenUsage());
    }

    /**
     * 示例3：多轮对话
     */
    static void example3_multiTurn(ChatModel model) {
        System.out.println("\n=== 示例3：多轮对话 ===");

        // 第一轮
        UserMessage firstUserMessage = UserMessage.from("你好，我叫小明");
        ChatResponse firstResponse = model.chat(firstUserMessage);
        System.out.println("用户: 你好，我叫小明");
        System.out.println("AI: " + firstResponse.aiMessage().text());

        // 第二轮（携带历史消息）
        UserMessage secondUserMessage = UserMessage.from("我叫什么名字？");
        ChatResponse secondResponse = model.chat(
            firstUserMessage, 
            firstResponse.aiMessage(), 
            secondUserMessage
        );
        System.out.println("用户: 我叫什么名字？");
        System.out.println("AI: " + secondResponse.aiMessage().text());
    }

    /**
     * 示例4：使用 SystemMessage
     */
    static void example4_systemMessage(ChatModel model) {
        System.out.println("\n=== 示例4：SystemMessage ===");

        dev.langchain4j.data.message.SystemMessage systemMessage = 
            dev.langchain4j.data.message.SystemMessage.from(
                "你是一个友善的助手，总是使用emoji回答问题"
            );
        UserMessage userMessage = UserMessage.from("你好");

        ChatResponse response = model.chat(systemMessage, userMessage);
        System.out.println("AI: " + response.aiMessage().text());
    }
}