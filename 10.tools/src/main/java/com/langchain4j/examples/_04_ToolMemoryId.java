package com.langchain4j.examples;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ToolMemoryId 示例
 * 
 * 演示 @Tool 与 @MemoryId 结合使用，实现多用户/多会话记忆。
 * 
 * @MemoryId 用于标识不同的用户会话
 * @Tool 方法可以接收 @ToolMemoryId 参数，自动获取当前会话ID
 */
public class _04_ToolMemoryId {

    // =====================================================
    // 用户数据存储（模拟数据库）
    // =====================================================
    
    static class UserDatabase {
        // 存储用户余额：userId -> balance
        static Map<String, Double> balances = new ConcurrentHashMap<>();
        
        // 存储对话历史：userId -> conversation history
        static Map<String, String> histories = new ConcurrentHashMap<>();
        
        static {
            // 初始化用户余额
            balances.put("user-alice", 1000.0);
            balances.put("user-bob", 500.0);
            balances.put("user-carol", 2000.0);
        }
    }

    // =====================================================
    // 定义工具类（使用 @Tool 和 @ToolMemoryId）
    // =====================================================
    
    static class BankingTools {
        
        // 查询余额 - 接收 @ToolMemoryId 来区分用户
        @Tool("查询用户余额")
        String getBalance(String memoryId) {
            Double balance = UserDatabase.balances.getOrDefault(memoryId, 0.0);
            return String.format("用户 %s 的余额为: ¥%.2f", memoryId, balance);
        }
        
        // 存款 - 接收 @ToolMemoryId 来区分用户
        @Tool("用户存款")
        String deposit(String memoryId, double amount) {
            double current = UserDatabase.balances.getOrDefault(memoryId, 0.0);
            double newBalance = current + amount;
            UserDatabase.balances.put(memoryId, newBalance);
            return String.format("用户 %s 存款 ¥%.2f 成功，当前余额: ¥%.2f", memoryId, amount, newBalance);
        }
        
        // 记录对话历史
        @Tool("记录对话历史")
        String saveHistory(String memoryId, String message) {
            String history = UserDatabase.histories.getOrDefault(memoryId, "");
            history += message + "\n";
            UserDatabase.histories.put(memoryId, history);
            return "已保存对话历史";
        }
    }

    // =====================================================
    // 助手接口（使用 @MemoryId）
    // =====================================================
    
    interface Assistant {
        @UserMessage("回答用户 {{it}} 的问题")
        String chat(@MemoryId String memoryId, @V("it") String message);
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

        System.out.println("=====================@ToolMemoryId 示例=====================");
        System.out.println();

        // 创建支持多会话的助手
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .tools(new BankingTools())
                .build();

        // 模拟多用户场景
        System.out.println("【场景】多用户银行服务");
        System.out.println("──────────────────────────────────────");
        
        // 用户 Alice 查询余额
        System.out.println("用户 alice 查询余额:");
        String response1 = assistant.chat("user-alice", "我的余额是多少？");
        System.out.println("回答: " + response1);
        System.out.println();
        
        // 用户 Bob 查询余额
        System.out.println("用户 bob 查询余额:");
        String response2 = assistant.chat("user-bob", "我的余额是多少？");
        System.out.println("回答: " + response2);
        System.out.println();
        
        // 用户 Alice 存款
        System.out.println("用户 alice 存款 500:");
        String response3 = assistant.chat("user-alice", "我想存500元");
        System.out.println("回答: " + response3);
        System.out.println();
        
        // 再次查询 Alice 的余额（验证存款）
        System.out.println("用户 alice 再次查询余额:");
        String response4 = assistant.chat("user-alice", "现在余额是多少？");
        System.out.println("回答: " + response4);
        System.out.println();

        System.out.println("=====================执行完成=====================");
        System.out.println();
        System.out.println("提示：@ToolMemoryId 的作用：");
        System.out.println("1. 自动传递当前 @MemoryId 给工具方法");
        System.out.println("2. 实现多用户/多会话隔离");
        System.out.println("3. 每个用户有独立的内存和数据");
    }
}
