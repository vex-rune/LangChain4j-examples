package com.langchain4j.examples;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;

import java.util.Map;
import java.util.UUID;

/**
 * Document 类示例
 * 
 * Document 是 LangChain4j 中表示文档的核心类
 * 
 * 主要功能：
 * 1. 存储文档文本内容
 * 2. 存储元数据（键值对）
 * 3. 转换为 TextSegment（用于向量存储）
 * 
 * Document 结构：
 * ┌─────────────────────────────────────────┐
 * │            Document                     │
 * ├─────────────────────────────────────────┤
 * │  text: String                           │
 * │  metadata: Metadata                     │
 * │    - key: value                         │
 * │    - source: 文件路径/URL                │
 * │    - owner: 所有者                       │
 * │    - id: 唯一标识                        │
 * └─────────────────────────────────────────┘
 */
public class _01_DocumentDemo {

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("                    Document 类示例");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();

        // =====================================================
        // 1. 创建 Document 的几种方式
        // =====================================================
        System.out.println("【1】创建 Document");
        System.out.println();

        // 方式1：从文本创建（自动带空 Metadata）
        Document doc1 = Document.from("这是文档的文本内容");
        System.out.println("方式1 - Document.from(text):");
        System.out.println("  文本: " + doc1.text());
        System.out.println("  元数据: " + doc1.metadata().toMap());
        System.out.println();

        // 方式2：从文本和 Metadata 创建
        Metadata metadata = Metadata.from(Map.of(
            "source", "用户手册",
            "page", 1,
            "author", "张三"
        ));
        Document doc2 = Document.from("这是带元数据的文档内容", metadata);
        System.out.println("方式2 - Document.from(text, metadata):");
        System.out.println("  文本: " + doc2.text());
        System.out.println("  元数据: " + doc2.metadata().toMap());
        System.out.println();

        // =====================================================
        // 2. Metadata 操作
        // =====================================================
        System.out.println("【2】Metadata 操作");
        System.out.println();

        Metadata meta = new Metadata();
        
        // 添加各种类型的值
        meta.put("source", "https://example.com/doc");
        meta.put("owner", "李四");
        meta.put("pageCount", 10);
        meta.put("version", 1.5);
        meta.put("lastUpdated", System.currentTimeMillis());
        meta.put("id", UUID.randomUUID().toString());

        System.out.println("添加元数据后:");
        System.out.println("  source: " + meta.getString("source"));
        System.out.println("  owner: " + meta.getString("owner"));
        System.out.println("  pageCount: " + meta.getInteger("pageCount"));
        System.out.println("  version: " + meta.getDouble("version"));
        System.out.println("  id: " + meta.getString("id"));
        System.out.println();

        // 检查键是否存在
        System.out.println("检查键是否存在:");
        System.out.println("  contains 'source': " + meta.containsKey("source"));
        System.out.println("  contains 'missing': " + meta.containsKey("missing"));
        System.out.println();

        // 复制和合并
        System.out.println("复制和合并:");
        Metadata metaCopy = meta.copy();
        System.out.println("  复制后的元数据: " + metaCopy.toMap());

        Metadata extra = Metadata.from(Map.of("department", "技术部"));
        metaCopy.putAll(extra.toMap());
        System.out.println("  合并后的元数据: " + metaCopy.toMap());
        System.out.println();

        // 删除
        System.out.println("删除操作:");
        metaCopy.remove("id");
        System.out.println("  删除 'id' 后的元数据: " + metaCopy.toMap());
        System.out.println();

        // =====================================================
        // 3. Document 转 TextSegment
        // =====================================================
        System.out.println("【3】Document 转换为 TextSegment");
        System.out.println();

        Document doc3 = Document.from("这是一段要转换为 TextSegment 的文本");
        doc3.metadata().put("index", 0);
        doc3.metadata().put("chapter", "第一章");

        var textSegment = doc3.toTextSegment();
        System.out.println("转换结果:");
        System.out.println("  TextSegment text: " + textSegment.text());
        System.out.println("  TextSegment metadata: " + textSegment.metadata().toMap());
        System.out.println();

        // =====================================================
        // 4. 实际应用示例
        // =====================================================
        System.out.println("【4】实际应用示例");
        System.out.println();

        // 模拟加载的文档集合
        Document docA = Document.from(
            "LangChain4j 是一个 Java 的 LLM 开发框架",
            Metadata.from(Map.of(
                "source", "langchain4j.md",
                "type", "技术文档",
                "author", "开发团队"
            ))
        );

        Document docB = Document.from(
            "RAG 是检索增强生成技术",
            Metadata.from(Map.of(
                "source", "rag-guide.md",
                "type", "教程",
                "author", "教学组"
            ))
        );

        System.out.println("文档集合:");
        for (Document doc : java.util.List.of(docA, docB)) {
            System.out.println("  - 文本: " + doc.text());
            System.out.println("    来源: " + doc.metadata().getString("source"));
            System.out.println("    类型: " + doc.metadata().getString("type"));
            System.out.println("    作者: " + doc.metadata().getString("author"));
            System.out.println();
        }

        // 模拟按类型过滤
        System.out.println("按类型 '技术文档' 过滤:");
        for (Document doc : java.util.List.of(docA, docB)) {
            if ("技术文档".equals(doc.metadata().getString("type"))) {
                System.out.println("  匹配: " + doc.text());
            }
        }
        System.out.println();

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("                    执行完成");
        System.out.println("═══════════════════════════════════════════════════════════════");
    }
}
