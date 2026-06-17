package com.langchain4j.examples;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.*;
import java.util.function.Function;

/**
 * 工具参数类型示例
 * <p>
 * langchain4j 中的工具可以接受各种类型的参数：
 * <p>
 * 1. 基本类型：int, double, boolean 等
 * 2. 对象类型：String, Integer, Double 等
 * 3. 自定义 POJO（可包含嵌套 POJO）
 * 4. enum 枚举
 * 5. 集合类型：List<T>, Set<T>, Map<K,V>
 * <p>
 * 注意：在 langchain4j 1.16.1 中，@Tool 注解在 langchain4j-agentic 模块中，
 * 该模块目前为 beta 版本，API 不稳定。当前使用 Function/Supplier 方式定义工具。
 * <p>
 * 如果未来版本支持 @Tool 注解，可以使用以下方式：
 * ```java
 * public class WeatherTools {
 * @Tool("查询城市天气")
 * String getWeather(@P("city") String city) {
 * return city + "今天天气晴朗";
 * }
 * }
 * ```
 */
public class _01_ToolParameterTypes {

    // =====================================================
    // 定义各种参数类型的工具
    // =====================================================

    /// 工具类, 天气
    public static class WeatherTools {
        @Tool("查询城市天气")
        public String getWeather(@P("city") String city) {
            return city + "今天天气晴朗";
        }

        @Tool("查询城市天气")
        public String getWeather_2 (@P("city") String city, @P("temperature") double temperature, @P("raining") boolean raining) {
            return city + "今天天气晴朗，温度为" + temperature + "°C，" + (raining ? "有雨" : "无雨");
        }
    }

    /// 工具类, 风险评估
    public static class RiskAssessmentTools {
        @Tool("评估风险")
        public String assessRisk(@P("level") String level, @P("reason") String reason) {
            return "评估结果: " + level + ", 原因: " + reason;
        }

        @Tool("评估风险")
        public String assessRisk_2 (@P("level") RiskLevel level, @P("reason") String reason) {
            return "评估结果: " + level + ", reasion: " + reason;
        }

        public enum RiskLevel {
            LOW, MEDIUM, HIGH
        }
    }

    /// 工具类, 返回对象的参数
    public static class ReturnObjectTools {
        @Tool("返回对象")
        public Person returnObject(@P("name") String name) {
            return new Person(name);
        }

        public record Person(String name) {
        }
    }

    /// 集合返回
    public static class ReturnCollectionTools {
        @Tool("返回集合")
        public List<String> returnCollection(@P("items") List<String> items) {
            return items;
        }
    }


    // =====================================================
    // 助手接口
    // =====================================================

    public interface Assistant {
        @UserMessage("调用所有的工具, 验证工具是否可以成功运行, 并完成{{userMessage}}的提问, 最后要总结用了那些工具, 工具都输出了什么")
        String chat(@V("userMessage") String userMessage);
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

        System.out.println("=====================工具参数类型示例=====================");
        System.out.println();
        System.out.println("提示：当前使用 Function<Map<String,Object>, T> 方式定义工具");
        System.out.println("      当 @Tool 注解稳定后，可使用注解方式");
        System.out.println();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(
                        new WeatherTools(),
                        new RiskAssessmentTools(),
                        new ReturnObjectTools(),
                        new ReturnCollectionTools()
                )
                .build();

        // 测试用例
        String[] queries = {
                "查询北京天气，温度25度，不下雨",
                "评估这个交易的风险等级是HIGH，原因是大额转账",
                "处理这些商品: [苹果, 香蕉, 橙子]，设置: {vip: true}，标签: {水果, 新鲜}",
                "用户张三28岁，住在北京朝阳区",
                "注册用户：王五，年龄30岁"
        };

        for (int i = 0; i < queries.length; i++) {
            System.out.println("──────────────────────────────────────");
            System.out.println("【测试 " + (i + 1) + "】");
            System.out.println("用户: " + queries[i]);
            String response = assistant.chat(queries[i]);
            System.out.println("回答: " + response);
            System.out.println();
        }

        System.out.println("=====================执行完成=====================");
    }
}
