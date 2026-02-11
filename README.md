# Lazy Form Instructor - LLM-driven Form Engine

一个工程级、可复用、可扩展的 Java 库，用于从 LLM 中提取结构化数据，并内置验证、置信度评分和重试逻辑。灵感来自 Python 的 `instructor`。

> **目标**：一句话输入 → 自动完成复杂表单的解析、校验、补全、展示与提交。
> 
> 它要做到 "实用、准确、智能、可交互确认"，不仅是一个简单的"填空"功能，而是一个"人机协作填单助手"。

---

## 核心特性

### LazyFormInstructor - 懒得填表单解析器, 用于表单的填写

- **Schema-First (模式驱动)**: 使用标准 JSON Schema 定义数据结构。
- **Decoupled (解耦设计)**: 将表单结构定义 (Business Logic) 与自然语言解析 (AI Logic) 分离。
- **Validation Loop (验证循环)**: 如果输出不匹配 schema，自动重试并让 LLM 修正。
- **Rich Output (丰富输出)**: 返回值、置信度、推理过程和备选值。
- **Provider Agnostic (提供者无关)**: 基于 Spring AI，支持 OpenAI、Azure 等多种 LLM 提供商。
- **Complex Forms (复杂表单)**: 支持嵌套对象、数组和复杂的跨字段约束。
- **Context-Aware (上下文感知)**: 使用 `Intent Context` (用户资料、历史、环境) 来推断默认值。

---

## 通用智能表单协议 (Universal Smart Form Protocol - USFP)

### 核心概念

1. **Form Schema**: 用 JSON Schema 描述表单结构（字段、类型、约束、枚举、依赖关系）。
2. **Intent Context**: 用户当前的上下文（时间、地点、用户偏好、历史操作）。
3. **Parsing Result**: 标准化的解析结果，包含：
   - `value`: 提取的值
   - `confidence`: 0.0 - 1.0 的置信度分数
   - `reasoning`: 解释提取逻辑
   - `alternatives`: 其他候选值（用于歧义消解）

### 数据结构示例

**输入 (JSON)**:
```json
{
  "schema": { ...JSON Schema definition... },
  "user_input": "I'm getting married next month! I'd like to take a week off starting from December 15th.",
  "context": { "now": "2023-10-27T10:00:00Z" }
}
```


* the json schema can be generated from the DTO

```java
/**
 * DTO for Leave Request form.
 * This class is used to generate JSON Schema for form validation.
 */
public class LeaveRequestForm {

    @SchemaRequired
    @SchemaEnum({"annual", "sick", "unpaid"})
    @SchemaDescription("Type of leave request")
    private String leaveType;

    @SchemaRequired
    @SchemaFormat("date")
    @SchemaDescription("Start date of leave")
    private String startDate;

    @SchemaRequired
    @SchemaFormat("date")
    @SchemaDescription("End date of leave")
    private String endDate;

    @SchemaRequired
    @SchemaDescription("Reason for leave")
    private String reason;

    @SchemaDescription("Medical certificate if required")
    private String medicalCertificate;

    @SchemaDescription("Person who will approve the leave")
    private String approver;
}
```

