package com.langchain4j.examples;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * LangChain4j 聊天记忆示例
 * 
 * 本示例演示：
 * 1. MessageWindowChatMemory - 保留最近 N 条消息
 * 2. 自动淘汰旧消息
 * 3. 为不同用户维护独立的聊天记忆
 * 
 * @see <a href="https://langchain4j.cn/tutorials/chat-memory.html">聊天记忆</a>
 */
public class ChatMemoryExample {

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

        // 示例1：基本的聊天记忆
        example1_basicMemory(model);

        // 示例2：限制消息数量（自动淘汰旧消息）
        example2_limitedMemory(model);

        // 示例3：为不同用户维护独立记忆
        example3_separateUserMemory(model);
    }

    /**
     * 示例1：基本的聊天记忆
     * 使用 MessageWindowChatMemory 自动维护对话历史
     */
    static void example1_basicMemory(ChatModel model) {
        System.out.println("\n=== 示例1：基本的聊天记忆 ===");

        ChatMemory memory = MessageWindowChatMemory.builder()
                .id("user-1")
                .maxMessages(20)
                .build();

        // 第一轮对话
        String userMessage1 = "你好，我叫小明";
        memory.add(UserMessage.from(userMessage1));
        ChatResponse response1 = model.chat(memory.messages());
        String answer1 = response1.aiMessage().text();
        memory.add(response1.aiMessage());

        System.out.println("用户: " + userMessage1);
        System.out.println("AI: " + answer1);

        // 第二轮对话（自动携带历史）
        String userMessage2 = "我叫什么名字？";
        memory.add(UserMessage.from(userMessage2));
        ChatResponse response2 = model.chat(memory.messages());
        String answer2 = response2.aiMessage().text();
        memory.add(response2.aiMessage());

        System.out.println("用户: " + userMessage2);
        System.out.println("AI: " + answer2);

        // 查看记忆中的所有消息
        System.out.println("\n当前记忆中的消息数量: " + memory.messages().size());
    }

    /**
     * 示例2：限制消息数量（自动淘汰旧消息）
     * 演示滑动窗口机制
     */
    static void example2_limitedMemory(ChatModel model) {
        System.out.println("\n=== 示例2：限制消息数量 ===");

        // 只保留最近 4 条消息
        ChatMemory memory = MessageWindowChatMemory.builder()
                .id("user-2")
                .maxMessages(4)
                .build();

        // 进行多轮对话
        String[] messages = {"你好", "今天天气怎么样？", "帮我推荐一本书", "这本书的作者是谁？", "再说一个作者"};
        for (String msg : messages) {
            memory.add(UserMessage.from(msg));
            ChatResponse response = model.chat(memory.messages());
            memory.add(response.aiMessage());
            System.out.println("用户: " + msg);
            System.out.println("AI: " + response.aiMessage().text());
            System.out.println("消息数量: " + memory.messages().size() + " (最多4条)");
        }
    }

    /**
     * 示例3：为不同用户维护独立记忆
     * 每个用户有独立的 memoryId
     */
    static void example3_separateUserMemory(ChatModel model) {
        System.out.println("\n=== 示例3：为不同用户维护独立记忆 ===");

        // 为用户 A 创建记忆
        ChatMemory memoryA = MessageWindowChatMemory.builder()
                .id("user-A")
                .maxMessages(20)
                .build();

        // 为用户 B 创建记忆
        ChatMemory memoryB = MessageWindowChatMemory.builder()
                .id("user-B")
                .maxMessages(20)
                .build();

        // 用户 A 的对话
        memoryA.add(UserMessage.from("我叫张三"));
        ChatResponse responseA = model.chat(memoryA.messages());
        memoryA.add(responseA.aiMessage());
        System.out.println("用户A - AI: " + responseA.aiMessage().text());

        // 用户 B 的对话
        memoryB.add(UserMessage.from("我叫李四"));
        ChatResponse responseB = model.chat(memoryB.messages());
        memoryB.add(responseB.aiMessage());
        System.out.println("用户B - AI: " + responseB.aiMessage().text());

        // 继续用户 A 的对话（AI 应该记得张三）
        memoryA.add(UserMessage.from("我叫什么名字？"));
        ChatResponse responseA2 = model.chat(memoryA.messages());
        memoryA.add(responseA2.aiMessage());
        System.out.println("用户A - AI: " + responseA2.aiMessage().text());

        // 继续用户 B 的对话（AI 应该记得李四）
        memoryB.add(UserMessage.from("我叫什么名字？"));
        ChatResponse responseB2 = model.chat(memoryB.messages());
        memoryB.add(responseB2.aiMessage());
        System.out.println("用户B - AI: " + responseB2.aiMessage().text());

        // 验证记忆隔离
        System.out.println("\n用户A消息数: " + memoryA.messages().size());
        System.out.println("用户B消息数: " + memoryB.messages().size());
    }
}