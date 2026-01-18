# Implementation Tasks: Streaming Support

## 1. Streaming API Surface
- [ ] 1.1 Extend `LlmClient` with `streamChat(prompt): Flux<String>` and `supportsStreaming()`
- [ ] 1.2 Implement OpenAI-compatible SSE streaming in `OpenAiLlmClient`
- [ ] 1.3 Provide safe fallback streaming in `SpringAiLlmClient` and `MockLlmClient`

## 2. SmartFormInstructor Streaming
- [ ] 2.1 Add `StreamingParseEvent` model (raw chunk, snapshot, attempt failed, final result)
- [ ] 2.2 Implement `SmartFormInstructor.parseStreaming()` buffering + emitting events
- [ ] 2.3 Ensure schema validation runs only on final assembled JSON for an attempt
- [ ] 2.4 Support retries in streaming path (emit attemptFailed then retry)

## 3. Tests & Examples
- [ ] 3.1 Unit tests for streaming event ordering and final schema validation behavior
- [ ] 3.2 Update example module(s) to demonstrate streaming output and final schema validation
- [ ] 3.3 Document known limitations (best-effort snapshots; final-only validation)