* Json Schema

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "leave_type": {
      "type": "string",
      "enum": ["annual", "sick", "unpaid"],
      "description": "Type of leave request"
    },
    "start_date": {
      "type": "string",
      "format": "date",
      "description": "Start date of leave"
    },
    "end_date": {
      "type": "string",
      "format": "date",
      "description": "End date of leave"
    },
    "reason": {
      "type": "string",
      "description": "Reason for leave"
    },
    "medical_certificate": {
      "type": "string",
      "description": "Medical certificate if required"
    },
    "approver": {
      "type": "string",
      "description": "Person who will approve the leave"
    }
  },
  "required": ["leave_type", "start_date", "end_date", "reason"]
}
```

**输出 (JSON)**:
```json
{
  "fields": {
    "leave_type": {
      "value": "annual",
      "confidence": 0.8,
      "reasoning": "The user mentions taking time off for a personal event (marriage), which typically falls under annual leave. No explicit mention of sick or unpaid leave.",
      "alternatives": ["sick", "unpaid"]
    },
    "start_date": {
      "value": "2025-12-15",
      "confidence": 1.0,
      "reasoning": "The user explicitly states 'starting from December 15th'.",
      "alternatives": []
    },
    "end_date": {
      "value": "2025-12-22",
      "confidence": 0.9,
      "reasoning": "The user says 'a week off' starting December 15th, which logically ends on December 22nd. This is a reasonable interpretation given the context.",
      "alternatives": []
    },
    "reason": {
      "value": "Wedding",
      "confidence": 0.95,
      "reasoning": "The user mentions being 'getting married next month', so the reason for leave is clearly related to a wedding.",
      "alternatives": ["Personal event", "Family occasion"]
    },
    "medical_certificate": {
      "value": null,
      "confidence": 1.0,
      "reasoning": "No mention of any medical condition or requirement for a medical certificate in the input.",
      "alternatives": []
    },
    "approver": {
      "value": "Alice",
      "confidence": 0.9,
      "reasoning": "The system context indicates that the user's manager is 'Alice', and it is standard practice for leave requests to be approved by the manager. There is no indication otherwise.",
      "alternatives": []
    }
  },
  "errors": []
}
```

---

## 架构设计

### 执行流程

1. **Prompt Generation (提示生成)**: 根据 schema 和 context 动态构建系统提示。
2. **LLM Interaction (LLM 交互)**: 通过 Spring AI 抽象调用 LLM。
3. **Parsing & Validation (解析与验证)**: 将输出映射为 JSON，针对 Schema 进行验证。
4. **Retry Loop (重试循环)**: 如果验证失败，将错误反馈给 LLM 进行修正。

### 项目结构

```
lazy-form-instructor/
├── instructor/                      # 核心库
│   ├── src/main/java/com/fanyamin/
│   │   ├── LazyFormInstructor.java  # 主引擎类
│   │   └── instructor/
│   │       ├── api/                 # 核心数据模型
│   │       ├── schema/              # JSON Schema 生成与验证
│   │       ├── llm/                 # LLM 客户端与提示管理
│   │       └── streaming/           # 流式解析事件
│   └── pom.xml
├── example/
│   ├── cli-demo/                    # 命令行示例
│   │   ├── src/main/java/com/fanyamin/example/
│   │   └── pom.xml
│   └── web-demo/                    # Web 应用示例 (Spring Boot + Vue.js)
│       ├── src/main/java/com/fanyamin/web/
│       ├── frontend/
│       ├── pom.xml
│       └── README.md
├── .github/workflows/               # CI/CD workflows
├── README.md                        # 主文档
├── build.sh                         # 构建脚本
└── run-demo.sh                      # 运行命令行演示
```

---

## 安装与使用

### 方式一：Web应用 (最简单) 🌐

**推荐给非开发人员** - 带图形界面的完整Web应用

1. 配置LLM (创建`.env`文件):

```bash
LLM_API_KEY=your-llm-api-key
LLM_BASE_URL=https://api.openai.com/v1/chat/completions
LLM_MODEL=gpt-4-turbo-preview
```

2. 启动应用:
```bash
cd example/web-demo
./start.sh
```

3. 打开浏览器: `http://localhost:5173`

详细信息请查看 [example/web-demo/README.md](example/web-demo/README.md)

### 方式二：命令行示例 💻

**适合开发者和API集成测试**

#### 表单解析示例

1. 构建核心库:
```bash
./build.sh
```

2. 运行示例:
```bash
./run-demo.sh
```

### 方式三：作为库集成 📚

**集成到你自己的项目**

在 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>com.fanyamin</groupId>
    <artifactId>lazy-form-instructor</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 快速开始

#### 1. 定义 Schema (JSON Schema)

```json
{
  "type": "object",
  "properties": {
    "name": { "type": "string" },
    "age": { "type": "integer" }
  },
  "required": ["name"]
}
```

#### 2. 实例化 Instructor

```java
// 使用 Spring AI ChatClient
ChatClient chatClient = ...;
LlmClient llmClient = new SpringAiLlmClient(chatClient);
LazyFormInstructor instructor = new LazyFormInstructor(llmClient);
```

#### 3. 解析自然语言

