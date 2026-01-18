# Technical Design: LazyFormInstructor V2

## Context

The current `LazyFormInstructor` is a 100-line class with hardcoded retry logic and no extensibility points. To reach production maturity, we need to add observability, reliability, and performance features without compromising simplicity for basic use cases.

**Constraints:**
- Must maintain backward compatibility with V1 API (deprecate, don't break)
- No new external dependencies beyond existing Spring ecosystem
- Keep zero-config default behavior simple
- Follow engineering best practices (feature toggles, resilience, observability)

**Stakeholders:**
- Internal developers using the library for form extraction
- Operations teams needing metrics and logs
- Performance engineers optimizing LLM call costs

## Goals / Non-Goals

### Goals
1. **Observability-first**: Every request traceable via hooks for logs/metrics/traces
2. **Configurable Retry**: Replace hardcoded loop with strategy pattern
3. **Extensible Validation**: Support custom business logic beyond JSON Schema
4. **Cost Optimization**: Caching to reduce redundant LLM calls
5. **Production-ready exceptions**: Typed errors for proper handling

### Non-Goals
- Breaking existing V1 API (provide migration path)
- Adding new LLM providers (focus on infrastructure, not providers)
- Building a UI/dashboard (hooks enable external tools)
- Distributed caching (local cache only for V2)

## Decisions

### 1. Hook System Architecture

**Decision**: Use interceptor pattern with lifecycle callbacks

```java
public interface InstructorHook {
    default void beforeRequest(HookContext ctx) {}
    default void afterResponse(HookContext ctx) {}
    default void onError(HookContext ctx, Exception e) {}
    default void onRetry(HookContext ctx, Exception lastError) {}
}
```

**Why:**
- Non-invasive: Existing code unchanged, hooks are opt-in
- Composable: Multiple hooks can be registered (logging + metrics + tracing)
- Spring-friendly: Hooks can be Spring beans with DI

**Alternatives Considered:**
- AOP/AspectJ: Too heavyweight, adds compile-time weaving complexity
- Events (Spring ApplicationEvent): Async by default, harder to guarantee ordering
- Middleware chain: Overly complex for this use case

### 2. Retry Strategy

**Decision**: Strategy pattern with configurable backoff policies

```java
public class RetryConfig {
    int maxAttempts = 3;
    BackoffStrategy backoff = BackoffStrategy.EXPONENTIAL;
    Duration initialInterval = Duration.ofSeconds(1);
    double multiplier = 2.0;
    double jitterFactor = 0.1;
    List<Class<? extends Exception>> retryOn = List.of(RateLimitException.class);
}
```

**Why:**
- Follows best practice: "Retry strategy with exponential backoff and jitter"
- Configurable per use case (rate limits vs validation errors)
- Testable: Mock time for deterministic tests

**Trade-offs:**
- More complex than simple loop, but necessary for production reliability
- Requires time abstraction for testing (use `Clock` injectable)

### 3. Custom Validation Framework

**Decision**: Annotation-driven validators with access to full object context

```java
public interface FieldValidator<T> {
    ValidationResult validate(T value, ValidationContext context);
}

@Target(ElementType.FIELD)
public @interface SchemaValidate {
    Class<? extends FieldValidator> validator();
}
```

**Why:**
- Familiar pattern (JSR-303/Bean Validation style)
- Type-safe: Validators are generic on field type
- Context-aware: Can access other fields for cross-field validation

**Alternatives Considered:**
- Extend JSON Schema with custom keywords: Too complex, requires schema parser changes
- Lambda-based validators: Less discoverable, no type safety
- Rule engine: Overkill for common validation patterns

### 4. Caching Strategy

**Decision**: Pluggable cache interface with key = hash(prompt + model + schema + temperature)

```java
public interface ResponseCache {
    Optional<String> get(String key);
    void put(String key, String response, Duration ttl);
}
```

**Implementations:**
- `NoOpCache` (default): No caching overhead for non-repetitive workloads
- `InMemoryCache`: ConcurrentHashMap with LRU eviction
- `FileCache`: Disk-based persistence for development/debugging

**Why:**
- Cache key includes all prompt-affecting parameters (schema, temperature, model)
- TTL support for time-sensitive data
- Simple interface allows custom implementations (Redis, etc.)

**Trade-offs:**
- Cache invalidation: Rely on TTL, no active invalidation (acceptable for LLM responses)
- No distributed caching: Single-instance only (V2 limitation)

### 5. Builder API Design

**Decision**: Static builder with fluent methods

```java
SmartFormInstructor instructor = SmartFormInstructor.builder()
    .llmClient(llmClient)
    .retryConfig(retryConfig)
    .withCache(new FileCache("/tmp/instructor"))
    .addHook(new LoggingHook())
    .addHook(new MetricsHook())
    .build();
```

**Why:**
- Industry standard (Lombok `@Builder` style)
- Optional configuration: Only specify what you need
- Composable: Multiple hooks, validators, etc.

**V1 Compatibility:**
- Old constructors still work, internally delegate to builder with defaults
- Add `@Deprecated` in V2.1, remove in V3.0

## Risks / Trade-offs

### Risk: Increased Complexity
**Impact**: Library becomes harder to understand for simple use cases
**Mitigation**: 
- Keep zero-config path simple (`new SmartFormInstructor(llmClient)` still works)
- Provide preset configurations (e.g., `RetryConfig.production()`)
- Comprehensive examples and migration guide

### Risk: Breaking Changes
**Impact**: Existing users need to update code
**Mitigation**:
- Maintain V1 API with deprecation warnings for 6 months
- Provide automated migration tool (AST-based code rewriter if needed)
- Clear migration documentation with before/after examples

### Risk: Performance Overhead
**Impact**: Hooks and retry logic add latency
**Mitigation**:
- Hooks are opt-in (no overhead if not used)
- Cache reduces LLM calls (net performance win)
- Benchmark and document overhead (<5ms per request for hook execution)

### Trade-off: Streaming Complexity
**Benefit**: Better UX for long-running extractions
**Cost**: 
- Incremental JSON parsing is error-prone (malformed JSON)
- DTOs must be nullable-tolerant
- Testing requires more scenarios (partial states)

**Decision**: Phase 3 feature (optional), clearly document limitations

## Migration Plan

### Phase 1 (Week 1-2): Foundation
1. Add exception hierarchy
2. Implement hook system
3. Add retry configuration
4. **Breaking**: None (additive only)
5. **Migration**: No action required, new features opt-in

### Phase 2 (Week 3-4): DX & Validation
1. Fluent builder API
2. Custom validation framework
3. Response modes
4. **Breaking**: Deprecate old constructors (still functional)
5. **Migration**: Optional upgrade to builder pattern

### Phase 3 (Week 5-6): Performance
1. Caching layer
2. **Breaking**: None (opt-in features)
3. **Migration**: Enable caching via builder

### Rollout Strategy
- Feature toggle: `instructor.v2.enabled=true` (default: true in V2.0)
- Gradual adoption: Document V2 features in release notes
- Deprecation timeline: V1 API deprecated in V2.1, removed in V3.0 (6+ months)

## Open Questions

1. **Micrometer Integration**: Should metrics hooks be built-in or user-provided?
   - **Proposal**: Provide `MicrometerHook` implementation in separate module to avoid hard dependency
   
2. **OpenTelemetry Tracing**: Similar to metrics, separate module?
   - **Proposal**: Yes, `smart-form-instructor-otel` module for users who need it

3. **Cache Key Collision**: How to handle hash collisions in cache keys?
   - **Proposal**: Use SHA-256 hash (collision probability negligible), include version in key

4. **Streaming DTO Requirements**: Should we validate that target class has nullable fields?
   - **Proposal**: Runtime check in `parseStreaming()` - fail fast if DTO has primitive types

5. **Retry on What Exceptions**: Should schema validation failures trigger retry by default?
   - **Proposal**: Yes, but configurable via `RetryConfig.retryOn`

