# SmartFormInstructor V2 Enhancement - Quick Reference

## 📋 Proposal Overview

**Change ID**: `enhance-instructor-v2`  
**Status**: ✅ Validated, Ready for Review  
**Effort**: 5-6 weeks, 88 tasks across 3 phases

## 🎯 Goals

Transform `smart-form-instructor` from prototype to production-grade library with:

1. **Observability** - Hook system for logs/metrics/tracing
2. **Reliability** - Advanced retry, circuit breaking, timeouts
3. **Validation** - Custom validators beyond JSON Schema
4. **Performance** - Caching support
5. **DX** - Fluent builder API and typed exceptions

## 📊 Capabilities

```
┌─────────────────────────────────────────────────────────────┐
│  Core API (MODIFIED)                                        │
│  • Builder API                                              │
│  • Typed exceptions (5 new types)                           │
│  • Enhanced retry with backoff + jitter                     │
│  • Backward compatibility with V1                           │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  Observability (NEW)                                        │
│  • Hook system (beforeRequest, afterResponse, onError)      │
│  • Correlation IDs for tracing                              │
│  • Metrics integration (Micrometer)                         │
│  • Distributed tracing (OpenTelemetry)                      │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  Reliability (NEW)                                          │
│  • Exponential backoff with jitter                          │
│  • Circuit breaking                                         │
│  • Exception-specific retry policies                        │
│  • Timeout management (overall + per-attempt)               │
│  • Backpressure handling                                    │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  Validation (NEW)                                           │
│  • Custom validator framework                               │
│  • @SchemaValidate annotation                               │
│  • Cross-field validation context                           │
│  • Built-in validators (DateRange, Enum, Pattern)           │
│  • Validation retry integration                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  Performance (NEW)                                          │
│  • Response caching (NoOp, InMemory, File)                  │
│  • Cache key generation (SHA-256)                           │
│  • TTL-based expiration                                     │
│  • (Moved) Streaming support tracked in `add-instructor-streaming` │
└─────────────────────────────────────────────────────────────┘
```

## 🏗️ Architecture

### V1 (Current)
```
┌─────────────────────────┐
│  SmartFormInstructor    │
│  - llmClient            │
│  - maxRetries (fixed)   │
│  - parse()              │
└─────────────────────────┘
         ↓
    [LlmClient]
         ↓
    [LLM Provider]
```

### V2 (Enhanced)
```
┌────────────────────────────────────────────────────┐
│  SmartFormInstructor (Builder Pattern)             │
│  ┌──────────────┬──────────────┬─────────────┐    │
│  │ HookManager  │ RetryManager │ CacheManager│    │
│  └──────────────┴──────────────┴─────────────┘    │
│  ┌──────────────┬──────────────────────────┐      │
│  │ValidationEng.│ ModeHandler             │       │
│  └──────────────┴──────────────────────────┘      │
└────────────────────────────────────────────────────┘
         ↓                    ↓
    [LlmClient]          [Cache Layer]
         ↓                    ↓
    [Circuit Breaker]    [InMemory/File]
         ↓
    [LLM Provider]
```

## 📦 Package Structure

```
com.fanyamin.instructor/
├── api/                  # Existing: ParsingRequest, ParsingResult
├── exception/            # Enhanced: +5 new exception types
│   ├── InstructorException (base)
│   ├── ValidationException
│   ├── RetryExhaustedException
│   ├── RateLimitException
│   ├── IncompleteOutputException
│   └── ModeException
├── hooks/                # NEW: Observability
│   ├── InstructorHook
│   ├── HookContext
│   ├── HookManager
│   └── LoggingHook
├── retry/                # NEW: Reliability
│   ├── RetryConfig
│   ├── RetryManager
│   ├── BackoffStrategy
│   └── CircuitBreaker
├── cache/                # NEW: Performance
│   ├── ResponseCache
│   ├── NoOpCache
│   ├── InMemoryCache
│   └── FileCache
├── validation/           # NEW: Custom Validation
│   ├── FieldValidator<T>
│   ├── ValidationContext
│   ├── ValidationEngine
│   ├── @SchemaValidate
│   └── validators/
│       ├── DateRangeValidator
│       ├── EnumValidator
│       └── PatternValidator
├── mode/                 # NEW: Response Modes
│   ├── ResponseMode
│   └── ModeHandler
// (Moved) streaming/ is tracked in `add-instructor-streaming`
```

