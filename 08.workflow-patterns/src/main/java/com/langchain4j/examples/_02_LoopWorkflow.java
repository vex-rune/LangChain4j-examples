package com.langchain4j.examples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.Map;

public class _02_LoopWorkflow {

    // 1. 风格评分Agent
    public interface StyleScorer {
        @Agent("评估故事风格匹配度，输出0.0-1.0的分数")
        @UserMessage("评估以下故事与「{{style}}」风格的匹配度。\n只输出一个0.0到1.0之间的数字。\n故事：{{story}}")
        double scoreStyle(@V("story") String story, @V("style") String style);
    }

    // 2. 风格编辑Agent
    public interface StyleEditor {
        @Agent("根据目标风格编辑故事")
        @UserMessage("将以下故事改写为{{style}}风格。\n只输出修改后的故事，不要其他内容。\n原文：{{story}}")
        String editStory(@V("story") String story, @V("style") String style);
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

        StyleScorer scorer = AgenticServices.agentBuilder(StyleScorer.class)
                .chatModel(baseModel)
                .outputKey("score")
                .build();

        StyleEditor editor = AgenticServices.agentBuilder(StyleEditor.class)
                .chatModel(baseModel)
                .outputKey("story")
                .build();

        dev.langchain4j.agentic.UntypedAgent styleReviewLoop = AgenticServices.loopBuilder()
                .subAgents(scorer, editor)
                .maxIterations(5)
                .exitCondition(scope -> scope.readState("score", 0.0) >= 0.8)
                .build();

        Map<String, Object> input = Map.of(
                "story", "在一个龙与法师共存的世界里，年轻的英雄必须使用古老魔法力量和勇气拯救王国。",
                "style", "喜剧"
        );

        System.out.println("=====================循环工作流开始=====================");
        String result = (String) styleReviewLoop.invoke(input);
        System.out.println("=====================循环工作流完成=====================");
        System.out.println(result);
    }
}
