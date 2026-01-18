# SmartFormInstructor V2 Architecture Design

## 1. Executive Summary

This document outlines the architectural evolution of `smart-form-instructor` (V2) to match the maturity of the Python Instructor library. The goal is to transform the current prototype into a **production-grade, observable, and extensible** library for structured data extraction.

**Key Design Goals:**
1.  **Observability**: First-class support for logging, metrics, and tracing via Hooks.
2.  **Reliability**: Robust retry mechanisms with exponential backoff and circuit breaking.
3.  **Flexibility**: Custom validation logic beyond JSON Schema.
4.  **Performance**: Caching and Streaming support for better UX and cost efficiency.
5.  **Developer Experience**: Typed exceptions, fluent builders, and DSL patterns.

---

## 2. Core Architecture

### 2.1 Component Diagram

```mermaid
graph TB
    subgraph ClientLayer [Client Integration]
        App[Application]
        Config[Retry/Cache Config]
    end

    subgraph CoreEngine [Instructor Core]
        API[SmartFormInstructor]
        HookMgr[Hook Manager]
        RetryMgr[Retry Strategy]
        CacheMgr[Response Cache]
    end

    subgraph ProcessingLayer [Processing Pipeline]
        Mode[Mode Handler]
        Schema[Schema Generator]
        Validator[Validation Engine]
        Stream[Streaming Parser]
    end

    subgraph Infrastructure [Infrastructure]
        LlmClient[LlmClient Interface]
        SpringAI[Spring AI Implementation]
        OpenAI[OpenAI Direct (Optional)]
    end

    App --> API
    Config --> API
    API --> HookMgr
    API --> CacheMgr
    CacheMgr --> RetryMgr
    RetryMgr --> Mode
    Mode --> LlmClient
    LlmClient --> Stream
    Stream --> Validator
    Validator --> Schema
```

---

## 3. Detailed Design

### 3.1 Error Handling & Exceptions

Move from generic errors to a typed exception hierarchy to allow applications to react differently to distinct failure modes (e.g., retrying on rate limits but failing on validation errors).

**Package**: `com.fanyamin.instructor.exception`

*   `InstructorException` (Base RuntimeException)
    *   `ValidationException`: Schema or custom validation failed. Contains `List<ValidationError>`.
    *   `RetryExhaustedException`: Max retries reached. Contains `lastErrors` and `attempts`.
    *   `IncompleteOutputException`: LLM output truncated (token limit). Contains partial JSON.
    *   `ModeException`: Configuration invalid for selected provider/mode.
    *   `RateLimitException`: LLM provider rate limit hit.

### 3.2 Hooks & Observability System

A non-invasive way to instrument the library for logging, metrics (Prometheus/Micrometer), and tracing (OpenTelemetry/Langfuse).

**Package**: `com.fanyamin.instructor.hooks`

**Interface**: `InstructorHook`
```java
public interface InstructorHook {
    default void beforeRequest(HookContext ctx) {}
    default void afterResponse(HookContext ctx) {}
    default void onError(HookContext ctx, Exception e) {}
    default void onRetry(HookContext ctx, Exception lastError) {}
}
```

**Context**: `HookContext` contains correlation ID, start time, prompt, model, model options, etc.

### 3.3 Advanced Retry Strategy

Replace the hardcoded loop with a configurable strategy using exponential backoff and jitter.

**Package**: `com.fanyamin.instructor.retry`

**Configuration**: `RetryConfig`
*   `maxAttempts`: int (default 3)
*   `backoffStrategy`: `FIXED`, `EXPONENTIAL`
*   `initialInterval`: Duration
*   `multiplier`: double
*   `retryOn`: `List<Class<? extends Exception>>` (e.g., `RateLimitException`)

### 3.4 Enhanced Validation System

Extend JSON Schema validation with Java-based custom validators for complex business logic.

**Package**: `com.fanyamin.instructor.validation`

**Annotations**:
*   `@SchemaValidate(validator = MyCustomValidator.class)`

**Interface**: `FieldValidator<T>`
```java
public interface FieldValidator<T> {
    ValidationResult validate(T value, ValidationContext context);
}
```
**Context**: Allows access to other fields in the object (cross-field validation).

### 3.5 Caching Layer

Avoid redundant LLM calls for identical requests.

**Package**: `com.fanyamin.instructor.cache`

**Interface**: `ResponseCache`
*   `get(String key)`
*   `put(String key, String response)`

**Implementations**:
*   `NoOpCache` (default)
*   `InMemoryCache` (ConcurrentHashMap)
*   `FileCache` (Disk-based persistence)

**Key Generation**: Hash of (Prompt + Model + Schema + Temperature).

### 3.6 Response Modes

Optimize interaction pattern based on provider capabilities.

**Package**: `com.fanyamin.instructor.mode`

**Enum**: `ResponseMode`
*   `JSON`: Standard JSON mode (prompt engineering).
*   `TOOLS`: OpenAI Function Calling.
*   `TOOLS_STRICT`: OpenAI Structured Outputs (strict schema).
*   `MD_JSON`: Markdown-wrapped JSON (common for reasoning models).

### 3.7 Streaming Support

Enable real-time feedback for long-running extractions.

**Package**: `com.fanyamin.instructor.streaming`

**Interface**: `StreamingInstructor`
*   `Stream<PartialResult<T>> parseStreaming(ParsingRequest request)`

**Implementation**:
*   Requires incremental JSON parsing (e.g., logic similar to `jiter` in Python).
*   Needs to update `LlmClient` to expose `Flux<String>` or `Stream<String>`.

#### 3.7.1 Detailed Streaming Design

