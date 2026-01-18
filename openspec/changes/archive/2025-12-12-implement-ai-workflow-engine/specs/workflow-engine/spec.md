## ADDED Requirements

### Requirement: DAG Workflow Structure
The system SHALL organize workflows as a Directed Acyclic Graph (DAG) comprised of distinct Node types.

#### Scenario: Workflow Definition
- **GIVEN** a workflow definition
- **THEN** it MUST consist of `StartNode`, `EndNode`, `DecisionNode`, and `ProcessNode` types
- **AND** Nodes MUST be connected by directed Edges

### Requirement: AI Decision Nodes
The system SHALL provide `AiDecisionNode` implementations that use an LLM to evaluate the `WorkflowContext` and determine the next path.

#### Scenario: Semantic Branching
- **GIVEN** an `AiDecisionNode` with a policy "Approve if under $500 or for office supplies"
- **AND** a context with "Item: Golden Pen, Cost: $1000"
- **WHEN** the node executes
- **THEN** the LLM evaluates the policy
- **AND** the node returns a result that routes to the "Reject" or "Manager Approval" path

### Requirement: AI Process Nodes
The system SHALL provide `AiProcessNode` implementations where the "work" is performed by an LLM (e.g., generation, extraction).

#### Scenario: Summarization Task
- **GIVEN** an `AiProcessNode` configured to "Summarize Request History"
- **WHEN** the node executes
- **THEN** it calls the LLM with the context history
- **AND** writes the summary back to the `WorkflowContext`

### Requirement: Context Propagation
The system SHALL pass a `WorkflowContext` object between nodes to maintain state.

#### Scenario: Data Flow
- **WHEN** Node A writes `variable_x` to the context
- **THEN** Node B (downstream) SHALL be able to read `variable_x`
