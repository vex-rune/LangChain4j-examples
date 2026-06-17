package com.langchain4j.examples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.*;

public class _08_ScopeAndPersistence {

    // =====================================================
    // 定义数据类型
    // =====================================================

    public enum RequestCategory {
        LEGAL, MEDICAL, TECH, OTHER
    }

    // =====================================================
    // 自定义持久化存储
    // =====================================================

    /**
     * 基于内存的 AgenticScopeStore 实现
     * <p>
     * 实际应用中可以将数据存储到数据库、Redis、文件系统等
     */
    public static class InMemoryAgenticScopeStore implements AgenticScopeStore {

        private final Map<String, String> storage = new HashMap<>();

        @Override
        public boolean save(AgenticScopeKey scopeKey, DefaultAgenticScope scope) {
            String key = scopeKey.agentId() + ":" + scopeKey.memoryId();
            String value = scope == null ? null : scope.toString(); // 实际应用中应该序列化
            storage.put(key, value);
            System.out.println("【持久化】保存 scope: " + key);
            System.out.println("  内容: " + value );
            return true;
        }

        @Override
        public Optional<DefaultAgenticScope> load(AgenticScopeKey scopeKey) {
            String key = scopeKey.agentId() + ":" + scopeKey.memoryId();
            String value = storage.get(key);
            System.out.println("【持久化】加载 scope: " + key);
            if (value != null) {
                System.out.println("  找到数据: " + (value.length() > 50 ? value.substring(0, 50) + "..." : value));
                // 实际应用中应该反序列化
                return Optional.empty();
            }
            System.out.println("  未找到数据");
            return Optional.empty();
        }

        @Override
        public boolean delete(AgenticScopeKey scopeKey) {
            String key = scopeKey.agentId() + ":" + scopeKey.memoryId();
            boolean removed = storage.remove(key) != null;
            System.out.println("【持久化】删除 scope: " + key + " -> " + (removed ? "成功" : "失败"));
            return removed;
        }

        @Override
        public Set<AgenticScopeKey> getAllKeys() {
            System.out.println("【持久化】获取所有 scopeKey:");
            Set<AgenticScopeKey> keys = new HashSet<>();
            for (String k : storage.keySet()) {
                String[] parts = k.split(":");
                if (parts.length == 2) {
                    keys.add(new AgenticScopeKey(parts[0], parts[1]));
                }
            }
            System.out.println("  数量: " + keys.size());
            return keys;
        }

        // 额外方法：查看存储内容（用于调试）
        public void printStorage() {
            System.out.println("【存储内容】共 " + storage.size() + " 条记录:");
            storage.forEach((k, v) -> System.out.println("  " + k + " -> " + v));
        }
    }

    // =====================================================
    // 带内存的Agent
    // =====================================================

    // 1. 医疗专家Agent
    public interface MedicalExpert {
        @Agent(name = "医疗专家", outputKey = "response")
        @UserMessage("你是一个医疗健康专家。回答：{{request}}")
        String medical(@MemoryId String memoryId, @V("request") String request);
    }

    // 2. 技术专家Agent
    public interface TechnicalExpert {
        @Agent(name = "技术专家", outputKey = "response")
        @UserMessage("你是一个技术支持专家。回答：{{request}}")
        String technical(@MemoryId String memoryId, @V("request") String request);
    }

    // 3. 分类路由Agent
    public interface CategoryRouter {
        @Agent(name = "分类路由", outputKey = "category")
        @UserMessage("分析请求，分类为：MEDICAL/TECH/OTHER\n只输出英文单词\n请求：{{request}}")
        RequestCategory classify(@V("request") String request);
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
//                .logRequests(true)
                .build();

        // 创建持久化存储
        InMemoryAgenticScopeStore store = new InMemoryAgenticScopeStore();
        AgenticScopePersister.setStore(store);

        // =====================================================
        // 示例1：基本持久化操作
        // =====================================================
        System.out.println("=====================示例1：基本持久化操作=====================");
        System.out.println();

        // 保存
        AgenticScopeKey key1 = new AgenticScopeKey("MedicalExpert", "user-1");
        store.save(key1, null);

        AgenticScopeKey key2 = new AgenticScopeKey("TechnicalExpert", "user-1");
        store.save(key2, null);

        // 加载
        store.load(key1);

        // 获取所有键
        store.getAllKeys();

        // 删除
        store.delete(key1);

        // 查看存储内容
        store.printStorage();

        // =====================================================
        // 示例2：带持久化的专家问答
        // =====================================================
        System.out.println("\n\n=====================示例2：带持久化的专家问答=====================");
        System.out.println("对话数据会被持久化存储");
        System.out.println();

        MedicalExpert medExpert = AgenticServices.agentBuilder(MedicalExpert.class)
                .chatModel(baseModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .outputKey("response")
                .build();

        TechnicalExpert techExpert = AgenticServices.agentBuilder(TechnicalExpert.class)
                .chatModel(baseModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .outputKey("response")
                .build();

        CategoryRouter router = AgenticServices.agentBuilder(CategoryRouter.class)
                .chatModel(baseModel)
                .outputKey("category")
                .build();

        dev.langchain4j.agentic.UntypedAgent expertsSystem = AgenticServices.conditionalBuilder()
                .subAgents(scope -> scope.readState("category", RequestCategory.OTHER) == RequestCategory.MEDICAL, medExpert)
                .subAgents(scope -> scope.readState("category", RequestCategory.OTHER) == RequestCategory.TECH, techExpert)
                .build();

        dev.langchain4j.agentic.UntypedAgent expertRouter = AgenticServices.sequenceBuilder()
                .subAgents(router, expertsSystem)
                .outputKey("response")
                .build();

        String[] requests = {"我头痛应该怎么办？", "如何用Java写一个类？"};

        for (String request : requests) {
            System.out.println("问题: " + request);
            String response = (String) expertRouter.invoke(Map.of("request", request));
            System.out.println("回答: " + response.length());
            System.out.println();
        }

        // 查看持久化后的存储
        store.printStorage();

        System.out.println("\n=====================执行完成=====================");
    }
}