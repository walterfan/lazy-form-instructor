## 1. Core Workflow Engine
- [x] 1.1 Define `WorkflowContext` to hold data and execution trace.
- [x] 1.2 Define abstract `WorkflowNode` and `WorkflowEdge`.
- [x] 1.3 Implement `WorkflowGraph` (DAG) to manage nodes and traversals.
- [x] 1.4 Implement `WorkflowEngine` runner.

## 2. Node Implementations
- [x] 2.1 Implement `StartNode` and `EndNode`.
- [ ] 2.2 Implement `ProcessNode` interface.
    - [x] 2.2.1 `ActionNode`: Wraps Java Functional Interfaces.
    - [ ] 2.2.2 `AiProcessNode`: Uses LLM to generate content/transform data.
- [ ] 2.3 Implement `DecisionNode` interface.
    - [x] 2.3.1 `LogicDecisionNode`: Deterministic checks.
    - [x] 2.3.2 `AiDecisionNode`: Uses LLM to make semantic decisions (Approve/Reject).

## 3. Tool Registry & Function Calling
- [ ] 3.1 Define `Tool` and `ToolRegistry` abstractions.
- [ ] 3.2 Implement annotation-based tool registration (`@WorkflowTool`, `@ToolParam`).
- [ ] 3.3 Implement OpenAI-compatible tool schema generation.
- [ ] 3.4 Integrate tool execution into `AiDecisionNode` and `AiProcessNode`.

## 4. Library Integration
- [ ] 4.1 Create `WorkflowBuilder` fluent API.
- [x] 4.2 Implement `TraceLog` to record AI reasoning steps.
- [x] 4.3 Add cycle detection and graph validation.

## 5. CLI Example Module (smart-form-workflow-example)
- [x] 5.1 Create Maven module structure.
- [x] 5.2 Implement `LeaveRequestWorkflowExample.java`:
    - [x] 5.2.1 Define Leave Request workflow DAG.
    - [x] 5.2.2 Configure AI validation and approval nodes.
    - [x] 5.2.3 Run workflow with sample inputs and display trace.

## 6. Web Demo Module (smart-form-workflow-web)
- [ ] 6.1 Create Maven module structure with Spring Boot.
- [ ] 6.2 Implement Backend APIs:
    - [ ] 6.2.1 `POST /api/workflows/submit` - Submit workflow request.
    - [ ] 6.2.2 `GET /api/workflows/{id}/status` - Get workflow status and trace.
    - [ ] 6.2.3 `GET /api/workflows/{id}/graph` - Get DAG structure for visualization.
- [ ] 6.3 Implement Frontend (Vue.js + TypeScript):
    - [ ] 6.3.1 Request input form.
    - [ ] 6.3.2 Workflow visualization (DAG with active node highlighting).
    - [ ] 6.3.3 Decision reasoning display (confidence, reasoning, alternatives).
    - [ ] 6.3.4 Trace log viewer.

## 7. Testing
- [x] 7.1 Write unit tests for graph branching logic.
- [ ] 7.2 Validate `AiDecisionNode` behavior with mock LLM responses.
- [ ] 7.3 Test tool execution and callback flow.
- [x] 7.4 Verify acceptance test cases from design document.

