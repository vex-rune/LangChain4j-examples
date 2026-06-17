package com.langchain4j.examples;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.Scanner;

/**
 * 纯代理式 AI 示例
 * 
 * 与工作流模式不同，纯代理式 AI 让 LLM 自主决定调用哪些工具和处理流程。
 * 
 * 核心组件：
 * - SupervisorAgent: 监督代理，根据用户请求决定调用哪个子Agent
 * - 子Agent: 执行具体任务的工具
 * - HumanInTheLoop: 人类参与环节
 */
public class PureAgenticExample {

    // =====================================================
    // 定义子Agent（工具）
    // =====================================================
    
    // 1. 取款代理
    public interface WithdrawAgent {
        @SystemMessage("你是一个银行柜员，只能从用户账户取款美元")
        @UserMessage("从{{user}}的账户取款{{amount}}美元，返回余额")
        @Agent("银行取款柜员")
        String withdraw(@V("user") String user, @V("amount") Double amount);
    }

    // 2. 存款代理
    public interface CreditAgent {
        @SystemMessage("你是一个银行柜员，只能向用户账户存款美元")
        @UserMessage("向{{user}}的账户存款{{amount}}美元，返回余额")
        @Agent("银行存款柜员")
        String credit(@V("user") String user, @V("amount") Double amount);
    }

    // 3. 汇率代理
    public interface ExchangeAgent {
        @SystemMessage("你是一个外汇兑换员，可以进行货币兑换")
        @UserMessage("将{{amount}}{{originalCurrency}}兑换成{{targetCurrency}}，返回兑换后的金额")
        @Agent("外汇兑换员")
        Double exchange(@V("originalCurrency") String originalCurrency, @V("amount") Double amount, @V("targetCurrency") String targetCurrency);
    }

    // 4. 星座运势代理
    public interface AstrologyAgent {
        @SystemMessage("你是一个占星师，根据用户的姓名和星座生成运势")
        @UserMessage("为{{name}}（星座：{{sign}}）生成今日运势")
        @Agent("占星师，根据姓名和星座生成运势")
        String horoscope(@V("name") String name, @V("sign") String sign);
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

        ChatModel baseModel = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .logRequests(true)
                .build();

        System.out.println("=====================纯代理式 AI 示例=====================");
        System.out.println("Supervisor Agent 会自主决定调用哪个子Agent");
        System.out.println();

        // 创建子Agent
        WithdrawAgent withdrawAgent = AgenticServices.agentBuilder(WithdrawAgent.class)
                .chatModel(baseModel)
                .build();
        
        CreditAgent creditAgent = AgenticServices.agentBuilder(CreditAgent.class)
                .chatModel(baseModel)
                .build();
        
        ExchangeAgent exchangeAgent = AgenticServices.agentBuilder(ExchangeAgent.class)
                .chatModel(baseModel)
                .build();
        
        AstrologyAgent astrologyAgent = AgenticServices.agentBuilder(AstrologyAgent.class)
                .chatModel(baseModel)
                .build();

        // 创建监督代理
        SupervisorAgent bankSupervisor = AgenticServices.supervisorBuilder()
                .chatModel(baseModel)
                .subAgents(withdrawAgent, creditAgent, exchangeAgent)
                .maxAgentsInvocations(5)
                .build();

        // =====================================================
        // 示例1：银行服务
        // =====================================================
        System.out.println("【示例1】银行服务 - Supervisor 自动选择合适的柜员");
        System.out.println();

        String result = bankSupervisor.invoke("执行转账操作：从 Mario 账户转账 100 欧元到 Georgios 账户, 最后告诉我他们的余额");
        System.out.println("最终结果: " + result);
        System.out.println();

        // =====================================================
        // 示例2：人类参与环节（Human-in-the-loop）
        // =====================================================
        System.out.println("【示例2】人类参与环节 - AI 会询问缺失信息");
        System.out.println();

        // 创建人类参与环节
        // humanInTheLoopBuilder
        HumanInTheLoop askSign = AgenticServices.humanInTheLoopBuilder()
                .description("向用户询问星座信息")
                .outputKey("sign")
                .responseProvider(scope -> {
                    // 从 scope 中读取要询问的问题
                    String request = (String) scope.readState("sign", null);
                    System.out.println();
                    System.out.println("【AI 提问】" + request);
                    System.out.print("请输入你的星座（如：双子座）> ");
                    return new Scanner(System.in).nextLine();
                })
                .build();

        // 创建带人类参与的监督代理
        SupervisorAgent horoscopeAgent = AgenticServices.supervisorBuilder()
                .chatModel(baseModel)
                .subAgents(astrologyAgent, askSign)
                .maxAgentsInvocations(5)
                .build();

        System.out.println("用户输入：我叫Mario，请告诉我今日运势");
        System.out.println();
        
        String horoscope = horoscopeAgent.invoke("我叫Mario，请告诉我今日运势");
        System.out.println();
        System.out.println("【最终结果】" + horoscope);

        System.out.println("\n=====================执行完成=====================");
    }
}
