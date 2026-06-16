# LangChain4j 编码注意事项

## 1. AI Services 接口参数注解规则

**每个参数必须用 `@V`、`@UserMessage`、`@MemoryId` 之一标注，否则运行时报错**

### 错误信息
```
dev.langchain4j.service.IllegalConfigurationException: 
Parameter 'arg0' of method 'xxx' should be annotated with @V or @UserMessage or @UserName or @MemoryId
```

### 正确写法
```java
// ✅ 正确：所有参数都有注解
interface Writer {
    @UserMessage("主题：{{topic}}\n受众：{{audience}}")
    String write(
        @V("topic") String topic,
        @V("audience") String audience
    );
}

// ✅ 单参数用 {{it}} 引用
@UserMessage("主题：{{it}}")
String write(String topic);

// ❌ 错误：参数没有注解会报错
@UserMessage("主题：{{it}}")
String write(String topic);  // 运行时报错！
```

## 2. @UserMessage 模板语法

```java
// {{it}} - 引用单个参数（第一个未命名的参数）
@UserMessage("主题：{{it}}")
String write(String topic);

// {{变量名}} - 引用 @V 标注的参数
@UserMessage("主题：{{topic}}\n内容：{{content}}")
String write(
    @V("topic") String topic,
    @V("content") String content
);

// 模板字符串拼接
@UserMessage(
    "请处理以下请求：\n" +
    "类型：{{type}}\n" +
    "内容：{{content}}"
)
String handle(
    @V("type") String type,
    @V("content") String content
);
```

## 3. 环境变量

```bash
# 正确
export DEEPSEEK_API_KEY=your-key

# ❌ 错误：不是 DEEP_SEEK_API_KEY 或其他变体
```

## 4. Maven 依赖版本

### 稳定版本
```xml
<properties>
    <langchain4j.version>1.0.0</langchain4j.version>
</properties>

<dependencies>
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j</artifactId>
        <version>${langchain4j.version}</version>
    </dependency>
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-open-ai</artifactId>
        <version>${langchain4j.version}</version>
    </dependency>
</dependencies>
```

### langchain4j-agentic 版本问题
```
Could not resolve dependencies for project
dependency: dev.langchain4j:langchain4j-agentic:jar:1.16.2-beta26 was not found
```

`langchain4j-agentic` 模块版本与其他模块不兼容，Maven Central 上该模块版本落后很多。
**建议：手动实现工作流，不使用此模块。**

## 5. @Agent 注解（官方 API）

### Agent 核心概念
- **Agent** = 使用 LLM 执行特定任务的组件
- **outputName** = 输出存储到共享变量，供其他 Agent 使用
- **AgenticScope** = 多个 Agent 共享的数据集合

### 官方 API（需要 langchain4j-agentic 模块）
```java
public interface Writer {
    @UserMessage("根据主题创作故事：{{topic}}")
    @Agent(outputName = "story")
    String generateStory(@V("topic") String topic);
}

// 创建 Agent
Writer writer = AgenticServices
    .agentBuilder(Writer.class)
    .chatModel(model)
    .outputName("story")
    .build();

// 使用
String story = writer.generateStory("dragons");
```

### 手动实现（当前可用）
```java
// 定义 Agent 接口
interface WriterAgent {
    @UserMessage("创作故事：{{topic}}")
    String generateStory(@V("topic") String topic);
}

interface EditorAgent {
    @UserMessage("编辑故事：{{story}}")
    String editStory(@V("story") String story);
}

// 创建 Agent
WriterAgent writer = AiServices.create(WriterAgent.class, model);
EditorAgent editor = AiServices.create(EditorAgent.class, model);

// 顺序执行（模拟 AgenticScope）
String story = writer.generateStory(topic);
story = editor.editStory(story);
```

## 6. 工作流模式实现

### 顺序工作流
```java
// 按顺序调用，每个输出作为下一个输入
String story = writer.generateStory(topic);           // outputName = "story"
story = editor.editForAudience(story, audience);      // @V("story")
story = styleEditor.adjustStyle(story, style);         // @V("story")
```

### 循环工作流
```java
// while 循环 + 退出条件
int iteration = 0;
while (iteration < maxIterations) {
    double score = scorer.scoreStyle(result, style);   // outputName = "score"
    if (score >= 0.8) break;
    result = editor.improve(result);                    // @V("story")
    iteration++;
}
```

### 并行工作流
```java
// ExecutorService + Future
ExecutorService executor = Executors.newFixedThreadPool(2);
Future<List<String>> mealsFuture = executor.submit(() -> foodExpert.findMeal(mood));
Future<List<String>> moviesFuture = executor.submit(() -> movieExpert.findMovie(mood));
List<String> meals = mealsFuture.get();
List<String> movies = moviesFuture.get();
```

### 条件工作流
```java
// if-else 分支
RequestCategory category = classifier.classify(request);  // outputName = "category"
if (category == RequestCategory.MEDICAL) {
    response = medicalExpert.answer(request);             // @V("request")
} else if (category == RequestCategory.LEGAL) {
    response = legalExpert.answer(request);
}
```

## 7. 常见错误汇总

| 错误信息 | 原因 | 解决 |
|---------|------|------|
| `Parameter 'xxx' should be annotated with @V or @UserMessage` | 参数没有注解 | 给每个参数加 `@V("name")` |
| `NoClassDefFoundError: LangChain4jManaged` | `langchain4j-agentic` 版本不兼容 | 使用稳定版本 `1.0.0` |
| `Could not resolve dependencies` | 模块版本不存在 | 使用 `1.0.0` 替代 |
| `langchain4j-agentic:jar:xxx was not found` | beta 版本未发布到 Maven Central | 手动实现工作流 |
| `Failed to parse JSON` | LLM 输出格式不标准 | 使用 `responseFormat("json_object")` + 提示词含 "JSON" |
| `Prompt must contain the word 'json'` | DeepSeek JSON 模式要求 | 提示词中添加 "json" 关键词 |

## 8. 结构化输出（JSON 解析）

### 问题
```
dev.langchain4j.service.output.OutputParsingException: 
Failed to parse "{\"name\": \"张三\"}" into xxx
```

### 解决
```java
// 1. 使用 responseFormat
ChatModel model = OpenAiChatModel.builder()
    .baseUrl("https://api.deepseek.com")
    .apiKey(apiKey)
    .modelName("deepseek-chat")
    .responseFormat("json_object")  // 强制 JSON 模式
    .build();

// 2. 提示词包含 "JSON"
@UserMessage("返回 JSON 格式：{{text}}")

// 3. 清理 JSON（移除 markdown 代码块）
String cleanJson = json.replaceAll("```json", "").replaceAll("```", "").trim();
```

## 9. API Key 获取地址

| 服务 | 地址 |
|------|------|
| DeepSeek | https://platform.deepseek.com |
| OpenAI | https://platform.openai.com |
| 阿里云百炼 | https://bailian.console.aliyun.com |
| 小米 MiMo | https://platform.xiaomimimo.com |

## 10. LangChain4j 官方文档

- 中文文档：https://langchain4j.cn/
- 英文文档：https://docs.langchain4j.dev/
- GitHub：https://github.com/langchain4j/langchain4j
