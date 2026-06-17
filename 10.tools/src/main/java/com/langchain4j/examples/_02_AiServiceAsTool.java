package com.langchain4j.examples;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AI Service 作为工具示例
 * 
 * 一个 AI Service 可以作为工具供其他 AI Service 使用。
 * 
 * 例如：路由器 Agent 可以根据请求类型将请求路由到不同的专家 Agent
 */
public class _02_AiServiceAsTool {

    // =====================================================
    // 路由器 Agent
    // =====================================================
    
    interface RouterAgent {
        @UserMessage("""
            分析以下用户请求，分类为 'medical'（医疗）、'legal'（法律）或 'technical'（技术）。
            然后将请求原样转发给相应的专家工具。
            最后直接返回专家的回答，不要做任何修改。
            
            用户请求：{{it}}
            """)
        String askToExpert(String request);
    }

    // =====================================================
    // 医疗专家 Agent
    // =====================================================
    
    interface MedicalExpert {
        @UserMessage("你是一个医疗专家。分析以下问题并给出医疗建议：\n{{request}}")
        @Tool("医疗专家")
        String medicalRequest(@V("request") String request);
    }

    // =====================================================
    // 法律专家 Agent
    // =====================================================
    
    interface LegalExpert {
        @UserMessage("你是一个法律专家。分析以下问题并给出法律建议：\n{{request}}")
        @Tool("法律专家")
        String legalRequest(@V("request") String request);
    }

    // =====================================================
    // 技术专家 Agent
    // =====================================================
    
    interface TechnicalExpert {
        @UserMessage("你是一个技术专家。分析以下问题并给出技术建议：\n{{request}}")
        @Tool("技术专家")
        String technicalRequest(@V("request") String request);
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

        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .logRequests(true)
                .build();

        System.out.println("=====================AI Service 作为工具示例=====================");
        System.out.println();

        // 创建3个专家 Agent
        MedicalExpert medicalExpert = AiServices.builder(MedicalExpert.class)
                .chatModel(model)
                .build();
        
        LegalExpert legalExpert = AiServices.builder(LegalExpert.class)
                .chatModel(model)
                .build();
        
        TechnicalExpert technicalExpert = AiServices.builder(TechnicalExpert.class)
                .chatModel(model)
                .build();

        // 创建路由器 Agent，将专家作为工具
        RouterAgent routerAgent = AiServices.builder(RouterAgent.class)
                .chatModel(model)
                .tools(medicalExpert, legalExpert, technicalExpert)
                .build();

        // 测试用例
        String[] queries = {
                "我摔断了腿，应该怎么办？",
                "邻居装修太吵，可以起诉他吗？",
                "如何用Java写一个线程安全的单例模式？"
        };

        for (int i = 0; i < queries.length; i++) {
            System.out.println("──────────────────────────────────────");
            System.out.println("【测试 " + (i + 1) + "】");
            System.out.println("用户: " + queries[i]);
            System.out.println();
            
            String response = routerAgent.askToExpert(queries[i]);
            System.out.println("回答: " + response);
            System.out.println();
        }

        System.out.println("=====================执行完成=====================");
    }
}