## 🔄 API Evolution

### V1 (Deprecated but Functional)
```java
// Old constructor - still works
SmartFormInstructor instructor = new SmartFormInstructor(llmClient);
ParsingResult result = instructor.parse(request);
```

### V2 (Recommended)
```java
// Fluent builder with full configurability
SmartFormInstructor instructor = SmartFormInstructor.builder()
    .llmClient(llmClient)
    .retryConfig(RetryConfig.builder()
        .maxAttempts(5)
        .backoff(BackoffStrategy.EXPONENTIAL)
        .jitterFactor(0.1)
        .build())
    .withCache(new InMemoryCache(100))
    .addHook(new LoggingHook())
    .addHook(new MetricsHook())
    .build();

ParsingResult result = instructor.parse(request);
```

### Custom Validation
```java
public class LeaveRequest {
    @SchemaValidate(validator = DateRangeValidator.class)
    private LocalDate startDate;
    private LocalDate endDate;
}

// DateRangeValidator checks startDate < endDate
```

## 📈 Implementation Roadmap

```
Phase 1: Foundation (Weeks 1-2)
  ├─ Exception hierarchy (6 tasks)
  ├─ Hook system (7 tasks)
  └─ Retry strategy (9 tasks)
  
Phase 2: DX & Flexibility (Weeks 3-4)
  ├─ Builder API (7 tasks)
  ├─ Custom validation (8 tasks)
  └─ Response modes (9 tasks)
  
Phase 3: Performance (Weeks 5-6)
  ├─ Caching (11 tasks)
  └─ (Moved) Streaming tasks live in `add-instructor-streaming`
  
Cross-Cutting
  ├─ Testing (7 tasks)
  ├─ Documentation (7 tasks)
  └─ Observability modules (5 tasks)
```

## ⚠️ Breaking Changes & Migration

### Breaking Changes
- Constructor signatures change (old ones deprecated)
- New exception types may be thrown

### Migration Strategy
1. **Phase 1**: No action needed - V1 API works with V2 defaults
2. **Phase 2**: Optional migration to builder API
3. **Phase 3**: Update exception handling for typed exceptions
4. **Timeline**: 6 months deprecation period before V1 removal

### Zero-Config Example
```java
// V1 code still works in V2 - no changes needed!
SmartFormInstructor instructor = new SmartFormInstructor(llmClient);
```

## ✅ Validation Status

```bash
$ openspec validate enhance-instructor-v2 --strict
Change 'enhance-instructor-v2' is valid
✅ Validation passed!

$ openspec list
Changes:
  enhance-instructor-v2     0/88 tasks
```

## 📚 Documentation

- **Proposal**: `openspec/changes/enhance-instructor-v2/proposal.md`
- **Design**: `openspec/changes/enhance-instructor-v2/design.md`
- **Tasks**: `openspec/changes/enhance-instructor-v2/tasks.md`
- **Summary**: `openspec/changes/enhance-instructor-v2/SUMMARY.md`
- **Specs**:
  - `specs/core-api/spec.md` (MODIFIED + 3 ADDED requirements)
  - `specs/observability/spec.md` (5 ADDED requirements)
  - `specs/reliability/spec.md` (6 ADDED requirements)
  - `specs/validation/spec.md` (7 ADDED requirements)
  - `specs/performance/spec.md` (6 ADDED requirements)

## 🎬 Next Steps

1. **Review**: Stakeholder approval of proposal
2. **Kickoff**: Assign Phase 1 tasks
3. **Implement**: Follow 3-phase roadmap
4. **Test**: Comprehensive acceptance tests
5. **Document**: Migration guides and examples
6. **Release**: V2.0 with feature toggle

---

**Status**: 🟢 Ready for Implementation  
**Risk Level**: 🟡 Medium (mitigated by backward compatibility)  
**Expected ROI**: 🟢 High (production-ready library)

