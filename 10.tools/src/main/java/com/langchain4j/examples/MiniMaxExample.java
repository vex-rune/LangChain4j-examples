package com.langchain4j.examples;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.Result;

/**
 * MiniMax API 接入示例
 * 
 * MiniMax API 与 OpenAI API 兼容，可以使用 OpenAiChatModel 接入。
 * 
 * MiniMax API 地址: https://api.minimax.chat
 * 
 * 注意：流式工具调用支持取决于 MiniMax 是否支持 function calling + stream
 */
public class MiniMaxExample {

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
    }

    // =====================================================
    // 助手接口
    // =====================================================
    
    interface Assistant {
        @UserMessage("回答用户问题：{{it}}")
        Result<String> chat(String message);
    }

    // =====================================================
    // 主方法
    // =====================================================
    
    public static void main(String[] args) {
        // MiniMax API 配置
        String apiKey = System.getenv("MINIMAX_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("请配置环境变量 MINIMAX_API_KEY");
            System.err.println("或修改代码中的 apiKey 为你的 API Key");
            return;
        }

        System.out.println("=====================MiniMax API 接入示例=====================");
        System.out.println();

        // 创建 ChatModel - 使用 MiniMax API 地址
        ChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl("https://api.minimax.chat/v1")  // MiniMax API 地址
                .apiKey(apiKey)
                .modelName("MiniMax-Text-01")  // MiniMax 模型名
                .build();

        System.out.println("【示例】使用 MiniMax API 调用工具");
        System.out.println("──────────────────────────────────────");
        System.out.println();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .tools(new BookingTools())
                .build();

        String query = "帮我取消订单 123-456";
        System.out.println("用户: " + query);
        System.out.println();

        // 调用
        Result<String> result = assistant.chat(query);

        // 获取结果
        System.out.println();
        System.out.println("AI 回答: " + result.content());

        // 获取工具执行记录
        System.out.println();
        System.out.println("工具执行记录:");
        for (ToolExecution te : result.toolExecutions()) {
            System.out.println("  - " + te.request().name() + ": " + te.result());
        }

        System.out.println();
        System.out.println("=====================执行完成=====================");
        System.out.println();
        System.out.println("提示：");
        System.out.println("1. MiniMax API 与 OpenAI 兼容，使用 OpenAiChatModel 接入");
        System.out.println("2. 修改 baseUrl 为 MiniMax API 地址");
        System.out.println("3. 修改 modelName 为 MiniMax 支持的模型");
    }
}
