package com.langchain4j.examples;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.Result;

/**
 * 工具获取上下文参数示例
 * <p>
 * 演示工具如何从用户消息中自动提取参数：
 * 1. 从用户消息中提取简单参数
 * 2. 从之前的工具执行结果中获取参数
 * 3. 使用上下文信息
 */
public class _07_ToolParameterContext {

    // =====================================================
    // 订单服务工具
    // =====================================================

    static class OrderTools {

        // 1. 直接从用户消息提取参数
        // 用户说"取消订单123"，LLM会自动提取订单ID
        @Tool("取消订单")
        String cancelOrder(InvocationParameters invocationParameters) {
            String orderId = invocationParameters.getOrDefault("orderId", "");
            if (orderId.isBlank()) {
                throw new RuntimeException("订单ID不能为空");
            }
            System.out.println("  [cancelOrder] 收到订单ID: " + orderId);
            return "订单 " + orderId + " 已成功取消";
        }

        // 2. 多个参数 - LLM会尝试从消息中提取所有参数
        @Tool("修改订单地址")
        String updateAddress(InvocationParameters invocationParameters, @V("newAddress") String newAddress) {
            String orderId = invocationParameters.getOrDefault("orderId", "");
            if (orderId.isBlank()) {
                throw new RuntimeException("订单ID不能为空");
            }
            if (newAddress.isBlank()) {
                throw new RuntimeException("新地址不能为空");
            }
            System.out.println("  [updateAddress] 订单ID: " + orderId + ", 新地址: " + newAddress);
            return "订单 " + orderId + " 的地址已更新为: " + newAddress;
        }

        // 3. 数字参数
        @Tool("查询订单价格")
        double getOrderPrice(InvocationParameters invocationParameters) {
            String orderId = invocationParameters.getOrDefault("orderId", "");
            if (orderId.isBlank()) {
                throw new RuntimeException("订单ID不能为空");
            }
            // 模拟返回价格
            if (orderId.contains("123")) {
                return 99.99;
            }
            return 199.99;
        }

        // 4. 布尔参数
        @Tool("检查订单是否VIP")
        boolean isVipOrder(InvocationParameters invocationParameters) {
            String orderId = invocationParameters.getOrDefault("orderId", "");
            if (orderId.isBlank()) {
                throw new RuntimeException("订单ID不能为空");
            }
            return orderId.contains("VIP");
        }
    }

    // =====================================================
    // 助手接口
    // =====================================================

    interface Assistant {
        @UserMessage("回答用户问题：{{message}}")
        Result<String> chat(@V("orderId") String orderId, @V("message") String message, InvocationParameters ctx);
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

        System.out.println("=====================工具获取上下文参数示例=====================");
        System.out.println();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(new OrderTools())
                .build();

        // 测试场景
        String[] queries = {
                // 场景1：直接提供订单ID
                "帮我取消订单 123-456",

                // 场景2：多个参数
                "把订单 999-888 的收货地址改成上海市浦东新区",

                // 场景3：查询操作
                "订单 ABC-123 的价格是多少？",

                // 场景4：布尔判断
                "订单 VIP-001 是不是VIP订单？",

                // 场景5：复杂查询
                "查询订单 123-456 的价格，判断是否VIP，然后取消它"
        };

        for (int i = 0; i < queries.length; i++) {
            System.out.println("──────────────────────────────────────");
            System.out.println("【测试 " + (i + 1) + "】");
            System.out.println("用户: " + queries[i]);
            System.out.println();

            Result<String> result = assistant.chat(String.valueOf(i), queries[i], InvocationParameters.from(
                    "orderId", String.valueOf(i)));

            System.out.println();
            System.out.println("AI 回答: " + result.content());

            System.out.println();
            System.out.println("工具执行记录:");
            for (ToolExecution te : result.toolExecutions()) {
                System.out.println("  工具: " + te.request().name());
                System.out.println("  参数: " + te.request().arguments());
                System.out.println("  结果: " + te.result());
            }
            System.out.println();
        }

        System.out.println("=====================执行完成=====================");
        System.out.println();
        System.out.println("提示：工具参数提取机制：");
        System.out.println("1. LLM会自动分析用户消息，提取相关参数");
        System.out.println("2. 参数名应与工具方法参数名匹配");
        System.out.println("3. 可以支持多个参数");
        System.out.println("4. 复杂场景可以先查询再操作");
    }
}
