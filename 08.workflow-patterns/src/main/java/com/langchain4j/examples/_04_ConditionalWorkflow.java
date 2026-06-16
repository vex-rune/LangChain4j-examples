package com.langchain4j.examples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.Map;

public class _04_ConditionalWorkflow {

    // 枚举：请求分类
    enum RequestCategory {
        法律, 医疗, 技术, 其他
    }

    // 1. 分类路由Agent
    public interface CategoryRouter {
        @Agent("对用户请求进行分类")
        @UserMessage("分析以下用户请求，分类为：法律/医疗/技术/其他。\n只输出一个词。\n请求：{{request}}")
        RequestCategory classify(@V("request") String request);
    }

    // 2. 医疗专家Agent
    public interface MedicalExpert {
        @Agent("医疗健康专家")
        @UserMessage("你是一个医疗健康专家。请回答：{{request}}")
        String medical(@V("request") String request);
    }

    // 3. 法律专家Agent
    public interface LegalExpert {
        @Agent("法律顾问专家")
        @UserMessage("你是一个法律顾问专家。请回答：{{request}}")
        String legal(@V("request") String request);
    }

    // 4. 技术专家Agent
    public interface TechnicalExpert {
        @Agent("技术支持专家")
        @UserMessage("你是一个技术支持专家。请回答：{{request}}")
        String technical(@V("request") String request);
    }

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
                .build();

        CategoryRouter router = AgenticServices.agentBuilder(CategoryRouter.class)
                .chatModel(baseModel)
                .outputKey("category")
                .build();

        MedicalExpert medicalExpert = AgenticServices.agentBuilder(MedicalExpert.class)
                .chatModel(baseModel)
                .outputKey("response")
                .build();

        LegalExpert legalExpert = AgenticServices.agentBuilder(LegalExpert.class)
                .chatModel(baseModel)
                .outputKey("response")
                .build();

        TechnicalExpert technicalExpert = AgenticServices.agentBuilder(TechnicalExpert.class)
                .chatModel(baseModel)
                .outputKey("response")
                .build();

        dev.langchain4j.agentic.UntypedAgent expertsAgent = AgenticServices.conditionalBuilder()
                .subAgents(scope -> scope.readState("category", RequestCategory.其他) == RequestCategory.医疗, medicalExpert)
                .subAgents(scope -> scope.readState("category", RequestCategory.其他) == RequestCategory.法律, legalExpert)
                .subAgents(scope -> scope.readState("category", RequestCategory.其他) == RequestCategory.技术, technicalExpert)
                .build();

        dev.langchain4j.agentic.UntypedAgent expertRouter = AgenticServices.sequenceBuilder()
                .subAgents(router, expertsAgent)
                .outputKey("response")
                .build();

        // 测试请求
        String[] requests = {
                "我摔断了腿，应该怎么办？",
                "如何用Java写一个类？",
                "今天天气怎么样？"
        };

        System.out.println("=====================条件工作流开始=====================");
        for (String request : requests) {
            System.out.println("\n问题: " + request);
            String response = (String) expertRouter.invoke(Map.of("request", request));
            System.out.println("回答: " + response);
        }
        System.out.println("=====================条件工作流完成=====================");
    }
}
