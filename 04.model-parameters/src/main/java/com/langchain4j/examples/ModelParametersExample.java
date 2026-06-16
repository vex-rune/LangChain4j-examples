package com.langchain4j.examples;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * LangChain4j 模型参数示例
 * 
 * 本示例演示：
 * 1. 使用 Builder 模式配置模型参数
 * 2. temperature - 控制输出随机性
 * 3. maxTokens - 控制输出长度
 * 4. timeout - 设置请求超时
 * 5. logRequests/logResponses - 调试日志
 * 
 * @see <a href="https://langchain4j.cn/tutorials/model-parameters.html">模型参数</a>
 */
public class ModelParametersExample {

    public static void main(String[] args) {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("请设置环境变量 DEEPSEEK_API_KEY");
            return;
        }

        // 示例1：基本配置（使用默认值）
        example1_defaultConfig(apiKey);

        // 示例2：temperature 控制随机性
        example2_temperature(apiKey);

        // 示例3：maxTokens 限制输出长度
        example3_maxTokens(apiKey);

        // 示例4：完整配置
        example4_fullConfig(apiKey);
    }

    /**
     * 示例1：基本配置
     * 使用默认参数
     */
    static void example1_defaultConfig(String apiKey) {
        System.out.println("\n=== 示例1：基本配置 ===");

        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .build();

        String answer = model.chat("给我讲一个笑话");
        System.out.println("AI: " + answer);
    }

    /**
     * 示例2：temperature 控制随机性
     * 0.0 = 确定性输出，2.0 = 高度随机
     */
    static void example2_temperature(String apiKey) {
        System.out.println("\n=== 示例2：temperature ===");

        // 低温度 - 更确定性、专注
        ChatModel lowTempModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .temperature(0.0)
                .build();

        System.out.println("temperature=0.0 (低):");
        System.out.println("AI: " + lowTempModel.chat("1+1等于几"));

        // 高温度 - 更随机、有创意
        ChatModel highTempModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .temperature(1.5)
                .build();

        System.out.println("\ntemperature=1.5 (高):");
        System.out.println("AI: " + highTempModel.chat("1+1等于几"));
    }

    /**
     * 示例3：maxTokens 限制输出长度
     */
    static void example3_maxTokens(String apiKey) {
        System.out.println("\n=== 示例3：maxTokens ===");

        // 限制输出为短文本
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .maxTokens(20) // 最多 20 tokens
                .build();

        String answer = model.chat("介绍一下中国");
        System.out.println("AI (限制20 tokens): " + answer);
    }

    /**
     * 示例4：完整配置
     * 展示所有常用参数
     */
    static void example4_fullConfig(String apiKey) {
        System.out.println("\n=== 示例4：完整配置 ===");

        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .temperature(0.7)           // 控制随机性
                .maxTokens(200)             // 最大输出 tokens
                .timeout(java.time.Duration.ofSeconds(60)) // 超时设置
                .logRequests(true)          // 记录请求日志
                .logResponses(true)        // 记录响应日志
                .build();

        String answer = model.chat("请用一句话介绍 Java 编程语言");
        System.out.println("AI: " + answer);
    }
}