```java
String schema = "..."; // 加载 JSON schema
String input = "John is 30 years old";
Map<String, Object> context = Map.of("now", "2023-10-27");

ParsingRequest request = new ParsingRequest(schema, input, context);
ParsingResult result = instructor.parse(request);

if (result.errors().isEmpty()) {
    System.out.println("Name: " + result.fields().get("name").value());
    System.out.println("Confidence: " + result.fields().get("name").confidence());
    System.out.println("Reasoning: " + result.fields().get("name").reasoning());
}
```

---

## 高级功能

### 复杂表单支持

- **嵌套对象**: 处理子表单（例如 `attendees[]` 包含 `name`, `role`）。
- **可重复数组**: 管理列表项（例如 `expense_items[]`）。
- **条件依赖**: 建模逻辑规则（例如 "如果请假类型是病假，则需要医疗证明"）。
- **跨字段约束**: 例如 `start_date <= end_date`, `sum(items.amount) == total`。

### 多步骤/向导式表单

- **部分填充**: 可以填充当前步骤的字段，并从相同输入推断未来步骤的值。
- **未来步骤提示**: 标记已经确定的后续步骤字段，以便 UI 预填充或跳过这些步骤。

### 歧义与冲突处理

- **歧义消解**: 当用户意图不明确时返回备选方案（例如 "下个星期五" 可能是本周五或下周五）。
- **结构化歧义**: 对于不清楚的数组元素引用（例如多个行程段中的"北京之行"），系统提供多个候选结构。
- **跨字段冲突**: 例如 `start_date` 晚于 `end_date`，将作为显式错误或低置信度建议呈现。

### 上下文与业务规则集成

- **动态默认值**: 从用户资料中获取默认值（例如 `approver` 默认为 `managerId`）。
- **策略规则层**: 严格的业务约束（例如最大请假天数、审批链）应在确定性规则层中实施，而不仅仅依赖 LLM。

---

## 工程实践建议

### Prompt 设计与模板化

系统提供 Prompt 模板，按 schema 自动生成提示（包含字段名、类型、示例、校验规则）并注入用户输入。

**示例系统提示**:
```
You are a smart form filling assistant. Your goal is to extract structured data from user input based on a provided JSON Schema.

### INSTRUCTIONS
1. Analyze the Input: Read the User Input and Context.
2. Follow the Schema: The output MUST adhere to the provided JSON Schema.
3. Extraction Rules:
   - Value: Extract the value for each field.
   - Confidence: Assign a confidence score (0.0 to 1.0).
   - Reasoning: Explain why you extracted this value.
   - Alternatives: List alternative values if ambiguous.

### CONTEXT
{context}

### FORM SCHEMA
{schema}

### USER INPUT
{user_input}

Answer strictly in JSON.
```

### 多轮与确认策略

若解析后存在 `missing_fields`：
1. 主动向用户发起明确的补充问题（多轮对话）。
2. 使用 LLM 推断默认值并自动补全，标记为"系统补齐"。

### 验证与安全

- JSON Schema 作基础校验（类型、格式、枚举）。
- 业务校验（配额、权限、日期冲突）通过插件式 Validator 实现。
- 对 LLM 返回做严格 schema 校验后才能用于提交（防止注入、格式异常）。

### 可观察性

- 记录每次 LLM 请求/响应（prompt、response、解析结果），用于模型调优与错误分析。
- 统计常见缺失字段以改进 schema 或提示。

---

## 示例：请假申请表单

### Schema 定义

```json
{
  "type": "object",
  "properties": {
    "leave_type": {
      "type": "string",
      "enum": ["annual", "sick", "unpaid"]
    },
    "start_date": { "type": "string", "format": "date" },
    "end_date": { "type": "string", "format": "date" },
    "reason": { "type": "string" },
    "medical_certificate": { "type": "string" }
  },
  "required": ["leave_type", "start_date", "end_date"]
}
```

### 用户输入

> "I'm feeling really sick, I need a week off starting next Monday. Please send it to my manager."

### 上下文

```json
{
  "now": "2023-10-27T10:00:00Z",
  "user": {
    "role": "employee",
    "managerId": "u_123"
  },
  "locale": "en-US"
}
```

### 预期输出

