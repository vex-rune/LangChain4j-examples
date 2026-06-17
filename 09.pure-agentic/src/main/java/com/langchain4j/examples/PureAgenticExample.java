package com.langchain4j.examples;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * 纯代理式 AI 示例
 * 
 * 与工作流模式不同，纯代理式 AI 让 LLM 自主决定调用哪些工具和处理流程。
 * 
 * 核心组件：
 * - SupervisorAgent: 监督代理，根据用户请求决定调用哪个子Agent
 * - 子Agent: 执行具体任务的工具
 */
public class PureAgenticExample {

    // =====================================================
    // 定义子Agent（工具）
    // =====================================================

    // =====================================================
    // 主方法
    // =====================================================
    
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

        System.out.println("=====================纯代理式 AI 示例=====================");
        System.out.println("Supervisor Agent 会自主决定调用哪个子Agent");
        System.out.println();

        BankTool bankTool = new BankTool();
        System.out.println( "创建两个账户：Mario 和 Georgios");
        bankTool.createAccount("Mario", 1000.0);
        System.out.println("Mario 的余额：" + bankTool.getBalance("Mario"));
        bankTool.createAccount("Georgios", 1000.0);
        System.out.println("Georgios 的余额：" + bankTool.getBalance("Georgios"));

        WithdrawAgent withdrawAgent = AgenticServices
                .agentBuilder(WithdrawAgent.class)
                .chatModel(baseModel)
                .tools(bankTool)
                .build();
        CreditAgent creditAgent = AgenticServices
                .agentBuilder(CreditAgent.class)
                .chatModel(baseModel)
                .tools(bankTool)
                .build();

        ExchangeAgent exchange = AgenticServices
                .agentBuilder(ExchangeAgent.class)
                .chatModel(baseModel)
                .tools(new ExchangeTool())
                .build();

        SupervisorAgent bankSupervisor = AgenticServices
                .supervisorBuilder()
                .chatModel(baseModel)
                .subAgents(withdrawAgent, creditAgent, exchange)
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .build();

        // 执行转账操作：从 Mario 账户转账 100 欧元到 Georgios 账户
        String result = bankSupervisor.invoke("执行转账操作：从 Mario 账户转账 100 欧元到 Georgios 账户, 最后告诉我他们的余额");
        System.out.println("执行结果：" + result);

        System.out.println("=====================执行完成=====================");
    }
}
