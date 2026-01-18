# Implementation Tasks: SmartFormInstructor V2 Enhancement

## Phase 1: Foundation (Observability & Reliability)

### 1. Exception Hierarchy
- [ ] 1.1 Create `ValidationException` with `List<ValidationError>` field
- [ ] 1.2 Create `RetryExhaustedException` with `lastErrors` and `attempts` fields
- [ ] 1.3 Create `IncompleteOutputException` with `partialJson` field
- [ ] 1.4 Create `ModeException` for invalid mode/provider combinations
- [ ] 1.5 Create `RateLimitException` for LLM rate limit errors
- [ ] 1.6 Update existing code to throw specific exceptions instead of generic `InstructorException`

### 2. Hook System
- [ ] 2.1 Create `HookContext` record with correlationId, startTime, prompt, model, response fields
- [ ] 2.2 Create `InstructorHook` interface with lifecycle methods
- [ ] 2.3 Create `HookManager` to manage hook registration and execution
- [ ] 2.4 Implement `LoggingHook` as reference implementation
- [ ] 2.5 Integrate hook calls in `SmartFormInstructor.parse()` method
- [ ] 2.6 Add correlation ID generation (UUID per request)
- [ ] 2.7 Write unit tests for hook execution order and error handling

### 3. Advanced Retry Strategy
- [ ] 3.1 Create `BackoffStrategy` enum (FIXED, EXPONENTIAL)
- [ ] 3.2 Create `RetryConfig` class with configuration fields
- [ ] 3.3 Create `RetryManager` to execute retry logic with backoff
- [ ] 3.4 Add jitter calculation to prevent thundering herd
- [ ] 3.5 Support exception-specific retry policies (`retryOn` list)
- [ ] 3.6 Replace hardcoded loop in `SmartFormInstructor.parse()` with `RetryManager`
- [ ] 3.7 Add `Clock` injectable for testability
- [ ] 3.8 Write unit tests for exponential backoff timing
- [ ] 3.9 Write integration test for retry exhaustion

## Phase 2: Developer Experience & Flexibility

### 4. Fluent Builder API
- [ ] 4.1 Create `SmartFormInstructor.Builder` inner class
- [ ] 4.2 Add static `builder()` factory method
- [ ] 4.3 Implement builder methods: `llmClient()`, `retryConfig()`, `addHook()`, `withCache()`
- [ ] 4.4 Update constructors to delegate to builder internally
- [ ] 4.5 Add `@Deprecated` annotations to old constructors
- [ ] 4.6 Write migration guide with before/after examples
- [ ] 4.7 Update README with V2 builder examples

### 5. Custom Validation Framework
- [ ] 5.1 Create `ValidationContext` interface with access to full object
- [ ] 5.2 Create `FieldValidator<T>` interface
- [ ] 5.3 Create `@SchemaValidate` annotation
- [ ] 5.4 Create `ValidationEngine` to scan annotations and execute validators
- [ ] 5.5 Integrate custom validation after schema validation
- [ ] 5.6 Implement `DateRangeValidator` as example validator
- [ ] 5.7 Write unit tests for cross-field validation
- [ ] 5.8 Document custom validator creation guide

### 6. Response Modes
- [ ] 6.1 Create `ResponseMode` enum (JSON, TOOLS, TOOLS_STRICT, MD_JSON)
- [ ] 6.2 Create `ModeHandler` interface
- [ ] 6.3 Implement `JsonModeHandler` (existing behavior)
- [ ] 6.4 Implement `ToolsModeHandler` for OpenAI Function Calling
- [ ] 6.5 Implement `ToolsStrictModeHandler` for Structured Outputs
- [ ] 6.6 Implement `MdJsonModeHandler` for markdown-wrapped JSON
- [ ] 6.7 Add mode selection in builder API
- [ ] 6.8 Validate mode compatibility with LLM provider
- [ ] 6.9 Write unit tests for each mode handler

## Phase 3: Performance & Advanced Features

### 7. Caching Layer
- [ ] 7.1 Create `ResponseCache` interface
- [ ] 7.2 Implement `NoOpCache` (default, no-op implementation)
- [ ] 7.3 Implement `InMemoryCache` with ConcurrentHashMap and LRU eviction
- [ ] 7.4 Implement `FileCache` with disk persistence
- [ ] 7.5 Create cache key generator (hash of prompt + model + schema + temperature)
- [ ] 7.6 Add TTL support in cache implementations
- [ ] 7.7 Integrate cache check before LLM call
- [ ] 7.8 Add cache metrics (hit rate, size) via hooks
- [ ] 7.9 Write unit tests for cache key generation
- [ ] 7.10 Write integration test for cache hit/miss scenarios
- [ ] 7.11 Document cache configuration and key generation

## Cross-Cutting Tasks

### 9. Testing & Quality
- [ ] 9.1 Write acceptance test for observability (hook execution)
- [ ] 9.2 Write acceptance test for retry strategy
- [ ] 9.3 Write acceptance test for custom validation
- [ ] 9.4 Write acceptance test for caching
- [ ] 9.5 (Moved) Streaming acceptance tests live in `add-instructor-streaming`
- [ ] 9.6 Run performance benchmarks (with/without hooks/cache)
- [ ] 9.7 Update test coverage to >80%

### 10. Documentation & Migration
- [ ] 10.1 Update README with V2 features overview
- [ ] 10.2 Create V1-to-V2 migration guide
- [ ] 10.3 Create examples for each major feature (hooks, caching)
- [ ] 10.4 Document performance characteristics and overhead
- [ ] 10.5 Create troubleshooting guide for common issues
- [ ] 10.6 Add JavaDoc for all new public APIs
- [ ] 10.7 Create architecture diagram showing V2 components

### 11. Optional: Observability Modules
- [ ] 11.1 Create `smart-form-instructor-micrometer` module
- [ ] 11.2 Implement `MicrometerHook` for metrics
- [ ] 11.3 Create `smart-form-instructor-otel` module (optional)
- [ ] 11.4 Implement `OpenTelemetryHook` for distributed tracing
- [ ] 11.5 Document observability integrations

## Validation Checkpoints

After each phase:
1. Run `mvn clean verify` to ensure all tests pass
2. Run `openspec validate enhance-instructor-v2 --strict`
3. Review code coverage report
4. Test backward compatibility with V1 API
5. Update documentation with completed features

## Dependencies Between Tasks

- Tasks 1.x must complete before 2.6 (hook error handling needs typed exceptions)
- Task 2.x must complete before 3.6 (hooks need to fire during retry)
- Task 4.x can be done in parallel with 5.x and 6.x
- Task 7.x requires 2.x complete (cache metrics via hooks)
- Streaming tasks moved to separate change `add-instructor-streaming`

## Parallelization Opportunities

- **Phase 1**: Tasks 1.x and 2.x can be done in parallel
- **Phase 2**: Tasks 5.x and 6.x can be done in parallel after 4.x
- **Phase 3**: Tasks 7.x and 8.x are fully independent

## Rollback Plan

If critical issues arise:
1. Feature toggle: Set `instructor.v2.enabled=false` to revert to V1 behavior
2. V1 constructors remain functional throughout V2 lifecycle
3. Each phase can be released independently (gradual rollout)

