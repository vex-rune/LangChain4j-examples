package com.langchain4j.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * LangChain4j AI 服务高级示例
 * 
 * 本示例演示：
 * 1. boolean 返回类型（情感分析）
 * 2. Enum 返回类型（优先级分析）
 * 3. POJO 返回类型（带 @Description）
 * 4. Result<T> 返回类型（元数据）
 * 
 * @see <a href="https://langchain4j.cn/tutorials/ai-services.html">AI 服务</a>
 */
public class AiServicesAdvancedExample {

    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("请设置环境变量 DEEPSEEK_API_KEY");
            return;
        }

        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .build();

        // 示例1：boolean 返回类型
        example1_booleanReturn(model);

        // 示例2：Enum 返回类型
        example2_enumReturn(model);

        // 示例3：POJO + @Description
        example3_pojoWithDescription(model);

        // 示例4：Result<T> 返回元数据
        example4_resultReturn(model);

        // 示例5：List<String> 返回类型
        example5_listReturn(model);
    }

    /**
     * 示例1：boolean 返回类型
     * 用于情感分析等二分类任务
     */
    static void example1_booleanReturn(ChatModel model) {
        System.out.println("\n=== 示例1：boolean 返回类型（情感分析） ===");

        interface SentimentAnalyzer {
            @dev.langchain4j.service.UserMessage("判断以下文本情感是否积极，回答 true 或 false：{{it}}")
            boolean isPositive(String text);
        }

        SentimentAnalyzer analyzer = AiServices.create(SentimentAnalyzer.class, model);

        System.out.println("文本: 今天天气真好！");
        System.out.println("结果: " + analyzer.isPositive("今天天气真好！"));

        System.out.println("文本: 糟糕，忘记带钥匙了");
        System.out.println("结果: " + analyzer.isPositive("糟糕，忘记带钥匙了"));
    }

    /**
     * 示例2：Enum 返回类型
     * 用于多分类任务
     */
    static void example2_enumReturn(ChatModel model) {
        System.out.println("\n=== 示例2：Enum 返回类型（优先级分析） ===");

        enum Priority {
            CRITICAL,  // 紧急
            HIGH,      // 高
            LOW        // 低
        }

        interface PriorityAnalyzer {
            @dev.langchain4j.service.UserMessage("分析以下问题的优先级，回答 CRITICAL/HIGH/LOW：\n{{it}}")
            Priority analyzePriority(String issueDescription);
        }

        PriorityAnalyzer analyzer = AiServices.create(PriorityAnalyzer.class, model);

        String issue1 = "主要支付网关宕机，客户无法处理交易";
        System.out.println("问题: " + issue1);
        System.out.println("优先级: " + analyzer.analyzePriority(issue1));

        String issue2 = "文档中的拼写错误";
        System.out.println("问题: " + issue2);
        System.out.println("优先级: " + analyzer.analyzePriority(issue2));
    }

    /**
     * 示例3：POJO + @Description
     * @Description 注解帮助 LLM 更好地理解字段含义
     */
    static void example3_pojoWithDescription(ChatModel model) {
        System.out.println("\n=== 示例3：POJO + @Description ===");

        // 定义地址类
        class Address {
            @Description("街道名称")
            String street;
            
            @Description("门牌号")
            Integer streetNumber;
            
            @Description("城市名称")
            String city;

            @Override
            public String toString() {
                return "Address{street='" + street + "', number=" + streetNumber + ", city='" + city + "'}";
            }
        }

        // 定义人员类
        class Person {
            @Description("人的姓氏")
            String firstName;
            
            @Description("人的名字")
            String lastName;
            
            @Description("出生年份")
            Integer birthYear;
            
            Address address;

            @Override
            public String toString() {
                return "Person{firstName='" + firstName + "', lastName='" + lastName + 
                       "', birthYear=" + birthYear + ", address=" + address + "}";
            }
        }

        interface PersonExtractor {
            @dev.langchain4j.service.UserMessage(
                "从以下文本中提取人物信息，返回JSON格式：\n" +
                "{\"firstName\":\"姓\",\"lastName\":\"名\",\"birthYear\":年份,\"address\":{\"street\":\"街道\",\"number\":门牌,\"city\":\"城市\"}}\n" +
                "文本：\n{{it}}"
            )
            String extractPerson(@dev.langchain4j.service.V("text") String text);
        }

        PersonExtractor extractor = AiServices.create(PersonExtractor.class, model);

        String text = "1968年，一个名叫John Doe的孩子出生了。" +
                     "他住在345 Whispering Pines Avenue，Springfield。";

        try {
            String json = extractor.extractPerson(text);
            System.out.println("模型输出: " + json);
            
            // 使用 Jackson 解析
            ObjectMapper mapper = new ObjectMapper();
            String cleanJson = json.replaceAll("```json", "").replaceAll("```", "").trim();
            int start = cleanJson.indexOf('{');
            int end = cleanJson.lastIndexOf('}');
            if (start >= 0 && end >= 0) {
                cleanJson = cleanJson.substring(start, end + 1);
            }
            Person person = mapper.readValue(cleanJson, Person.class);
            System.out.println("解析结果: " + person);
        } catch (Exception e) {
            System.out.println("解析失败: " + e.getMessage());
        }
    }

    /**
     * 示例4：Result<T> 返回元数据
     * 获取 Token 使用量、FinishReason 等信息
     */
    static void example4_resultReturn(ChatModel model) {
        System.out.println("\n=== 示例4：Result<T> 返回元数据 ===");

        interface OutlineGenerator {
            @dev.langchain4j.service.UserMessage("为以下主题生成文章大纲，列出3个要点：\n主题：{{it}}")
            Result<List<String>> generateOutline(String topic);
        }

        OutlineGenerator generator = AiServices.create(OutlineGenerator.class, model);

        Result<List<String>> result = generator.generateOutline("Java 编程");

        System.out.println("大纲内容:");
        for (int i = 0; i < result.content().size(); i++) {
            System.out.println((i + 1) + ". " + result.content().get(i));
        }

        if (result.tokenUsage() != null) {
            System.out.println("\nToken 使用量: " + result.tokenUsage());
        }

        if (result.finishReason() != null) {
            System.out.println("结束原因: " + result.finishReason());
        }
    }

    /**
     * 示例5：List<String> 返回类型
     * 提取多个项目
     */
    static void example5_listReturn(ChatModel model) {
        System.out.println("\n=== 示例5：List<String> 返回类型 ===");

        interface ListExtractor {
            @dev.langchain4j.service.UserMessage(
                "从以下文本中提取所有提到的编程语言名称，返回JSON数组格式：\n{{it}}"
            )
            List<String> extractLanguages(String text);
        }

        ListExtractor extractor = AiServices.create(ListExtractor.class, model);

        String text = "我使用 Java、Python 和 JavaScript 进行开发，也了解 Go 语言。";
        try {
            List<String> languages = extractor.extractLanguages(text);
            System.out.println("提取的编程语言: " + languages);
        } catch (Exception e) {
            System.out.println("提取失败: " + e.getMessage());
        }
    }
}