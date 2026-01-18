## ADDED Requirements

### Requirement: Streaming Output
The system SHALL provide streaming output for long-running extractions.

#### Scenario: Stream raw chunks during parsing
- **GIVEN** a parsing request
- **WHEN** `SmartFormInstructor.parseStreaming(request)` is called
- **THEN** the system MUST emit raw LLM output chunks as they arrive
- **AND** the stream MUST include attempt information

#### Scenario: Emit best-effort snapshots
- **GIVEN** an active streaming parse
- **WHEN** the buffered JSON becomes parseable as `ParsingResult`
- **THEN** the system SHOULD emit a snapshot event containing the current partial `ParsingResult`
- **AND** snapshot events MUST be best-effort and MAY be skipped when parsing is not possible

#### Scenario: Final-only schema validation
- **GIVEN** a streaming parse in progress
- **WHEN** the LLM completes and the final JSON is assembled
- **THEN** the system MUST run JSON Schema validation once on the final output
- **AND** the final result event MUST include the schema validation errors (empty list if OK)

#### Scenario: Retry on schema validation failure
- **GIVEN** a streaming parse attempt completes with schema validation errors
- **WHEN** retries are enabled
- **THEN** the system MUST emit an attempt-failed event with schema errors
- **AND** start a new attempt using an error-correction prompt
- **AND** emit a final result when validation succeeds or retries are exhausted