**Goal**: Provide real-time updates of the extracted object as tokens arrive from the LLM, enabling improved user experience for long-running tasks.

**1. LlmClient Extension**

The `LlmClient` interface must be extended to support streaming.

```java
public interface LlmClient {
    // Existing synchronous method
    String chat(String prompt);

    // New streaming method (using Project Reactor Flux for compatibility with Spring AI)
    default reactor.core.publisher.Flux<String> streamChat(String prompt) {
        throw new UnsupportedOperationException("Streaming not implemented");
    }
}
```

**2. Incremental JSON Parsing**

Parsing incomplete JSON is the core challenge. Since standard JSON parsers fail on partial inputs (e.g., `{"name": "Jo`), we need a tolerant parser.

**Strategy**:
1.  **Buffer & Repair**: Append tokens to a buffer. Try to "close" open braces/quotes to make it valid JSON, then parse.
2.  **Streaming Parser (Recommended)**: Use a parser designed for streams (like Jackson's `JsonParser`) or a custom state machine that emits events when fields are completed.

For Java, a robust approach is using a `JsonTokenParser` state machine:

```java
public class IncrementalJsonParser {
    private final StringBuilder buffer = new StringBuilder();
    private final ObjectMapper mapper = new ObjectMapper();

    public Optional<JsonNode> parsePartial(String token) {
        buffer.append(token);
        String current = buffer.toString();
        
        // Attempt to repair common truncation issues (unclosed quotes, braces)
        String repaired = repairJson(current);
        
        try {
            return Optional.of(mapper.readTree(repaired));
        } catch (Exception e) {
            return Optional.empty(); // Still invalid, wait for more tokens
        }
    }
    
    // Naive repair logic (to be refined)
    private String repairJson(String raw) {
        // Count unclosed braces/quotes and append them
        // This is complex to get right; consider using a library like 
        // a Java port of 'jiter' or specific streaming JSON libraries.
        return raw; 
    }
}
```

**3. PartialResult Model**

The streaming API returns a wrapper containing the partial object and metadata.

```java
public record PartialResult<T>(
    T partialObject,       // The object populated with currently available data
    double progress,       // Estimated progress (0.0 - 1.0)
    boolean isComplete     // True if parsing is finished
) {}
```

**4. Streaming Workflow**

1.  **Request**: User calls `instructor.parseStreaming(request)`.
2.  **LLM Call**: `LlmClient.streamChat(prompt)` initiates the request.
3.  **Stream Processing**:
    *   Subscribe to the token stream.
    *   On each token, update the `IncrementalJsonParser`.
    *   If the parser yields a valid (partial) JSON tree, map it to the target Class `T`.
        *   *Note*: The DTO `T` must be tolerant of missing fields (all fields nullable/Optional).
    *   Emit a `PartialResult<T>` with the updated object.
4.  **Completion**: When LLM stream ends, perform final validation.

---

## 4. Migration Guide (V1 to V2)

The V2 API is designed to be largely backward compatible, but offers a new fluent builder entry point.

**V1 (Deprecated but supported):**
```java
SmartFormInstructor instructor = new SmartFormInstructor(llmClient);
```

**V2 (Recommended):**
```java
SmartFormInstructor instructor = SmartFormInstructor.builder()
    .llmClient(llmClient)
    .retryConfig(retryConfig)
    .withCache(new FileCache("/tmp/instructor"))
    .addHook(new LoggingHook())
    .build();
```

---

## 5. Roadmap

1.  **Phase 1 (Foundation)**: Exceptions, Hooks, Retry.
2.  **Phase 2 (DX)**: Custom Validators, DSL (Maybe pattern), Modes.
3.  **Phase 3 (Performance)**: Caching, Streaming.

---

## 6. Acceptance Test Cases

### 6.1 Observability & Hooks
- **Goal**: Verify that hooks are correctly triggered during the request lifecycle.
- **Setup**: Register a `TestHook` that records event timestamps.
- **Action**: Call `instructor.parse(request)`.
- **Assertion**:
    - `beforeRequest` called before LLM invocation.
    - `afterResponse` called after success.
    - `correlationId` matches in both events.

### 6.2 Retry Strategy
- **Goal**: Verify retry logic and exponential backoff.
- **Setup**: Mock LLM client to fail 2 times with `RateLimitException` then succeed. Configure `maxAttempts=3`.
- **Action**: Call `instructor.parse(request)`.
- **Assertion**:
    - LLM client called 3 times.
    - `onRetry` hook triggered 2 times.
    - Final result is successful.

### 6.3 Custom Validation
- **Goal**: Verify Java-based custom validation logic.
- **Setup**:
    - Define `DateRangeValidator` for `LeaveRequest` (start < end).
    - Annotate DTO with `@SchemaValidate`.
- **Action**: Input prompt implying invalid date range (start > end).
- **Assertion**:
    - Returns `ValidationException` or `ParsingResult` with errors.
    - Error message matches validator output ("Start date must be before end date").

### 6.4 Caching
- **Goal**: Verify redundant requests don't hit the LLM.
- **Setup**: Enable `InMemoryCache`.
- **Action**:
    - Run `parse(request1)`.
    - Run `parse(request1)` again immediately.
- **Assertion**:
    - LLM client called exactly once.
    - Second response time is near-zero.

### 6.5 Streaming (Future Phase)
- **Goal**: Verify real-time partial updates.
- **Setup**: Use `StreamingInstructor`.
- **Action**: Subscribe to `parseStreaming(request)`.
- **Assertion**:
    - Receive multiple `PartialResult` events.
    - Fields populate incrementally (e.g., `name` arrives before `reason`).
