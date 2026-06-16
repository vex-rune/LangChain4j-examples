package com.langchain4j.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

/**
 * LangChain4j 结构化输出 - 使用 Jackson 解析
 * 
 * 本示例演示：
 * 1. 使用 AI 服务返回 JSON 字符串
 * 2. 使用 Jackson 解析 JSON 为 POJO
 * 
 * @see <a href="https://langchain4j.cn/tutorials/ai-services.html">AI 服务</a>
 */
public class StructuredOutputWithJacksonExample {

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

        // 示例：使用 Jackson 解析 JSON
        example1_jacksonParse(model);
    }

    /**
     * 示例：提示模型返回 JSON，然后用 Jackson 解析
     */
    static void example1_jacksonParse(ChatModel model) throws Exception {
        System.out.println("\n=== Jackson 解析 JSON 示例 ===");

        // 定义数据类
        record Person(String name, int age, String city) {}

        // 定义接口，返回 String（JSON 格式）
        interface PersonExtractor {
            @dev.langchain4j.service.UserMessage(
                "从以下文本中提取人物信息，直接返回JSON，不要有其他内容：\n" +
                "格式：{\"name\":\"姓名\",\"age\":年龄,\"city\":\"城市\"}\n" +
                "文本：{{text}}"
            )
            String extract(@dev.langchain4j.service.V("text") String text);
        }

        PersonExtractor extractor = AiServices.create(PersonExtractor.class, model);

        String text = "张三是一个25岁的程序员，他住在上海工作";
        String json = extractor.extract(text);
        System.out.println("模型输出: " + json);

        // 使用 Jackson 解析
        ObjectMapper mapper = new ObjectMapper();
        try {
            // 清理可能的 markdown 代码块
            String cleanJson = cleanJsonResponse(json);
            Person person = mapper.readValue(cleanJson, Person.class);
            
            System.out.println("\n解析成功！");
            System.out.println("姓名: " + person.name());
            System.out.println("年龄: " + person.age());
            System.out.println("城市: " + person.city());
            System.out.println("完整对象: " + person);
        } catch (Exception e) {
            System.out.println("解析失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 清理 JSON 响应中的多余内容（如 markdown 代码块）
     */
    static String cleanJsonResponse(String response) {
        if (response == null) return "";
        
        String cleaned = response.trim();
        
        // 移除 markdown 代码块标记
        if (cleaned.startsWith("```")) {
            int firstBrace = cleaned.indexOf('{');
            int lastBrace = cleaned.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace >= 0) {
                cleaned = cleaned.substring(firstBrace, lastBrace + 1);
            } else {
                cleaned = cleaned.replaceAll("```json?", "").replaceAll("```", "").trim();
            }
        }
        
        return cleaned.trim();
    }
}