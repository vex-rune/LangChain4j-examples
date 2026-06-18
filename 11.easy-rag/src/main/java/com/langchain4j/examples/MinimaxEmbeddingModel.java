package com.langchain4j.examples;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MiniMax Embedding 模型实现
 * 
 * MiniMax Embedding API:
 * - 模型: embo-01
 * - 维度: 1536
 * - API: https://api.minimax.chat/v1/embeddings
 * 
 * 支持两种 type:
 * - db: 用于存储到向量数据库
 * - query: 用于查询/检索
 */
public class MinimaxEmbeddingModel implements EmbeddingModel {

    public enum EmbeddingType {
        DB("db"),
        QUERY("query");

        private final String value;

        EmbeddingType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private final String apiKey;
    private final String groupId;
    private final String modelName = "embo-01";
    private final HttpClient httpClient;

    public MinimaxEmbeddingModel(String apiKey, String groupId) {
        this.apiKey = apiKey;
        this.groupId = groupId;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public Response<Embedding> embed(String text) {
        return embed(text, EmbeddingType.DB);
    }

    /**
     * 嵌入文本，支持指定 type
     * @param text 文本
     * @param type 类型：db(存储) 或 query(查询)
     * @return Embedding 响应
     */
    public Response<Embedding> embed(String text, EmbeddingType type) {
        try {
            String requestBody = String.format("""
                {
                    "texts": ["%s"],
                    "model": "%s",
                    "type": "%s"
                }
                """, text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n"), modelName, type.getValue());

            String url = "https://api.minimax.chat/v1/embeddings?GroupId=" + groupId;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("MiniMax API error: " + response.statusCode() + " - " + response.body());
            }

            float[] vectors = parseVectorsFromResponse(response.body());
            return Response.from(Embedding.from(vectors));

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to call MiniMax Embedding API", e);
        }
    }

    @Override
    public Response<Embedding> embed(TextSegment textSegment) {
        return embed(textSegment.text());
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        try {
            StringBuilder textsJson = new StringBuilder("[");
            for (int i = 0; i < textSegments.size(); i++) {
                String text = textSegments.get(i).text()
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n");
                textsJson.append("\"").append(text).append("\"");
                if (i < textSegments.size() - 1) {
                    textsJson.append(",");
                }
            }
            textsJson.append("]");

            String requestBody = String.format("""
                {
                    "texts": %s,
                    "model": "%s",
                    "type": "%s"
                }
                """, textsJson, modelName, EmbeddingType.DB.getValue());

            String url = "https://api.minimax.chat/v1/embeddings?GroupId=" + groupId;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("MiniMax API error: " + response.statusCode() + " - " + response.body());
            }

            List<Embedding> embeddings = parseAllVectorsFromResponse(response.body());
            return Response.from(embeddings);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to call MiniMax Embedding API", e);
        }
    }

    @Override
    public int dimension() {
        return 1536;
    }

    @Override
    public String modelName() {
        return modelName;
    }

    /**
     * 计算两个向量的点积
     */
    private double dotProduct(float[] a, float[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    /**
     * 计算向量的 L2 范数
     */
    private double norm(float[] vector) {
        double sum = 0;
        for (float v : vector) {
            sum += v * v;
        }
        return Math.sqrt(sum);
    }

    /**
     * 计算两个向量的余弦相似度
     */
    public double cosineSimilarity(float[] a, float[] b) {
        return dotProduct(a, b) / (norm(a) * norm(b));
    }

    private float[] parseVectorsFromResponse(String response) {
        Pattern pattern = Pattern.compile("\"vectors\"\\s*:\\s*\\[\\[(.*?)\\]\\]");
        Matcher matcher = pattern.matcher(response);
        
        if (matcher.find()) {
            String arrayStr = matcher.group(1);
            return parseFloatArray(arrayStr);
        }
        
        throw new RuntimeException("Failed to parse vectors from response: " + response);
    }

    private List<Embedding> parseAllVectorsFromResponse(String response) {
        List<Embedding> embeddings = new ArrayList<>();
        
        Pattern outerPattern = Pattern.compile("\"vectors\"\\s*:\\s*(\\[\\[.*?\\]\\])", Pattern.DOTALL);
        Matcher outerMatcher = outerPattern.matcher(response);
        
        if (outerMatcher.find()) {
            String vectorsArray = outerMatcher.group(1);
            
            Pattern innerPattern = Pattern.compile("\\[([\\d.,\\s-]+)\\]");
            Matcher innerMatcher = innerPattern.matcher(vectorsArray);
            
            while (innerMatcher.find()) {
                String arrayStr = innerMatcher.group(1);
                float[] vectors = parseFloatArray(arrayStr);
                embeddings.add(Embedding.from(vectors));
            }
        }
        
        if (embeddings.isEmpty()) {
            throw new RuntimeException("Failed to parse embeddings from response: " + response);
        }
        
        return embeddings;
    }

    private float[] parseFloatArray(String arrayStr) {
        String[] parts = arrayStr.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}
