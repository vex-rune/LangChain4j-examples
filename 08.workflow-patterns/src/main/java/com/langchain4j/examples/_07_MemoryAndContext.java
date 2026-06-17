package com.langchain4j.examples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.Map;

public class _07_MemoryAndContext {

    // =====================================================
    // 定义数据类型
    // =====================================================

    public enum RequestCategory {
        LEGAL, MEDICAL, TECH, OTHER
    }

    // =====================================================
    // 带内存的Agent
    // =====================================================
    
    // 1. 医疗专家Agent（带内存）
    public interface MedicalExpertWithMemory {
        @Agent(name = "医疗专家", outputKey = "response")
        @UserMessage("你是一个医疗健康专家。回答用户问题：{{request}}")
        String medical(@MemoryId String memoryId, @V("request") String request);
    }

    // 2. 法律专家Agent（带内存）
    public interface LegalExpertWithMemory {
        @Agent(name = "法律专家", outputKey = "response")
        @UserMessage("你是一个法律顾问专家。回答用户问题：{{request}}")
        String legal(@MemoryId String memoryId, @V("request") String request);
    }

    // 3. 技术专家Agent（带内存）
    public interface TechnicalExpertWithMemory {
        @Agent(name = "技术专家", outputKey = "response")
        @UserMessage("你是一个技术支持专家。回答用户问题：{{request}}")
        String technical(@MemoryId String memoryId, @V("request") String request);
    }

    // 4. 分类路由Agent
    public interface CategoryRouter {
        @Agent(name = "分类路由", outputKey = "category")
        @UserMessage("分析用户请求，分类为：MEDICAL/LEGAL/TECH/OTHER\n只输出英文单词\n请求：{{request}}")
        RequestCategory classify(@V("request") String request);
    }

    // 5. 上下文总结Agent
    public interface ContextSummarizer {
        @Agent(name = "上下文总结", outputKey = "summary")
        @UserMessage("将以下对话总结成2句话的摘要：\n{{it}}")
        String summarize(String conversation);
    }

    // =====================================================
    // 主方法
    // =====================================================
    
    public static void main(String[] args) {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("请配置环境变量 DEEPSEEK_API_KEY");
            return;
        }

        ChatModel baseModel = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .logRequests(true)
                .logResponses(true)
                .build();

        // =====================================================
        // 示例1：基础内存 - 同一对话保持上下文
        // =====================================================
        System.out.println("=====================示例1：基础内存=====================");
        System.out.println("同一个 memoryId 的对话会保持上下文");
        System.out.println();

        MedicalExpertWithMemory medicalExpert = AgenticServices.agentBuilder(MedicalExpertWithMemory.class)
                .chatModel(baseModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .outputKey("response")
                .build();

        System.out.println("【问题1】我摔断了腿，应该怎么办？");
        String answer1 = medicalExpert.medical("user-1", "我摔断了腿，应该怎么办？");
        System.out.println("回答: " + answer1);
        System.out.println();

        System.out.println("【问题2】需要多久才能康复？");
        String answer2 = medicalExpert.medical("user-1", "需要多久才能康复？");
        System.out.println("回答: " + answer2);
        System.out.println("（AI应该记得之前的对话）");

        // =====================================================
        // 示例2：不同memoryId 上下文不共享
        // =====================================================
        System.out.println("\n\n=====================示例2：不同memoryId=====================");
        System.out.println("不同的 memoryId 有独立的上下文");
        System.out.println();

        System.out.println("【用户A】我头痛，应该怎么办？");
        String userAAnswer = medicalExpert.medical("user-A", "我头痛，应该怎么办？");
        System.out.println("回答: " + userAAnswer);

        System.out.println("\n【用户B】我胃痛，应该怎么办？");
        String userBAnswer = medicalExpert.medical("user-B", "我胃痛，应该怎么办？");
        System.out.println("回答: " + userBAnswer);
        System.out.println("（用户A和用户B的对话互不影响）");

        // =====================================================
        // 示例3：专家问答系统（带内存）
        // =====================================================
        System.out.println("\n\n=====================示例3：专家问答系统（带内存）=====================");
        System.out.println("分类 → 选择专家 → 回答");
        System.out.println();

        // 创建各专家（带内存）
        MedicalExpertWithMemory medExpert = AgenticServices.agentBuilder(MedicalExpertWithMemory.class)
                .chatModel(baseModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .outputKey("response")
                .build();

        LegalExpertWithMemory legalExpert = AgenticServices.agentBuilder(LegalExpertWithMemory.class)
                .chatModel(baseModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .outputKey("response")
                .build();

        TechnicalExpertWithMemory techExpert = AgenticServices.agentBuilder(TechnicalExpertWithMemory.class)
                .chatModel(baseModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .outputKey("response")
                .build();

        CategoryRouter router = AgenticServices.agentBuilder(CategoryRouter.class)
                .chatModel(baseModel)
                .outputKey("category")
                .build();

        // 创建条件工作流
        dev.langchain4j.agentic.UntypedAgent expertsSystem = AgenticServices.conditionalBuilder()
                .subAgents(scope -> scope.readState("category", RequestCategory.OTHER) == RequestCategory.MEDICAL, medExpert)
                .subAgents(scope -> scope.readState("category", RequestCategory.OTHER) == RequestCategory.LEGAL, legalExpert)
                .subAgents(scope -> scope.readState("category", RequestCategory.OTHER) == RequestCategory.TECH, techExpert)
                .build();

        dev.langchain4j.agentic.UntypedAgent expertRouter = AgenticServices.sequenceBuilder()
                .subAgents(router, expertsSystem)
                .outputKey("response")
                .build();

        // 测试请求
        String[] requests = {
                "我摔断了腿，应该怎么办？",
                "如何用Java写一个类？",
                "今天天气怎么样？"
        };

        for (String request : requests) {
            System.out.println("问题: " + request);
            String response = (String) expertRouter.invoke(Map.of("request", request));
            System.out.println("回答: " + response);
            System.out.println();
        }

        System.out.println("=====================执行完成=====================");
    }
}
