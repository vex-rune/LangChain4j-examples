# AI Agent 工作流核心概念

## 什么是工作流？

**工作流 = 多个 AI 服务按一定规则组合执行**

工作流将复杂任务拆分为多个简单步骤，每个步骤由专门的 AI 服务处理。

## 为什么要用工作流？

| 问题 | 解决方案 |
|------|----------|
| 单一 AI 无法处理复杂任务 | 拆分为多个简单任务 |
| 提示词过长，AI 忽略部分指令 | 每个服务只关注自己的任务 |
| 需要不同类型的能力 | 不同服务处理不同场景 |
| 成本和延迟 | 按需调用，非必须不调用 |

## 四大工作流模式

### 1. 顺序工作流（Sequential）

**概念**：按顺序执行，上一步输出作为下一步输入

```
输入 → [服务A] → [服务B] → [服务C] → 输出
```

**适用场景**：
- 创作类任务：写 → 编辑 → 润色
- 处理类任务：提取 → 转换 → 格式化

**代码模式**：
```java
String result = serviceA.process(input);
result = serviceB.process(result);
result = serviceC.process(result);
```

---

### 2. 循环工作流（Loop）

**概念**：反复执行，直到满足退出条件

```
输入 → [检查] → 满足条件? ─┬─ 是 → 输出
                         └─ 否 → [改进] → 返回检查
```

**适用场景**：
- 反复优化直到达标
- 生成后不断改进质量
- 达到特定标准

**代码模式**：
```java
while (!isSatisfied(result) && iteration < maxIterations) {
    result = service.improve(result);
}
```

---

### 3. 并行工作流（Parallel）

**概念**：多个服务同时执行，最后合并结果

```
              ┌─ [服务A] ─┐
输入 ────────→┤           ├→ 合并 → 输出
              └─ [服务B] ─┘
```

**适用场景**：
- 多个独立任务同时处理
- 减少总等待时间
- 生成多个相关但独立的结果

**代码模式**：
```java
ExecutorService executor = Executors.newFixedThreadPool(2);
Future<A> futureA = executor.submit(() -> serviceA.process(input));
Future<B> futureB = executor.submit(() -> serviceB.process(input));
A resultA = futureA.get();
B resultB = futureB.get();
```

---

### 4. 条件工作流（Conditional）

**概念**：根据条件选择不同的执行路径

```
              ┌─ 条件1 ─→ [服务A]
输入 → [分类] →┤─ 条件2 ─→ [服务B]
              └─ 条件3 ─→ [服务C]
```

**适用场景**：
- 路由到不同专家
- 根据输入类型选择处理方式
- 智能分发任务

**代码模式**：
```java
String category = classifier.categorize(input);
if (category.equals("A")) {
    return serviceA.process(input);
} else if (category.equals("B")) {
    return serviceB.process(input);
}
```

---

## 工作流的核心思想

### 1. 单一职责
每个 AI 服务只做一件事，做得好

### 2. 可组合性
简单服务可以组合成复杂工作流

### 3. 可复用性
同一个服务可用于不同工作流

### 4. 可控性
通过条件控制何时调用什么服务

## 实际应用示例

### 内容审核系统
```
[接收内容] → [分类] → ┬─ 医疗 → [医疗专家]
                     ├─ 法律 → [法律专家]
                     ├─ 技术 → [技术专家]
                     └─ 普通 → [直接发布]
```

### 智能客服
```
[接收问题] → [意图识别] → ┬─ 咨询 → [知识库检索] → [整理答案]
                         ├─ 投诉 → [情感分析] → [优先级排序]
                         └─ 退订 → [确认身份] → [执行退订]
```

### 文章生成
```
[主题] → [大纲生成] → [并行写章节] → [合并] → [校对] → [润色] → 发布
```

## 总结

| 模式 | 关键词 | 何时使用 |
|------|--------|----------|
| 顺序 | 按步骤 | 固定的处理流程 |
| 循环 | 重复、改进 | 需要反复优化 |
| 并行 | 同时、并发 | 独立任务 |
| 条件 | 判断、分支 | 根据情况选择 |
