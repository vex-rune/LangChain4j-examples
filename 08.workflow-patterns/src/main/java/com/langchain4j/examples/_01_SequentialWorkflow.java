package com.langchain4j.examples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.Map;

public class _01_SequentialWorkflow {

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
                .logRequests(true)   // 打印请求体（完整prompt、参数）
                .logResponses(true)  // 打印大模型返回原始内容
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

        dev.langchain4j.agentic.UntypedAgent novelCreator = AgenticServices.sequenceBuilder()
                .subAgents(writer, editor, styler)
                .outputKey("story")
                .build();

        Map<String, Object> input = Map.of(
                "topic", "灵笼3",
                "audience", "年轻人",
                "style", "赛博朋克"
        );

        String finalStory = (String) novelCreator.invoke(input);

        System.out.println("=====================最终成品=====================");
        System.out.println(finalStory);
    }
}
