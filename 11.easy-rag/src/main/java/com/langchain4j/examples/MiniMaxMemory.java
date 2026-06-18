package com.langchain4j.examples;

import dev.langchain4j.data.embedding.Embedding;

import java.util.*;

/**
 * MiniMax Memory 类 - 简化版向量内存
 * 
 * 模拟 Python 版本的 Memory 类：
 * - save_memory: 存储文本和 embedding
 * - retrieve: 根据 query 检索相关文本
 * 
 * 注意：实际生产环境应使用向量数据库（如 Milvus、Pinecone 等）
 */
public class MiniMaxMemory {

    private final List<MemoryItem> data = new ArrayList<>();
    private final MinimaxEmbeddingModel embeddingModel;

    public MiniMaxMemory(MinimaxEmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * 存储文本及其 embedding
     */
    public void saveMemory(String text) {
        Embedding embedding = embeddingModel.embed(text, MinimaxEmbeddingModel.EmbeddingType.DB).content();
        float[] vector = new float[embedding.dimension()];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = embedding.vectorAsList().get(i).floatValue();
        }
        
        data.add(new MemoryItem(text, vector));
        System.out.println("  ✓ 已存储: " + text.substring(0, Math.min(30, text.length())) + "...");
    }

    /**
     * 根据 query 检索 topk 个相关文本
     */
    public String retrieve(String query, int topk) {
        // 使用 query 类型生成查询 embedding
        Embedding queryEmbedding = embeddingModel.embed(query, MinimaxEmbeddingModel.EmbeddingType.QUERY).content();
        float[] queryVector = new float[queryEmbedding.dimension()];
        for (int i = 0; i < queryVector.length; i++) {
            queryVector[i] = queryEmbedding.vectorAsList().get(i).floatValue();
        }
        
        // 计算相似度并排序
        List<MemoryItem> sorted = new ArrayList<>(data);
        sorted.sort((a, b) -> {
            double simA = embeddingModel.cosineSimilarity(a.embedding, queryVector);
            double simB = embeddingModel.cosineSimilarity(b.embedding, queryVector);
            return Double.compare(simB, simA); // 降序
        });

        // 取 topk
        List<String> topTexts = new ArrayList<>();
        for (int i = 0; i < Math.min(topk, sorted.size()); i++) {
            topTexts.add(sorted.get(i).text);
        }

        // 构建上下文
        return """
                使用根据以下内容来回答问题。如果你不知道答案，就说你不知道，不要试图编造答案。

                """
                + String.join("\n----\n", topTexts);
    }

    /**
     * 内存项
     */
    private static class MemoryItem {
        final String text;
        final float[] embedding;

        MemoryItem(String text, float[] embedding) {
            this.text = text;
            this.embedding = embedding;
        }
    }
}
