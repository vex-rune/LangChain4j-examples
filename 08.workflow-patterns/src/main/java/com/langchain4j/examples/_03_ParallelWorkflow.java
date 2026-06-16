package com.langchain4j.examples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

public class _03_ParallelWorkflow {

    // 1. 美食专家Agent
    public interface FoodExpert {
        @Agent("根据氛围推荐餐点")
        @UserMessage("根据「{{mood}}」氛围推荐3道餐点，每行一个餐点名称")
        List<String> findMeal(String mood);
    }

    // 2. 电影专家Agent
    public interface MovieExpert {
        @Agent("根据氛围推荐电影")
        @UserMessage("根据「{{mood}}」氛围推荐3部电影，每行一个电影名称")
        List<String> findMovie(String mood);
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

        FoodExpert foodExpert = AgenticServices.agentBuilder(FoodExpert.class)
                .chatModel(baseModel)
                .outputKey("meals")
                .build();

        MovieExpert movieExpert = AgenticServices.agentBuilder(MovieExpert.class)
                .chatModel(baseModel)
                .outputKey("movies")
                .build();

        String mood = "浪漫";

        System.out.println("=====================并行工作流开始=====================");
        
        List<String> meals = foodExpert.findMeal(mood);
        List<String> movies = movieExpert.findMovie(mood);
        
        System.out.println("=====================并行工作流完成=====================");
        System.out.println("推荐餐点:");
        meals.forEach(System.out::println);
        System.out.println("\n推荐电影:");
        movies.forEach(System.out::println);
    }
}