```json
{
  "fields": {
    "leave_type": {
      "value": "sick",
      "confidence": 0.99,
      "reasoning": "Explicitly mentioned 'sick'",
      "alternatives": []
    },
    "start_date": {
      "value": "2023-10-30",
      "confidence": 0.95,
      "reasoning": "Calculated 'next Monday' from context.now",
      "alternatives": []
    },
    "end_date": {
      "value": "2023-11-05",
      "confidence": 0.90,
      "reasoning": "Inferred 'a week' as 7 days from start_date",
      "alternatives": ["2023-11-03"]
    }
  },
  "errors": [
    {
      "path": "medical_certificate",
      "message": "Required for sick leave > 3 days",
      "type": "business_rule"
    }
  ]
}
```

---

## 示例：任务创建表单

### Schema 定义 (简化版)

```json
{
  "type": "object",
  "properties": {
    "name": { "type": "string" },
    "priority": { "type": "integer", "minimum": 1, "maximum": 5 },
    "schedule_time": { "type": "string", "format": "date-time" },
    "deadline": { "type": "string", "format": "date-time" },
    "minutes": { "type": "integer" }
  },
  "required": ["name", "schedule_time", "deadline"]
}
```

### 用户输入

> "tomorrow is monday, I have to finish the sending alert feature ASAP, the release date is next friday"

### 上下文

```json
{
  "now": "2023-10-27T10:00:00Z",
  "locale": "en-US"
}
```

### 预期输出

```json
{
  "fields": {
    "name": {
      "value": "sending alert feature",
      "confidence": 0.98,
      "reasoning": "Extracted from 'finish the sending alert feature'",
      "alternatives": []
    },
    "priority": {
      "value": 5,
      "confidence": 0.85,
      "reasoning": "Mapped from 'ASAP' to highest priority",
      "alternatives": [4]
    },
    "schedule_time": {
      "value": "2023-10-30T09:00:00Z",
      "confidence": 0.95,
      "reasoning": "Calculated 'tomorrow is monday' from context.now",
      "alternatives": []
    },
    "deadline": {
      "value": "2023-11-03T23:59:59Z",
      "confidence": 0.90,
      "reasoning": "Calculated 'next friday' (release date) from schedule_time",
      "alternatives": []
    },
    "minutes": {
      "value": null,
      "confidence": 0.0,
      "reasoning": "Not mentioned, recommend asking user for estimate",
      "alternatives": [120, 180]
    }
  },
  "errors": []
}
```

---

## Roadmap (开发路线)

- [x] 核心 API 与 JSON 协议
- [x] JSON Schema 验证
- [x] LLM 集成 (Spring AI)
- [x] 基础重试逻辑
- [x] 测试与文档
- [ ] 高级复杂 Schema 扩展（分组、跨字段约束）
- [ ] 多步骤表单状态管理
- [ ] 前端通用组件 (Vue/React)

---

## 技术栈

- **Java 17+**
- **Spring AI 1.0.0-M1** (LLM 抽象层)
- **Jackson** (JSON 处理)
- **NetworkNT JSON Schema Validator** (Schema 验证)
- **JUnit 5** (测试)

---

## 扩展功能 (未来计划)

- **表单模板学习**: 分析历史提交，生成更好的默认值或提示（模型微调 / RLHF）。
- **多语言支持**: 将 prompt 与 schema 描述国际化。
- **主动补问策略**: 自动生成多轮问题以高效收集缺失字段（优化 UX）。
- **审计与回滚**: 记录谁提交、何时及 LLM 版本，支持回滚改动。

---

## 实用策略与工程经验

1. **从 Schema 出发**: Schema 定义越完整，LLM 返回越受控。
2. **让 LLM 返回纯 JSON**: 避免自然语言噪声，使用 function-calling 或明确指令。
3. **严格校验所有 LLM 输出**: 始终把 JSON Schema 校验作为最后一道防线。
4. **记录 Prompt-Response**: 用于模型调优与错误分析。
5. **分级策略**: 对简单字段自动补全、对敏感/复杂字段走人工确认。
6. **可插拔 Validator & Adapter**: 支持业务校验、权限检查、不同后端提交方式。
7. **监控指标**: 解析成功率、平均轮次、常见缺失字段、用户改动率。

---

## License

Apache-2.0 license

---

## 贡献与支持

欢迎贡献代码、提出 issue 或改进建议！

如果您在使用中遇到问题，请查看 [文档](./instructor/README.md) 或提交 issue。
