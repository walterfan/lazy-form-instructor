# Lazy Form Workflow Engine

AI-driven workflow engine for automating complex business processes with LLM-powered decision making.

## Overview

`lazy-form-workflow` is a Java library that combines deterministic workflow graphs (DAG) with AI-powered nodes for semantic decision-making and content generation. It extends the capabilities of `lazy-form-instructor` from form parsing to complete process automation.

## Key Features

- **Directed Acyclic Graph (DAG)**: Define workflows as nodes and edges with branching logic
- **AI-Powered Decisions**: Use LLMs to make semantic decisions (approval/rejection, routing)
- **Deterministic Nodes**: Traditional code-based nodes for reliable operations
- **Cycle Detection**: Automatic validation prevents infinite loops
- **Execution Tracing**: Complete audit trail of all node executions
- **Flexible Branching**: Conditional edges based on context and node results

## Architecture

### Core Components

- **WorkflowContext**: Shared blackboard pattern for data flow between nodes
- **WorkflowNode**: Base interface for all node types
- **WorkflowEdge**: Conditional connections between nodes
- **WorkflowGraph**: DAG structure with cycle detection
- **WorkflowEngine**: Execution engine with tracing and error handling

### Node Types

| Node Type | Purpose | Example Use Case |
|-----------|---------|------------------|
| `StartNode` | Entry point | Initialize context |
| `EndNode` | Terminal point | Finalize workflow |
| `ActionNode` | Side-effectful operations | Database writes, API calls |
| `LogicDecisionNode` | Deterministic routing | Check amount > threshold |
| `AiDecisionNode` | Semantic decisions | Validate request reason, approve/reject |
| `AiProcessNode` | Content generation | Draft emails, summarize history |

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>com.fanyamin</groupId>
    <artifactId>lazy-form-workflow</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. Build a Simple Workflow

```java
// Create graph
WorkflowGraph graph = new WorkflowGraph();

// Define nodes
StartNode start = new StartNode("start");
LogicDecisionNode checkAmount = new LogicDecisionNode("check_amount",
    ctx -> {
        FieldResult amount = ctx.getRequest().fields().get("amount");
        return (int) amount.value() > 1000;
    });
ActionNode highValuePath = new ActionNode("high_value", 
    ctx -> {
        System.out.println("High value request - requires approval");
        return "PENDING_APPROVAL";
    });
ActionNode lowValuePath = new ActionNode("low_value",
    ctx -> {
        System.out.println("Low value request - auto-approved");
        return "APPROVED";
    });
EndNode end = new EndNode("end");

// Add nodes
graph.addNode(start);
graph.addNode(checkAmount);
graph.addNode(highValuePath);
graph.addNode(lowValuePath);
graph.addNode(end);

// Define edges
graph.addEdge(new WorkflowEdge("start", "check_amount"));
graph.addEdge(new WorkflowEdge("check_amount", "high_value",
    (ctx, result) -> result.payload().equals(true), "HIGH_VALUE", 1));
graph.addEdge(new WorkflowEdge("check_amount", "low_value",
    (ctx, result) -> result.payload().equals(false), "LOW_VALUE", 0));
graph.addEdge(new WorkflowEdge("high_value", "end"));
graph.addEdge(new WorkflowEdge("low_value", "end"));

// Execute
WorkflowEngine engine = new WorkflowEngine(graph);
WorkflowContext context = new WorkflowContext(parsingResult);
WorkflowResult result = engine.execute("start", context);

// Check result
System.out.println("Status: " + result.status());
System.out.println("Trace: " + result.context().getTraceLog());
```

### 3. Use AI Decision Nodes

```java
LlmClient llmClient = LlmClientFactory.createFromEnvironment();

String validationSchema = """
    {
      "type": "object",
      "properties": {
        "decision": {
          "type": "string",
          "enum": ["APPROVE", "REJECT", "ESCALATE"]
        }
      },
      "required": ["decision"]
    }
    """;

AiDecisionNode aiValidator = AiDecisionNode.builder()
    .id("ai_validation")
    .llmClient(llmClient)
    .systemPrompt("You are a leave request validator. Evaluate if the reason is valid.")
    .outputSchema(validationSchema)
    .confidenceThreshold(0.8)
    .build();

graph.addNode(aiValidator);
graph.addEdge(new WorkflowEdge("start", "ai_validation"));
graph.addEdge(new WorkflowEdge("ai_validation", "auto_approve",
    (ctx, result) -> {
        if (result.payload() instanceof AiDecisionNode.DecisionPayload decision) {
            return "APPROVE".equals(decision.decision) && decision.confidence >= 0.8;
        }
        return false;
    }));
```

## Examples

See `lazy-form-workflow-example` module for complete examples:
- Leave request workflow with AI validation and approval
- Multi-step approval with confidence-based routing

## Testing

The library includes comprehensive tests:

```bash
cd lazy-form-workflow
mvn test
```

### Acceptance Criteria Verified

✅ **Deterministic Flow**: Graph traversal with logic decisions  
✅ **AI Decision Routing**: High confidence auto-approval  
✅ **AI Decision Escalation**: Low confidence human review  
✅ **Cycle Detection**: Prevents infinite loops  
✅ **Execution Tracing**: Complete audit trail

## Design Principles

- **Blackboard Pattern**: `WorkflowContext` provides global state access (inspired by N8N)
- **AI as Router**: LLMs make semantic decisions, not just generate text (inspired by Dify)
- **Deterministic Core**: Reliable graph structure with AI augmentation
- **Explainability**: Every decision is traceable with reasoning

## API Reference

### WorkflowContext

```java
WorkflowContext ctx = new WorkflowContext(parsingResult);
ctx.setState("PENDING_VALIDATION");
ctx.setVar("user_id", "emp_123");
Object value = ctx.getVar("key");
List<TraceEntry> trace = ctx.getTraceLog();
```

### WorkflowEngine

```java
WorkflowEngine engine = new WorkflowEngine(graph);
WorkflowEngine engineWithLimit = new WorkflowEngine(graph, 50); // max iterations
WorkflowResult result = engine.execute("start_node_id", context);
```

### Node Results

```java
NodeResult.success(payload);
NodeResult.success(payload, "EDGE_HINT");
NodeResult.failure("Error message");
NodeResult.waiting(); // Pause for external input
```

## Integration with LazyFormInstructor

This library extends `lazy-form-instructor`:

1. Parse form with `LazyFormInstructor`
2. Create `WorkflowContext` with `ParsingResult`
3. Execute workflow with parsed data
4. Use AI nodes for semantic decisions

## Future Enhancements

- [ ] Tool/Function calling support for AI nodes
- [ ] WorkflowBuilder fluent API
- [ ] HumanTaskNode for manual approvals
- [ ] AiProcessNode for content generation
- [ ] Workflow persistence and resumption
- [ ] Web UI for workflow visualization

## License

See parent project for license information.



