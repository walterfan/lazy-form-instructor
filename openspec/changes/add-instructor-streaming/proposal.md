# Change: Add Streaming Support to SmartFormInstructor

## Why

Long-running extractions provide no real-time feedback today. Streaming output enables better UX (progressive rendering), faster perceived latency, and earlier error detection while keeping strict JSON Schema validation at completion.

## What Changes

- Add a **streaming parsing API** on `SmartFormInstructor`:
  - `parseStreaming(ParsingRequest): Flux<StreamingParseEvent>`
- Extend `LlmClient` with **streaming chat** support:
  - `streamChat(prompt): Flux<String>` (default fallback emits a single chunk from `chat()`).
- Provide a **streaming event model** to support:
  - raw chunk streaming
  - best-effort parsed snapshots (when buffered JSON becomes parseable)
  - final schema validation result (validation runs only on the final assembled JSON)
- Implement OpenAI-compatible **SSE streaming** in `OpenAiLlmClient`.

## Impact

- **Affected specs**:
  - `performance`: streaming UX / incremental parsing behavior
  - `core-api` (API surface): new `parseStreaming` method and event types
  - `llm-integration` (API surface): `LlmClient.streamChat`
- **Affected code**:
  - `smart-form-instructor/src/main/java/com/fanyamin/SmartFormInstructor.java`
  - `smart-form-instructor/src/main/java/com/fanyamin/instructor/llm/LlmClient.java`
  - `smart-form-instructor/src/main/java/com/fanyamin/instructor/llm/OpenAiLlmClient.java`
  - `smart-form-instructor/src/main/java/com/fanyamin/instructor/streaming/StreamingParseEvent.java`



