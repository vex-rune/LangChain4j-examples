package com.langchain4j.examples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class _03_ParallelWorkflow {

    // 1. 美食专家Agent
    public interface FoodExpert {
        @Agent("根据氛围推荐餐点")
        @UserMessage("根据「{{mood}}」氛围推荐3道餐点，每行一个餐点名称")
        List<String> findMeal(@V("mood") String mood);
    }

    // 2. 电影专家Agent
    public interface MovieExpert {
        @Agent("根据氛围推荐电影")
        @UserMessage("根据「{{mood}}」氛围推荐3部电影，每行一个电影名称")
        List<String> findMovie(@V("mood") String mood);
    }


    public static void main(String[] args) {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("请配置环境变量 DEEPSEEK_API_KEY");
            return;
        }

        // ExecutorService pool = Executors.newFixedThreadPool(2);

        ChatModel baseModel = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .logRequests(true)   // 打印请求体（完整prompt、参数）
                .logResponses(true)  // 打印大模型返回原始内容
                .build();

        FoodExpert foodExpert = AgenticServices.agentBuilder(FoodExpert.class)
                .chatModel(baseModel)
                .outputKey("meals")
                .build();

        MovieExpert movieExpert = AgenticServices.agentBuilder(MovieExpert.class)
                .chatModel(baseModel)
                .outputKey("movies")
                .build();


        UntypedAgent agent = AgenticServices
                // 1. 并行工作流
                .parallelBuilder()
                // 2. 子工作流
                .subAgents(foodExpert, movieExpert)
                // 3. 线程池
                // .executor(pool)
                .outputKey("plan")
                .output(agenticScope -> {
                    List<String> meals = (List<String>) agenticScope.readState("meals");
                    List<String> movies = (List<String>) agenticScope.readState("movies");
                    return String.format("计划：\n" +
                            "1. 观看电影：%s\n" +
                            "2. 吃美食：%s", movies, meals);
                })
                .build();

        System.out.println("=====================并行工作流开始=====================");


        String mood = "浪漫";
        Object object = agent.invoke(Map.of("mood", mood));

        System.out.println("=====================并行工作流完成=====================");

        System.out.println(object);

        // pool.shutdown();
    }
}
