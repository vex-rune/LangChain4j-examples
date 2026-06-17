# 声明式 API（Declarative API）

声明式 API 允许通过注解直接定义工作流，比编程式 API 更简洁易读。

## pom.xml 配置

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-agentic</artifactId>
    <version>1.16.1-beta26</version>
</dependency>
```

```xml
<!-- 编译时保留参数名（用于 @V 省略）-->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <parameters>true</parameters>
    </configuration>
</plugin>
```

## @Agent 注解

### 正确用法
```java
// ✅ 使用 name 和 outputKey
@Agent(name = "美食专家", outputKey = "meals")
List<String> findMeal(String mood);
```

### 错误用法
```java
// ❌ 不是 description
@Agent(description = "xxx")

// ❌ 不是 outputName
@Agent(name = "xxx", outputName = "xxx")
```

## 工作流注解

### 并行工作流
```java
@ParallelAgent(outputKey = "plans", subAgents = {FoodExpert.class, MovieExpert.class})
List<EveningPlan> plan(String mood);
```

### 顺序工作流
```java
@SequenceAgent(outputKey = "category", subAgents = {CategoryRouter.class})
RequestCategory categorize(String request);
```

### 循环工作流
```java
@LoopAgent(subAgents = {Scorer.class, Editor.class}, maxIterations = 5)
@ExitCondition
boolean shouldContinue(AgenticScope scope);
```

### 条件工作流
```java
@ConditionalAgent(outputKey = "response", subAgents = {MedicalExpert.class, TechExpert.class, LegalExpert.class})
String ask(String request);
```

## subAgents 用法

```java
// ✅ 直接用 Class[]
subAgents = {FoodExpert.class, MovieExpert.class}

// ❌ 不要用 @SubAgent 注解（当前版本不支持）
subAgents = {@SubAgent(type = FoodExpert.class, ...)}
```

## @V 省略

```java
// ✅ 编译时保留参数名，可以省略 @V
@Agent(outputKey = "meals")
@UserMessage("氛围：{{mood}}")
List<String> findMeal(String mood);

// ❌ 没有 -parameters，需要 @V
List<String> findMeal(@V("mood") String mood);
```

## 枚举值用英文

```java
// ✅ 英文枚举值，避免中文解析问题
enum RequestCategory {
    LEGAL, MEDICAL, TECH, OTHER
}

// ❌ 中文枚举值可能解析失败
enum RequestCategory {
    法律, 医疗, 技术, 其他
}
```

## @Output 注解

```java
// 定义如何合并子Agent的输出
@Output
static List<EveningPlan> createPlans(
    @V("movies") List<String> movies,
    @V("meals") List<String> meals
) {
    List<EveningPlan> plans = new ArrayList<>();
    for (int i = 0; i < Math.min(movies.size(), meals.size()); i++) {
        plans.add(new EveningPlan(movies.get(i), meals.get(i)));
    }
    return plans;
}
```

## @ActivationCondition 注解

```java
// 定义何时激活特定的Agent
@ActivationCondition(MedicalExpert.class)
static boolean activateMedical(@V("category") RequestCategory category) {
    return category == RequestCategory.MEDICAL;
}

@ActivationCondition(TechnicalExpert.class)
static boolean activateTechnical(@V("category") RequestCategory category) {
    return category == RequestCategory.TECH;
}
```

## @ExitCondition 注解

```java
// 定义循环退出条件
@ExitCondition
static boolean shouldExit(AgenticScope scope) {
    double score = scope.readState("score", 0.0);
    return score >= 0.8;
}
```

## 完整示例

### 并行工作流 - 晚间规划
```java
public interface FoodExpert {
    @Agent(name = "美食专家", outputKey = "meals")
    @UserMessage("根据「{{mood}}」氛围推荐3道餐点，每行一个")
    List<String> findMeal(String mood);
}

public interface MovieExpert {
    @Agent(name = "电影专家", outputKey = "movies")
    @UserMessage("根据「{{mood}}」氛围推荐3部电影，每行一个")
    List<String> findMovie(String mood);
}

public interface EveningPlannerAgent {
    @ParallelAgent(outputKey = "plans", subAgents = {FoodExpert.class, MovieExpert.class})
    List<EveningPlan> plan(String mood);

    @Output
    static List<EveningPlan> createPlans(
        @V("movies") List<String> movies,
        @V("meals") List<String> meals
    ) {
        List<EveningPlan> plans = new ArrayList<>();
        for (int i = 0; i < Math.min(movies.size(), meals.size()); i++) {
            plans.add(new EveningPlan(movies.get(i), meals.get(i)));
        }
        return plans;
    }
}

// 使用
EveningPlannerAgent planner = AgenticServices.createAgenticSystem(EveningPlannerAgent.class, model);
List<EveningPlan> plans = planner.plan("浪漫");
```

### 条件工作流 - 专家问答
```java
enum RequestCategory {
    LEGAL, MEDICAL, TECH, OTHER
}

public interface MedicalExpert {
    @Agent(name = "医疗专家", outputKey = "response")
    @UserMessage("你是一个医疗专家。回答：{{request}}")
    String medical(String request);
}

public interface TechnicalExpert {
    @Agent(name = "技术专家", outputKey = "response")
    @UserMessage("你是一个技术专家。回答：{{request}}")
    String technical(String request);
}

public interface ExpertRouterAgent {
    @SequenceAgent(outputKey = "category", subAgents = {CategoryRouter.class})
    RequestCategory categorize(String request);

    @ConditionalAgent(outputKey = "response", subAgents = {MedicalExpert.class, TechnicalExpert.class})
    String ask(String request);

    @ActivationCondition(MedicalExpert.class)
    static boolean activateMedical(@V("category") RequestCategory category) {
        return category == RequestCategory.MEDICAL;
    }

    @ActivationCondition(TechnicalExpert.class)
    static boolean activateTechnical(@V("category") RequestCategory category) {
        return category == RequestCategory.TECH;
    }
}

// 使用
ExpertRouterAgent router = AgenticServices.createAgenticSystem(ExpertRouterAgent.class, model);
String response = router.ask("我摔断了腿怎么办？");
```

## 常用注解汇总

| 注解 | 作用 | 注解位置 |
|------|------|----------|
| `@Agent` | 定义单个Agent | 接口 |
| `@ParallelAgent` | 并行执行多个Agent | 方法 |
| `@SequenceAgent` | 顺序执行多个Agent | 方法 |
| `@LoopAgent` | 循环执行Agent | 方法 |
| `@ConditionalAgent` | 条件选择Agent | 方法 |
| `@SubAgent` | 指定子Agent（当前版本不支持） | - |
| `@Output` | 合并子Agent输出 | 静态方法 |
| `@ActivationCondition` | Agent激活条件 | 静态方法 |
| `@ExitCondition` | 循环退出条件 | 静态方法 |
| `@ExecutorService` | 线程池配置（当前版本不支持） | 静态方法 |
