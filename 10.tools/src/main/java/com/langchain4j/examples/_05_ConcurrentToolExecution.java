package com.langchain4j.examples;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * 并发执行工具示例
 * <p>
 * 当 LLM 需要调用多个独立的工具时，可以并发执行以提高效率。
 * <p>
 * 使用 executeToolsConcurrently() 启用并发执行。
 */
public class _05_ConcurrentToolExecution {

    // =====================================================
    // 定义耗时工具（模拟 API 调用）
    // =====================================================

    static class CalculatorTools {

        // 天气
        @Tool("查询天气")
        String queryWeather(String city) {
            System.out.println("  [queryWeather] 查询天气: " + city);
            return String.format("查询结果: %s天气晴朗, 气温 25°C", city);
        }

        // 最新新闻
        @Tool("查询最新新闻")
        String queryNews(String time) {
            System.out.println("  [queryNews] 查询最新新闻: " + time);
            return "查询结果: 最新新闻 = AI 技术快速发展";
        }

        // 获取股票行情
        @Tool("获取股票行情")
        String queryStock(String symbol) {
            System.out.println("  [queryStock] 获取股票行情: " + symbol);
            return String.format("查询结果: %s股票行情 = 3000.00", symbol);
        }
    }

    // =====================================================
    // 助手接口
    // =====================================================

    interface Assistant {
        @UserMessage("回答用户问题：{{it}}")
        String chat(String message);
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

        System.out.println("=====================并发执行工具示例=====================");
        System.out.println();

        // 创建支持并发执行的助手
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(new CalculatorTools())
                // 启用并发执行，自定义线程池
                .executeToolsConcurrently()
                .build();

        // 测试场景：同时查询多个不相关的信息
        System.out.println("【测试】同时查询多个不相关的信息");
        System.out.println();

        long start = System.currentTimeMillis();
        String response = assistant.chat(
                "请同时帮我查询北京和上海的天气，科技新闻，以及腾讯股票行情"
        );
        long duration = System.currentTimeMillis() - start;

        System.out.println();
        System.out.println("──────────────────────────────────────");
        System.out.println("回答: " + response);
        System.out.println("──────────────────────────────────────");
        System.out.println("总耗时: " + duration + "ms");
        System.out.println();

        if (duration < 2500) {
            System.out.println("✅ 成功！工具并发执行，耗时 < 3秒（每个工具单独执行需要 ~3秒）");
        } else {
            System.out.println("⚠️ 工具串行执行，耗时较长");
        }

        System.out.println();
        System.out.println("=====================执行完成=====================");
        System.out.println();
        System.out.println("提示：并发执行适用场景：");
        System.out.println("1. 多个不相关的 API 调用");
        System.out.println("2. 每个工具耗时较长（如网络请求）");
        System.out.println("3. 工具之间没有依赖关系");
    }
}
