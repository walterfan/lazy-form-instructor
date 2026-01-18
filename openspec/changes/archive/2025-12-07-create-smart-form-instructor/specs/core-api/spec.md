## ADDED Requirements
### Requirement: Validation Retry Loop
The system SHALL implement an automatic retry mechanism when the LLM output fails schema validation or business rules.

#### Scenario: Retry on Type Mismatch
- **WHEN** the LLM returns a string for an integer field
- **THEN** the system MUST send a follow-up prompt to the LLM with the validation error and request a correction, up to a configured max retry count.
