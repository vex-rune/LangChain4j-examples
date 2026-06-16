package com.langchain4j.examples;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * LangChain4j 快速开始示例 - DeepSeek 模型
 * 
 * DeepSeek API 地址: https://api.deepseek.com
 * 模型: deepseek-chat
 * 
 * @see <a href="https://platform.deepseek.com">DeepSeek 开放平台</a>
 */
public class QuickStartExample {

    public static void main(String[] args) {
        // 从环境变量读取 API Key
        String apiKey = System.getenv("DEEPSEEK_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("请设置环境变量 DEEPSEEK_API_KEY");
            System.out.println("访问 https://platform.deepseek.com 获取 API Key");
            return;
        }

        // 使用 DeepSeek 模型（OpenAI 兼容协议）
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .build();

        // 发送聊天请求
        String userMessage = "请用一句话介绍 LangChain4j";
        System.out.println("用户: " + userMessage);

        String answer = model.chat(userMessage);
        System.out.println("AI: " + answer);
    }
}