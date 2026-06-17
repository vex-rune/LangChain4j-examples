package com.langchain4j.examples;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.tool.ToolExecutionErrorHandler;
import dev.langchain4j.service.tool.ToolErrorContext;
import dev.langchain4j.service.tool.ToolErrorHandlerResult;

/**
 * 工具异常处理示例
 * 
 * 演示如何处理工具执行中的错误：
 * 1. 工具参数错误
 * 2. 工具执行异常
 * 3. 使用 ToolExecutionErrorHandler 捕获并处理异常
 */
public class _03_ToolErrorHandling {

    // =====================================================
    // 定义计算器工具类
    // =====================================================
    
    static class CalculatorTools {
        
        // 除法工具 - 可能抛出除零异常
        @Tool("计算器 - 执行除法运算")
        String divide(double a, double b) {
            System.out.println("  [divide] 计算: " + a + " / " + b);
            if (b == 0) {
                throw new ArithmeticException("除数不能为零");
            }
            return String.valueOf(a / b);
        }
        
        // 数据库查询工具 - 模拟连接异常
        @Tool("数据库查询 - 根据表名和ID查询")
        String queryDatabase(String table, int id) {
            System.out.println("  [queryDatabase] 查询表: " + table + ", ID: " + id);
            if (table.equals("users") && id == 999) {
                throw new RuntimeException("数据库连接失败: Connection refused");
            }
            return String.format("查询结果: %s表ID=%d = {name: '张三', age: 25}", table, id);
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

        System.out.println("=====================工具异常处理示例=====================");
        System.out.println();

        // 创建助手，配置错误处理器
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(new CalculatorTools())
                // 配置工具执行错误处理器
                .toolExecutionErrorHandler((throwable, errorContext) -> {
                    System.out.println();
                    System.out.println("【错误处理器】捕获到异常:");
                    System.out.println("  工具名: " + errorContext.toolExecutionRequest().name());
                    System.out.println("  参数: " + errorContext.toolExecutionRequest().arguments());
                    System.out.println("  异常类型: " + throwable.getClass().getSimpleName());
                    System.out.println("  异常消息: " + throwable.getMessage());

                    // 返回错误消息，而不是抛出异常
                    // 这样 AI 可以继续处理，而不是直接失败
                    String errorMessage = "执行错误: " + throwable.getMessage();
                    return ToolErrorHandlerResult.text(errorMessage);
                })
                .build();

        // 测试用例
        String[] queries = {
                "计算 100 除以 0 等于多少？",
                "计算 50 除以 2 等于多少？",
                "查询 users 表 ID=999 的数据"
        };

        for (int i = 0; i < queries.length; i++) {
            System.out.println("──────────────────────────────────────");
            System.out.println("【测试 " + (i + 1) + "】");
            System.out.println("用户: " + queries[i]);
            System.out.println();
            
            try {
                String response = assistant.chat(queries[i]);
                System.out.println("回答: " + response);
            } catch (Exception e) {
                System.out.println("发生异常: " + e.getMessage());
            }
            System.out.println();
        }

        System.out.println("=====================执行完成=====================");
        System.out.println();
        System.out.println("提示：错误处理器可以：");
        System.out.println("1. 记录错误日志");
        System.out.println("2. 返回友好的错误消息给 AI");
        System.out.println("3. 提供备选方案");
    }
}
