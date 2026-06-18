package com.langchain4j.examples;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentByCharacterSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.document.transformer.jsoup.HtmlToTextDocumentTransformer;

/**
 * 文档转换器示例
 */
public class _03_DocumentTransformers {

    public static void main(String[] args) {
        // 1. HtmlToTextDocumentTransformer - HTML 转文本
        String html = """
                <html>
                <body>
                    <h1>LangChain4j 介绍</h1>
                    <p>LangChain4j 是一个 <strong>Java 的 LLM 开发框架</strong>。</p>
                    <div class="sidebar">侧边栏内容</div>
                </body>
                </html>
                """;

        HtmlToTextDocumentTransformer transformer = new HtmlToTextDocumentTransformer();
        Document htmlDoc = Document.from(html, Metadata.from(java.util.Map.of("source", "test.html")));
        Document cleanDoc = transformer.transform(htmlDoc);
        System.out.println("HtmlToTextDocumentTransformer:");
        System.out.println("  原文: " + html);
        System.out.println("  结果: " + cleanDoc.text());

        // 2. DocumentSplitter
        DocumentBySentenceSplitter sentenceSplitter = new DocumentBySentenceSplitter(100, 20);
        // 3. DocumentByParagraphSplitter - 按段落分割
        DocumentByParagraphSplitter paragraphSplitter = new DocumentByParagraphSplitter(100, 20);
        // 4. DocumentByCharacterSplitter - 按字符数分割
        DocumentByCharacterSplitter characterSplitter = new DocumentByCharacterSplitter(100, 20);

    }}
