# Change: Enhance SmartFormInstructor to Production-Grade V2

## Why

The current `smart-form-instructor` (V1) is a functional prototype for structured data extraction from LLMs. However, it lacks critical features needed for production deployment:

1. **Limited Observability**: No built-in instrumentation for logging, metrics, or distributed tracing.
2. **Basic Retry Logic**: Hardcoded retry loop without exponential backoff, jitter, or circuit breaking.
3. **Schema-Only Validation**: Cannot express complex business rules (e.g., cross-field validation, date range checks).
4. **No Caching**: Redundant LLM calls for identical requests waste cost and latency.
5. **Poor DX**: Generic exceptions make error handling difficult; no fluent builder API.
6. **Missing Extensibility**: V1 lacks opt-in extension points for production usage patterns.

This enhancement brings `smart-form-instructor` to parity with Python's `instructor` library, making it production-ready for enterprise use cases with proper observability, reliability, and developer experience.

## What Changes

### Phase 1: Foundation (Observability & Reliability)
- **Add Hook System**: Non-invasive instrumentation points (`beforeRequest`, `afterResponse`, `onError`, `onRetry`) for logging, metrics (Micrometer), and tracing (OpenTelemetry).
- **Enhanced Exception Hierarchy**: Typed exceptions (`ValidationException`, `RetryExhaustedException`, `RateLimitException`, `IncompleteOutputException`, `ModeException`) with rich metadata.
- **Advanced Retry Strategy**: Configurable retry with exponential backoff, jitter, and exception-specific policies.

### Phase 2: Developer Experience & Flexibility
- **Fluent Builder API**: `SmartFormInstructor.builder()` pattern for clean configuration.
- **Custom Validation Framework**: Java-based validators with annotations (`@SchemaValidate`) for business logic beyond JSON Schema.
- **Response Modes**: Support for different LLM interaction patterns (`JSON`, `TOOLS`, `TOOLS_STRICT`, `MD_JSON`).

### Phase 3: Performance & Advanced Features
- **Caching Layer**: Pluggable cache implementations (`NoOpCache`, `InMemoryCache`, `FileCache`) to avoid redundant LLM calls.
- (Moved) Streaming support is proposed in a separate change: `add-instructor-streaming`.

### Breaking Changes
- **BREAKING**: `SmartFormInstructor` constructor signatures change (old constructors deprecated but supported).
- **BREAKING**: `ParsingRequest` and `ParsingResult` may gain new fields (backward compatible via defaults).

## Impact

### New Capabilities
- `observability`: Hook system for instrumentation
- `reliability`: Advanced retry and circuit breaking
- `validation`: Custom validation beyond schema
- `performance`: Caching and streaming

### Modified Capabilities
- `core-api`: Enhanced with builder pattern, new exception types, and validation extensibility

### Affected Code
- **Core Package**: `com.fanyamin.SmartFormInstructor` - Add builder, hook manager, retry manager, cache manager
- **New Packages**:
  - `com.fanyamin.instructor.hooks` - Hook interfaces and implementations
  - `com.fanyamin.instructor.retry` - Retry strategy and configuration
  - `com.fanyamin.instructor.cache` - Cache interface and implementations
  - `com.fanyamin.instructor.validation` - Custom validator framework
  - `com.fanyamin.instructor.mode` - Response mode handlers
- (Moved) `com.fanyamin.instructor.streaming` - Streaming support is tracked in `add-instructor-streaming`
- **Exception Package**: Extend with new exception types
- **Test Coverage**: Comprehensive acceptance tests for all new features

### Migration Path
V1 API remains functional with deprecation warnings. Recommended migration to V2 builder API via documentation and examples.

