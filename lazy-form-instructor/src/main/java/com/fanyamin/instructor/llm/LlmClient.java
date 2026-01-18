package com.fanyamin.instructor.llm;

import reactor.core.publisher.Flux;

/**
 * Abstraction for interacting with an LLM provider.
 *
 * <p>Implementations may use Spring AI, OpenAI-compatible HTTP APIs, or mocks.
 * This interface supports both synchronous and streaming interactions.</p>
 */
public interface LlmClient {
    /**
     * Perform a single, non-streaming chat completion and return the full content as a String.
     */
    String chat(String prompt);

    /**
     * Perform a streaming chat completion and return a stream of incremental content chunks.
     *
     * <p>Default implementation is a safe fallback: it emits a single chunk from {@link #chat(String)}.</p>
     *
     * <p>Implementations that support true streaming SHOULD override this method and also override
     * {@link #supportsStreaming()} to return true.</p>
     */
    default Flux<String> streamChat(String prompt) {
        return Flux.defer(() -> Flux.just(chat(prompt)));
    }

    /**
     * Whether this client supports true streaming (multiple chunks over time).
     *
     * <p>If false, {@link #streamChat(String)} may still work but typically emits a single chunk.</p>
     */
    default boolean supportsStreaming() {
        return false;
    }
}

