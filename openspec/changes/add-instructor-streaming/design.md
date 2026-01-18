# Technical Design: Streaming Support (SmartFormInstructor)

## Context

`SmartFormInstructor.parse()` returns only after the LLM finishes and the response is validated. For many prompts, this can take multiple seconds, leaving UI/clients without progress visibility.

## Goals / Non-Goals

### Goals
- Stream raw LLM output as it arrives.
- Provide best-effort parsed snapshots for early UI updates.
- Validate JSON Schema strictly **only at the end** of an attempt.
- Keep existing `parse()` API unchanged and backward compatible.

### Non-Goals
- Perfect incremental JSON parsing of arbitrarily malformed outputs.
- Full schema validation on partial JSON (final-only validation).
- Mandating WebFlux for all consumers (streaming is exposed via Reactor `Flux`).

## API Design

### SmartFormInstructor

- `Flux<StreamingParseEvent> parseStreaming(ParsingRequest request)`
  - emits `AttemptStarted`
  - emits `RawChunk` as LLM chunks arrive
  - may emit `Snapshot` when buffered JSON becomes parseable as `ParsingResult`
  - emits `AttemptFailed` on schema validation failure (then may retry)
  - emits `FinalResult` once validation succeeds or retries are exhausted

### LlmClient

- `Flux<String> streamChat(String prompt)`
  - default fallback emits a single chunk using existing `chat()`
  - streaming-capable implementations override and set `supportsStreaming()=true`

## Data Flow

1. Build prompt from `PromptManager`.
2. Subscribe to `llmClient.streamChat(prompt)` and append chunks to an in-memory buffer.
3. Emit `RawChunk` per chunk.
4. Periodically attempt to parse the buffer as `ParsingResult`:
   - on success, emit `Snapshot` (best-effort; not schema-validated).
5. On stream completion, parse final JSON and run schema validation once:
   - if OK: emit `FinalResult` with empty schema errors
   - else: emit `AttemptFailed` and retry with an error-correction prompt

## Risks / Trade-offs

- **Thread lifecycle**: Reactor schedulers may keep threads alive (especially in CLI apps).
  - Mitigation: examples should shut down schedulers when running under `mvn exec:java`.
- **Partial JSON parsing is fragile**:
  - Mitigation: snapshots are best-effort only; correctness guaranteed only by final result.



