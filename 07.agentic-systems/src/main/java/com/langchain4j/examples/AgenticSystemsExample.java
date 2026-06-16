package com.langchain4j.examples;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

/**
 * LangChain4j Agentic Systems 示例
 * 
 * 本示例演示：
 * 1. Agent 的概念（使用 LLM 执行特定任务）
 * 2. 带工具的 Agent（Function Calling）
 * 3. 多 Agent 协作工作流
 * 
 * @see <a href="https://langchain4j.cn/tutorials/ai-services.html">AI 服务</a>
 */
public class AgenticSystemsExample {

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

        // 示例1：简单 Agent（单任务）
        example1_simpleAgent(model);

        // 示例2：多 Agent 协作
        example2_multiAgent(model);
    }

    /**
     * 示例1：简单 Agent
     * 定义一个 Agent 执行特定任务
     */
    static void example1_simpleAgent(ChatModel model) {
        System.out.println("\n=== 示例1：简单 Agent ===");

        // 定义创意写作 Agent
        interface CreativeWriter {
            @dev.langchain4j.service.UserMessage(
                "你是一个创意作家。根据给定的主题，写一个不超过3句话的故事。只返回故事内容。\n" +
                "主题：{{topic}}"
            )
            String writeStory(@dev.langchain4j.service.V("topic") String topic);
        }

        CreativeWriter writer = AiServices.create(CreativeWriter.class, model);

        String story = writer.writeStory("程序员与bug");
        System.out.println("故事：\n" + story);
    }

    /**
     * 示例2：多 Agent 协作
     * 不同的 Agent 负责不同任务，协同工作
     */
    static void example2_multiAgent(ChatModel model) {
        System.out.println("\n=== 示例2：多 Agent 协作 ===");

        // 定义研究 Agent
        interface Researcher {
            @dev.langchain4j.service.UserMessage(
                "从以下主题中提取3-5个关键信息点，用 bullet points 列出：\n{{topic}}"
            )
            String research(@dev.langchain4j.service.V("topic") String topic);
        }

        // 定义总结 Agent
        interface Summarizer {
            @dev.langchain4j.service.UserMessage(
                "将以下要点改写成简洁的中文摘要：\n{{keyPoints}}"
            )
            String summarize(@dev.langchain4j.service.V("keyPoints") String keyPoints);
        }

        // 创建各个 Agent
        Researcher researcher = AiServices.create(Researcher.class, model);
        Summarizer summarizer = AiServices.create(Summarizer.class, model);

        String topic = "人工智能对软件开发的影响";

        System.out.println("=== 工作流开始 ===");
        System.out.println("主题: " + topic);

        // 步骤1：研究
        System.out.println("\n[步骤1] 研究 Agent...");
        String keyPoints = researcher.research(topic);
        System.out.println("研究结果:\n" + keyPoints);

        // 步骤2：总结
        System.out.println("\n[步骤2] 总结 Agent...");
        String summary = summarizer.summarize(keyPoints);
        System.out.println("总结结果:\n" + summary);

        System.out.println("\n=== 工作流完成 ===");
    }
}