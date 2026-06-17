package com.langchain4j.examples;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.tool.ToolExecution;

/**
 * 访问已执行工具示例
 * 
 * 演示如何使用 Result<T> 获取工具执行记录。
 * 
 * 注意：TokenStream 方式需要单独的方法签名，当前版本暂不演示。
 */
public class _06_AccessToolExecutions {

    // =====================================================
    // 定义工具
    // =====================================================
    
    static class BookingTools {
        
        @Tool("取消订单")
        String cancelBooking(String bookingId) {
            System.out.println("  [工具执行] cancelBooking(bookingId=\"" + bookingId + "\")");
            if (bookingId.equals("123-456")) {
                return "订单 123-456 已成功取消";
            }
            return "无法找到订单 " + bookingId;
        }
        
        @Tool("查询订单状态")
        String getOrderStatus(String orderId) {
            System.out.println("  [工具执行] getOrderStatus(orderId=\"" + orderId + "\")");
            return "订单 " + orderId + " 状态: 已发货";
        }
        
        @Tool("获取用户信息")
        String getUserInfo(String userId) {
            System.out.println("  [工具执行] getUserInfo(userId=\"" + userId + "\")");
            return "用户 " + userId + " 信息: VIP会员, 积分 5000";
        }
    }

    // =====================================================
    // 助手接口 - 使用 Result<T> 包装返回类型
    // =====================================================
    
    interface Assistant {
        @UserMessage("回答用户问题：{{it}}")
        Result<String> chat(String message);
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

        System.out.println("=====================访问已执行工具示例=====================");
        System.out.println();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(new BookingTools())
                .build();

        // =====================================================
        // 示例：使用 Result<T> 获取工具执行记录
        // =====================================================
        System.out.println("【示例】使用 Result<T> 获取工具执行记录");
        System.out.println("──────────────────────────────────────");
        
        String query = "帮我取消订单 123-456，然后查询用户 alice 的信息";
        System.out.println("用户: " + query);
        System.out.println();
        
        // 调用并获取 Result
        Result<String> result = assistant.chat(query);
        
        // 获取 AI 回答
        String answer = result.content();
        System.out.println();
        System.out.println("AI 回答: " + answer);
        
        // 获取工具执行记录
        System.out.println();
        System.out.println("【工具执行记录】");
        System.out.println("共执行了 " + result.toolExecutions().size() + " 个工具:");
        System.out.println();
        
        for (int i = 0; i < result.toolExecutions().size(); i++) {
            ToolExecution te = result.toolExecutions().get(i);
            System.out.println("  工具 #" + (i + 1));
            System.out.println("    名称: " + te.request().name());
            System.out.println("    参数: " + te.request().arguments());
            System.out.println("    结果: " + te.result());
            System.out.println("    耗时: " + te.duration());
            System.out.println();
        }
        
        // 获取 Token 使用统计
        System.out.println("【Token 使用统计】");
        System.out.println("  输入 Token: " + result.tokenUsage().inputTokenCount());
        System.out.println("  输出 Token: " + result.tokenUsage().outputTokenCount());
        System.out.println("  总计: " + result.tokenUsage().totalTokenCount());

        System.out.println();
        System.out.println("=====================执行完成=====================");
        System.out.println();
        System.out.println("提示：访问工具执行信息的用途：");
        System.out.println("1. 审计日志 - 记录所有 AI 调用的工具");
        System.out.println("2. 调试 - 查看 AI 是否正确调用工具");
        System.out.println("3. 监控 - 统计工具使用频率");
        System.out.println("4. 费用计算 - 根据 Token 使用量计费");
    }
}
