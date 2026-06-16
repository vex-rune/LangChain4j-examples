# LangChain4j Examples

LangChain4j 快速开始示例

## 项目结构

```
langchain4j-examples/
├── pom.xml
└── src/main/java/com/langchain4j/examples/
    └── QuickStartExample.java
```

## 快速开始

### 1. 编译项目

```bash
mvn clean compile
```

### 2. 运行示例

使用演示密钥（有限制）：
```bash
mvn exec:java -Dexec.mainClass="com.langchain4j.examples.QuickStartExample"
```

使用自己的 OpenAI API Key：
```bash
export OPENAI_API_KEY=your-api-key
mvn exec:java -Dexec.mainClass="com.langchain4j.examples.QuickStartExample"
```

## 参考资料

- [LangChain4j 官方文档](https://langchain4j.dev/)
- [LangChain4j 中文教程](https://langchain4j.cn/)
- [快速开始](https://langchain4j.cn/get-started/)