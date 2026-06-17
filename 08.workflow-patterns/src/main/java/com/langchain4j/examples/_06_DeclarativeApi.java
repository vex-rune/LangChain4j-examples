package com.langchain4j.examples;

import dev.langchain4j.agentic.*;
import dev.langchain4j.agentic.declarative.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class _06_DeclarativeApi {

    // =====================================================
    // 定义数据类型
    // =====================================================

    public record EveningPlan(String movie, String meal) {
    }

    public enum RequestCategory {
        LEGAL, MEDICAL, TECH, OTHER
    }

    // =====================================================
    // 声明式并行工作流：晚间规划Agent
    // =====================================================

    // 1. 美食专家Agent
    public interface FoodExpert {
        @Agent(name = "美食专家",outputKey =  "meals")
        @UserMessage("根据「{{mood}}」氛围推荐3道餐点，每行一个餐点名称")
            // 编译时保留参数名, 就可以不使用 @V
        List<String> findMeal(String mood);
    }

    // 2. 电影专家Agent
    public interface MovieExpert {
        @Agent(name = "电影专家",outputKey =  "movies")
        @UserMessage("根据「{{mood}}」氛围推荐3部电影，每行一个电影名称")
        List<String> findMovie(@V("mood") String mood);

    }

    // 3. 使用声明式API定义晚间规划Agent
    public interface EveningPlannerAgent {
        // @ParallelAgent 注解定义并行执行
        @ParallelAgent(outputKey = "plans", subAgents = {FoodExpert.class, MovieExpert.class})
        @SystemMessage("""
                将用户请求分类，仅输出对应英文单词，禁止中文：
                摔伤就医 → MEDICAL
                合同纠纷 → LEGAL
                编程问题 → TECH
                其他问题 → OTHER
                请求：{{request}}
                """)
        List<EveningPlan> plan(@V("mood") String mood);

        // @Output 定义如何合并结果
        @Output
        static List<EveningPlan> createPlans(@V("movies") List<String> movies, @V("meals") List<String> meals) {
            List<EveningPlan> plans = new ArrayList<>();
            for (int i = 0; i < Math.min(movies.size(), meals.size()); i++) {
                plans.add(new EveningPlan(movies.get(i), meals.get(i)));
            }
            return plans;
        }
    }

    // =====================================================
    // 声明式条件工作流：专家问答Agent
    // =====================================================

    // 4. 分类路由Agent
    public interface CategoryRouter {
        @Agent(name = "分类路由")
        @UserMessage("分析以下用户请求，分类为：法律/医疗/技术/其他。\n只输出一个词。\n请求：{{request}}")
        RequestCategory classify(@V("request") String request);
    }

    // 5. 医疗专家Agent
    public interface MedicalExpert {
        @Agent(name = "医疗专家")
        @UserMessage("你是一个医疗健康专家。请回答：{{request}}")
        String medical(@V("request") String request);
    }

    // 6. 法律专家Agent
    public interface LegalExpert {
        @Agent(name = "法律专家")
        @UserMessage("你是一个法律顾问专家。请回答：{{request}}")
        String legal(@V("request") String request);
    }

    // 7. 技术专家Agent
    public interface TechnicalExpert {
        @Agent(name = "技术支持专家")
        @UserMessage("你是一个技术支持专家。请回答：{{request}}")
        String technical(@V("request") String request);
    }

    // 8. 使用声明式API定义专家路由Agent
    public interface ExpertRouterAgent {
        // 先分类
        @SequenceAgent(outputKey = "category", subAgents = {CategoryRouter.class})
        RequestCategory categorize(@V("request") String request);

        // 再根据分类选择专家
        @ConditionalAgent(outputKey = "response", subAgents = {MedicalExpert.class, TechnicalExpert.class, LegalExpert.class})
        String ask(@V("request") String request);

        // @ActivationCondition 定义激活条件
        @ActivationCondition(MedicalExpert.class)
        static boolean activateMedical(@V("category") RequestCategory category) {
            return category == RequestCategory.MEDICAL;
        }

        @ActivationCondition(TechnicalExpert.class)
        static boolean activateTechnical(@V("category") RequestCategory category) {
            return category == RequestCategory.TECH;
        }

        @ActivationCondition(LegalExpert.class)
        static boolean activateLegal(@V("category") RequestCategory category) {
            return category == RequestCategory.LEGAL;
        }
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

        ChatModel baseModel = OpenAiChatModel.builder().baseUrl("https://api.deepseek.com").apiKey(apiKey).modelName("deepseek-chat").logRequests(true).build();

        System.out.println("=====================声明式API示例=====================");
        System.out.println();

        // =====================================================
        // 示例1：声明式并行工作流
        // =====================================================
        System.out.println("【示例1】声明式并行工作流 - 晚间规划");
        System.out.println("使用 @ParallelAgent、@ExecutorService、@Output 注解");
        System.out.println();

        EveningPlannerAgent plannerAgent = AgenticServices.createAgenticSystem(EveningPlannerAgent.class, baseModel);

        List<EveningPlan> plans = plannerAgent.plan("浪漫");

        System.out.println("\n晚间浪漫计划:");
        for (int i = 0; i < plans.size(); i++) {
            EveningPlan plan = plans.get(i);
            System.out.println((i + 1) + ". 电影: " + plan.movie() + " | 餐点: " + plan.meal());
        }

        System.out.println("\n\n=====================声明式API示例=====================");
        System.out.println();

        // =====================================================
        // 示例2：声明式条件工作流
        // =====================================================
        System.out.println("【示例2】声明式条件工作流 - 专家问答");
        System.out.println("使用 @SequenceAgent、@ConditionalAgent、@ActivationCondition 注解");
        System.out.println();

        ExpertRouterAgent expertRouterAgent = AgenticServices.createAgenticSystem(ExpertRouterAgent.class, baseModel);

        String[] requests = {"我摔断了腿，应该怎么办？", "如何用Java写一个类？", "今天天气怎么样？"};

        for (String request : requests) {
            System.out.println("问题: " + request);
            String response = expertRouterAgent.ask(request);
            System.out.println("回答: " + response);
            System.out.println();
        }

        System.out.println("=====================执行完成=====================");
    }
}
