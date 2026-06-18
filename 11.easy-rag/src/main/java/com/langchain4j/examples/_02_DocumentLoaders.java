package com.langchain4j.examples;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.loader.UrlDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * 文档加载器示例
 */
public class _02_DocumentLoaders {

    public static void main(String[] args) throws Exception {
        // 1. FileSystemDocumentLoader - 从文件系统加载
        Path filePath = Path.of(Objects.requireNonNull(_02_DocumentLoaders.class.getClassLoader()
                .getResource("sample.txt")).toURI());
        if (Files.exists(filePath)) {
            Document doc = FileSystemDocumentLoader.loadDocument(filePath);
            System.out.println("FileSystemDocumentLoader: " + doc.text().substring(0, 30));
        }

        // 2. UrlDocumentLoader - 从 URL 加载
        try {
            URL url = new URL("https://www.example.com");
            Document urlDoc = UrlDocumentLoader.load(url, new TextDocumentParser());
            System.out.println("UrlDocumentLoader: " + urlDoc.text().substring(0, 30));
        } catch (Exception e) {
            System.out.println("UrlDocumentLoader: " + e.getMessage());
        }

    }
}
