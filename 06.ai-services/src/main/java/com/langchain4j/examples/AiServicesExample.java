package com.langchain4j.examples;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

/**
 * LangChain4j AI 服务示例
 * 
 * 本示例演示：
 * 1. 创建简单的 AI 服务接口
 * 2. 使用 @SystemMessage 定义系统提示
 * 3. 使用 @UserMessage 和 @V 注解
 * 4. 结构化输出（返回 JSON 字符串）
 * 5. 聊天记忆
 * 
 * @see <a href="https://langchain4j.cn/tutorials/ai-services.html">AI 服务</a>
 */
public class AiServicesExample {

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

        // 示例1：简单的 AI 服务
        example1_simpleAssistant(model);

        // 示例2：使用 @SystemMessage
        example2_systemMessage(model);

        // 示例3：使用 @UserMessage 和 @V
        example3_userMessage(model);

        // 示例4：结构化输出
        example4_structuredOutput(model);

        // 示例5：聊天记忆
        example5_chatMemory(model);
    }

    /**
     * 示例1：简单的 AI 服务
     */
    static void example1_simpleAssistant(ChatModel model) {
        System.out.println("\n=== 示例1：简单的 AI 服务 ===");

        interface Assistant {
            String chat(String userMessage);
        }

        Assistant assistant = AiServices.create(Assistant.class, model);
        String answer = assistant.chat("你好，请介绍一下自己");
        System.out.println("AI: " + answer);
    }

    /**
     * 示例2：使用 @SystemMessage
     */
    static void example2_systemMessage(ChatModel model) {
        System.out.println("\n=== 示例2：@SystemMessage ===");

        interface FriendlyAssistant {
            @dev.langchain4j.service.SystemMessage("你是一个友善的助手，总是使用emoji回答问题")
            String chat(String userMessage);
        }

        FriendlyAssistant assistant = AiServices.create(FriendlyAssistant.class, model);
        String answer = assistant.chat("你好");
        System.out.println("AI: " + answer);
    }

    /**
     * 示例3：使用 @UserMessage 和 @V
     */
    static void example3_userMessage(ChatModel model) {
        System.out.println("\n=== 示例3：@UserMessage 和 @V ===");

        interface CountryAssistant {
            @dev.langchain4j.service.UserMessage("{{country}} 的首都是什么？")
            String capitalOf(@dev.langchain4j.service.V("country") String country);
        }

        CountryAssistant assistant = AiServices.create(CountryAssistant.class, model);
        String answer = assistant.capitalOf("日本");
        System.out.println("AI: " + answer);
    }

    /**
     * 示例4：结构化输出
     * 返回 JSON 字符串，手动解析（避免 LangChain4j 解析器兼容性问题）
     */
    static void example4_structuredOutput(ChatModel model) {
        System.out.println("\n=== 示例4：结构化输出 ===");

        interface PersonExtractor {
            @dev.langchain4j.service.UserMessage(
                "从以下文本中提取人物信息，返回JSON格式：{\"name\":\"姓名\",\"age\":年龄,\"city\":\"城市\"}\n文本：{{text}}"
            )
            String extractPerson(@dev.langchain4j.service.V("text") String text);
        }

        PersonExtractor extractor = AiServices.create(PersonExtractor.class, model);

        String text = "张三是一个25岁的程序员，他住在上海工作";
        String json = extractor.extractPerson(text);
        System.out.println("提取结果（JSON）: " + json);
        
        // 提示：实际项目中可用 Jackson 解析 json 字符串
        System.out.println("提示：生产环境建议用 Jackson 解析 JSON");
    }

    /**
     * 示例5：聊天记忆
     */
    static void example5_chatMemory(ChatModel model) {
        System.out.println("\n=== 示例5：聊天记忆 ===");

        interface Chatbot {
            String chat(@dev.langchain4j.service.MemoryId int memoryId, 
                       @dev.langchain4j.service.UserMessage String message);
        }

        Chatbot chatbot = AiServices.builder(Chatbot.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();

        System.out.println("用户1: 你好，我叫小明");
        String answer1 = chatbot.chat(1, "你好，我叫小明");
        System.out.println("AI: " + answer1);

        System.out.println("用户1: 我叫什么名字？");
        String answer2 = chatbot.chat(1, "我叫什么名字？");
        System.out.println("AI: " + answer2);

        System.out.println("用户2: 你好，我叫小红");
        String answer3 = chatbot.chat(2, "你好，我叫小红");
        System.out.println("AI: " + answer3);

        System.out.println("用户2: 我叫什么名字？");
        String answer4 = chatbot.chat(2, "我叫什么名字？");
        System.out.println("AI: " + answer4);
    }
}