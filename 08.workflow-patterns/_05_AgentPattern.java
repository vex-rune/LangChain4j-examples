package com.langchain4j.examples;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

/**
 * Agent 模式核心概念
 * 
 * =====================================================
 * 核心概念
 * =====================================================
 * 
 * 1. @Agent 注解
 *    - 标记接口方法为一个 Agent
 *    - 定义输入输出
 *    
 * 2. outputName（输出变量名）
 *    - Agent 的输出存储到共享变量
 *    - 其他 Agent 可以通过 @V 读取
 *    
 * 3. @V 注解
 *    - 从 AgenticScope 读取变量
 *    - 也可以用参数名（如果编译用了 -parameters）
 *    
 * =====================================================
 * 官方 API（需要 langchain4j-agentic 模块）
 * =====================================================
 * 
 * 定义 Agent：
 * ```java
 * public interface Writer {
 *     @UserMessage("根据主题创作故事：{{topic}}")
 *     @Agent(outputName = "story")
 *     String generateStory(@V("topic") String topic);
 * }
 * ```
 * 
 * 创建 Agent：
 * ```java
 * Writer writer = AgenticServices.agentBuilder(Writer.class)
 *     .chatModel(model)
 *     .outputName("story")
 *     .build();
 * ```
 * 
 * 使用：
 * ```java
 * String story = writer.generateStory(" dragons");
 * ```
 * 
 * =====================================================
 * 当前实现（手动方式）
 * =====================================================
 */
public class _05_AgentPattern {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    Agent 模式核心示例                         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.println();

        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("【错误】请设置环境变量 DEEPSEEK_API_KEY");
            return;
        }

        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .build();

        // =====================================================
        // 手动实现：创建 Agent（模拟 @Agent + outputName）
        // =====================================================
        
        // Agent 1: Writer（outputName = "story"）
        WriterAgent writer = AiServices.create(WriterAgent.class, model);
        
        // Agent 2: Editor（从 @V("story") 读取输出）
        EditorAgent editor = AiServices.create(EditorAgent.class, model);

        String topic = "龙与法师";
        String audience = "青少年";
        String style = "奇幻冒险";

        System.out.println("【输入】");
        System.out.println("  主题: " + topic);
        System.out.println("  受众: " + audience);
        System.out.println("  风格: " + style);
        System.out.println();

        // =====================================================
        // 执行流程（模拟 AgenticScope）
        // =====================================================
        
        // Step 1: Writer Agent 生成故事（outputName = "story"）
        System.out.println("────────────────────────────────────────");
        System.out.println("【Agent 1: Writer】");
        System.out.println("  任务: 根据主题创作故事");
        System.out.println("  input: topic = \"" + topic + "\"");
        
        String story = writer.generateStory(topic);
        
        System.out.println("  output: story = \"" + story + "\"");
        System.out.println();

        // Step 2: Editor Agent 调整受众（input: story, audience）
        System.out.println("────────────────────────────────────────");
        System.out.println("【Agent 2: Editor - 调整受众】");
        System.out.println("  任务: 根据受众修改故事");
        System.out.println("  input: story = \"" + story + "\"");
        System.out.println("         audience = \"" + audience + "\"");
        
        story = editor.editForAudience(story, audience);
        
        System.out.println("  output: story = \"" + story + "\"");
        System.out.println();

        // Step 3: Editor Agent 调整风格
        System.out.println("────────────────────────────────────────");
        System.out.println("【Agent 2: Editor - 调整风格】");
        System.out.println("  任务: 根据风格修改故事");
        System.out.println("  input: story = \"" + story + "\"");
        System.out.println("         style = \"" + style + "\"");
        
        story = editor.adjustStyle(story, style);
        
        System.out.println("  output: story = \"" + story + "\"");
        System.out.println();

        System.out.println("══════════════════════════════════════════════════════════════════");
        System.out.println("【完成】最终故事:");
        System.out.println(story);
    }

    // =====================================================
    // Agent 接口定义（模拟 @Agent 注解）
    // =====================================================
    
    /**
     * Writer Agent
     * - 功能：根据主题创作故事
     * - input: topic
     * - outputName: "story"
     */
    interface WriterAgent {
        @dev.langchain4j.service.UserMessage(
            "你是一个创意作家。\n" +
            "根据以下主题创作一个简短的故事（3句话以内）。\n" +
            "只返回故事内容，不要有其他说明。\n" +
            "主题：{{topic}}"
        )
        String generateStory(@dev.langchain4j.service.V("topic") String topic);
    }

    /**
     * Editor Agent
     * - 功能：编辑故事
     * - input: story, audience/style
     * - outputName: "story"
     */
    interface EditorAgent {
        @dev.langchain4j.service.UserMessage(
            "你是一个专业编辑。\n" +
            "根据以下要求修改故事，只返回修改后的故事。\n" +
            "受众：{{audience}}\n" +
            "故事：\n{{story}}"
        )
        String editForAudience(
            @dev.langchain4j.service.V("story") String story,
            @dev.langchain4j.service.V("audience") String audience
        );

        @dev.langchain4j.service.UserMessage(
            "你是一个风格编辑。\n" +
            "根据以下风格调整故事，只返回调整后的故事。\n" +
            "风格：{{style}}\n" +
            "故事：\n{{story}}"
        )
        String adjustStyle(
            @dev.langchain4j.service.V("story") String story,
            @dev.langchain4j.service.V("style") String style
        );
    }
}
