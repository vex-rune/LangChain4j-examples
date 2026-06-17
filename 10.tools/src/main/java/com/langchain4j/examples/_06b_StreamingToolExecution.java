package com.langchain4j.examples;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.tool.BeforeToolExecution;

/**
 * 流式模式访问工具执行示例
 * 
 * 演示如何使用 StreamingChatModel 和 TokenStream 实时获取工具执行信息。
 *
 * DEEPSEEK 不支持流式工具调用，请使用非流式模式。请看 {@link  MiniMaxExample}
 */
public class _06b_StreamingToolExecution {

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
    }

    // =====================================================
    // 助手接口 - 流式版本
    // =====================================================
    
    interface StreamingAssistant {
        @UserMessage("回答用户问题：{{it}}")
        TokenStream chat(String message);
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

        System.out.println("=====================流式模式访问工具执行=====================");
        System.out.println();

        // 创建流式 ChatModel
        StreamingChatModel streamingChatModel = OpenAiStreamingChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .logRequests(true)
                .build();

        // 创建流式助手 - 使用 streamingChatModel()
        StreamingAssistant assistant = AiServices.builder(StreamingAssistant.class)
                .streamingChatModel(streamingChatModel)
                .tools(new BookingTools())
                .build();

        System.out.println("【示例】使用 TokenStream 和回调监控执行过程");
        System.out.println("──────────────────────────────────────");
        System.out.println();
        
        String query = "帮我取消订单 123-456";
        System.out.println("用户: " + query);
        System.out.println();
        System.out.println("开始执行...");
        System.out.println();
        
        // 创建 TokenStream
        TokenStream tokenStream = assistant.chat(query);
        
        // 设置回调 - 添加更多回调来跟踪执行
        tokenStream
            // 工具执行前回调
            .beforeToolExecution((BeforeToolExecution be) -> {
                System.out.println("【工具执行前】");
                System.out.println("  工具名: " + be.request().name());
                System.out.println("  参数: " + be.request().arguments());
            })
            // 工具执行后回调 - 实时获取工具执行信息
            .onToolExecuted(toolExecution -> {
                System.out.println("【工具执行后】");
                System.out.println("  工具名: " + toolExecution.request().name());
                System.out.println("  参数: " + toolExecution.request().arguments());
                System.out.println("  结果: " + toolExecution.result());
                if (toolExecution.duration() != null) {
                    System.out.println("  耗时: " + toolExecution.duration().toMillis() + "ms");
                }
            })
            // 部分响应回调 - 实时显示 AI 输出
            .onPartialResponse(partial -> {
                if (partial != null && !partial.isEmpty()) {
                    System.out.print(partial);
                }
            })
            // 中间响应回调（工具调用后的 AI 响应）
            .onIntermediateResponse(response -> {
                System.out.println();
                System.out.println("【中间响应】" + response.aiMessage().text());
            })
            // 完成回调
            .onCompleteResponse(complete -> {
                System.out.println();
                System.out.println();
                System.out.println("【执行完成】");
                if (complete.tokenUsage() != null) {
                    System.out.println("  输入 Token: " + complete.tokenUsage().inputTokenCount());
                    System.out.println("  输出 Token: " + complete.tokenUsage().outputTokenCount());
                }
            })
            // 错误回调
            .onError(error -> {
                System.out.println();
                System.out.println("【错误】" + error);
            })
            // 开始执行
            .start();

        System.out.println();
        System.out.println("=====================执行完成=====================");
        System.out.println();
        System.out.println("提示：流式模式回调执行顺序：");
        System.out.println("1. beforeToolExecution - 工具执行前");
        System.out.println("2. 工具实际执行");
        System.out.println("3. onToolExecuted - 工具执行后");
        System.out.println("4. onPartialResponse - AI 部分响应");
        System.out.println("5. onCompleteResponse - 执行完成");
    }
}
