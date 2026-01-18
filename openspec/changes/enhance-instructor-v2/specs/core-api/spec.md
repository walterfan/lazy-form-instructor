## MODIFIED Requirements

### Requirement: Validation Retry Loop
The system SHALL implement an automatic retry mechanism when the LLM output fails schema validation or business rules, using a configurable retry strategy with exponential backoff and jitter.

#### Scenario: Retry on Type Mismatch
- **GIVEN** the LLM returns a string for an integer field
- **WHEN** schema validation fails
- **THEN** the system MUST send a follow-up prompt to the LLM with the validation error
- **AND** retry according to the configured `RetryConfig` (exponential backoff with jitter)
- **AND** stop after reaching `maxAttempts`

#### Scenario: Retry Exhaustion
- **GIVEN** a retry configuration with `maxAttempts=3`
- **WHEN** all retry attempts fail validation
- **THEN** the system MUST throw `RetryExhaustedException` containing all validation errors and attempt count

#### Scenario: Rate Limit Handling
- **GIVEN** the LLM provider returns a rate limit error
- **WHEN** the error is in the `retryOn` exception list
- **THEN** the system MUST retry with exponential backoff
- **AND** include jitter to prevent thundering herd

## ADDED Requirements

### Requirement: Builder API
The system SHALL provide a fluent builder API for constructing `SmartFormInstructor` instances with optional configuration.

#### Scenario: Basic Builder Usage
- **WHEN** a developer creates an instructor using the builder
- **THEN** they can specify only the required `llmClient`
- **AND** all optional configurations default to sensible values
- **AND** the resulting instructor behaves identically to V1 constructor

#### Scenario: Advanced Configuration
- **GIVEN** a developer needs custom retry, caching, and hooks
- **WHEN** they use the builder pattern
- **THEN** they can chain configuration methods fluently
- **AND** register multiple hooks
- **AND** all configurations are applied to the resulting instructor

```java
SmartFormInstructor instructor = SmartFormInstructor.builder()
    .llmClient(llmClient)
    .retryConfig(RetryConfig.production())
    .withCache(new InMemoryCache())
    .addHook(new LoggingHook())
    .addHook(new MetricsHook())
    .build();
```

### Requirement: Typed Exception Hierarchy
The system SHALL throw specific exception types for different failure modes, enabling applications to handle errors appropriately.

#### Scenario: Validation Failure Exception
- **WHEN** LLM output fails validation after retries
- **THEN** the system MUST throw `ValidationException`
- **AND** include a list of all validation errors with field paths and messages

#### Scenario: Incomplete Output Exception
- **WHEN** the LLM output is truncated due to token limits
- **THEN** the system MUST throw `IncompleteOutputException`
- **AND** include the partial JSON for debugging

#### Scenario: Rate Limit Exception
- **WHEN** the LLM provider rate limit is exceeded
- **THEN** the system MUST throw `RateLimitException`
- **AND** include retry-after information if available

#### Scenario: Mode Configuration Exception
- **WHEN** an invalid response mode is configured for the LLM provider
- **THEN** the system MUST throw `ModeException` at build time
- **AND** include clear error message about compatibility

### Requirement: Backward Compatibility
The system SHALL maintain compatibility with V1 API while providing V2 enhancements.

#### Scenario: V1 Constructor Still Works
- **WHEN** a developer uses the old V1 constructor
- **THEN** the instructor functions correctly with default V2 settings
- **AND** no breaking changes occur in behavior

#### Scenario: Deprecation Warnings
- **WHEN** V1 constructors are used
- **THEN** the compiler emits deprecation warnings
- **AND** documentation guides migration to builder API

