package com.langchain4j.examples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.agent.ErrorContext;
import dev.langchain4j.agentic.agent.ErrorRecoveryResult;
import dev.langchain4j.agentic.agent.MissingArgumentException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class _05_ErrorHandling {

    // 1. 生成初稿Agent
    public interface CreativeWriter {
        @Agent("根据主题生成故事初稿")
        @UserMessage("根据以下主题写一个简短的故事，只输出故事内容，不要多余的话\n主题：{{topic}}")
        String generateStory(@V("topic") String topic);
    }

    // 2. 受众编辑Agent
    public interface AudienceEditor {
        @Agent("根据目标受众重写故事")
        @UserMessage("原文：{{story}}\n为{{audience}}受众改写故事，只输出修改后的完整故事")
        String editStory(@V("story") String story, @V("audience") String audience);
    }

    // 3. 风格润色Agent
    public interface StyleEditor {
        @Agent("根据指定风格调整故事")
        @UserMessage("故事内容：{{story}}\n用{{style}}风格重写，直接输出最终故事")
        String editStyle(@V("story") String story, @V("style") String style);
    }

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

        CreativeWriter writer = AgenticServices.agentBuilder(CreativeWriter.class)
                .chatModel(baseModel)
                .outputKey("story")
                .build();

        AudienceEditor editor = AgenticServices.agentBuilder(AudienceEditor.class)
                .chatModel(baseModel)
                .outputKey("story")
                .build();

        StyleEditor styler = AgenticServices.agentBuilder(StyleEditor.class)
                .chatModel(baseModel)
                .outputKey("story")
                .build();

        // 错误恢复标志
        AtomicBoolean errorRecoveryCalled = new AtomicBoolean(false);

        // 创建带错误处理的工作流
        dev.langchain4j.agentic.UntypedAgent novelCreator = AgenticServices.sequenceBuilder()
                .subAgents(writer, editor, styler)
                .errorHandler(errorContext -> {
                    System.out.println("\n=====================错误捕获=====================");
                    System.out.println("Agent名称: " + errorContext.agentName());
                    System.out.println("异常类型: " + errorContext.exception().getClass().getSimpleName());
                    System.out.println("异常信息: " + errorContext.exception().getMessage());
                    
                    // 检查是否是缺少参数 topic 的错误
                    if (errorContext.agentName().equals("generateStory")
                            && errorContext.exception() instanceof MissingArgumentException mEx
                            && mEx.argumentName().equals("topic")) {
                        System.out.println("检测到缺少 topic 参数，自动修复...");
                        // 写入默认值到 AgenticScope
                        errorContext.agenticScope().writeState("topic", "龙与法师");
                        errorRecoveryCalled.set(true);
                        System.out.println("修复完成，重试Agent调用");
                        System.out.println("================================================\n");
                        return ErrorRecoveryResult.retry();
                    }
                    
                    // 其他错误，抛出异常
                    return ErrorRecoveryResult.throwException();
                })
                .outputKey("story")
                .build();

        System.out.println("=====================示例1：正常执行=====================");
        Map<String, Object> input1 = Map.of(
                "topic", "赛博朋克城市",
                "audience", "年轻人",
                "style", "悬疑"
        );
        String result1 = (String) novelCreator.invoke(input1);
        System.out.println("\n执行结果:");
        System.out.println(result1);

        System.out.println("\n\n=====================示例2：缺少topic参数，触发错误恢复=====================");
        // 故意不传 topic 参数，会触发错误恢复
        Map<String, Object> input2 = Map.of(
                // "topic", "赛博朋克城市",  // 故意注释掉
                "audience", "成年人",
                "style", "恐怖"
        );
        
        try {
            String result2 = (String) novelCreator.invoke(input2);
            System.out.println("\n执行结果:");
            System.out.println(result2);
            System.out.println("错误恢复被调用: " + errorRecoveryCalled.get());
        } catch (Exception e) {
            System.out.println("执行失败（未配置错误恢复）: " + e.getMessage());
        }
    }
}
