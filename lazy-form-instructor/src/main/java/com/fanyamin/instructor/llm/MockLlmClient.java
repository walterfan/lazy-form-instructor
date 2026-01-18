package com.fanyamin.instructor.llm;

import reactor.core.publisher.Flux;

import java.util.List;

public class MockLlmClient implements LlmClient {
    
    private String mockResponse;
    private List<String> mockStreamChunks;

    public void setMockResponse(String mockResponse) {
        this.mockResponse = mockResponse;
    }

    /**
     * Set explicit streaming chunks to emit in order.
     * If set, {@link #streamChat(String)} will emit these chunks instead of a single full response.
     */
    public void setMockStreamChunks(List<String> mockStreamChunks) {
        this.mockStreamChunks = mockStreamChunks;
    }

    @Override
    public String chat(String prompt) {
        if (mockResponse == null) {
            // Default dummy response matching the protocol
            return """
                {
                  "fields": {
                    "mockField": {
                      "value": "mockValue",
                      "confidence": 0.9,
                      "reasoning": "This is a mock response.",
                      "alternatives": []
                    }
                  },
                  "errors": []
                }
                """;
        }
        return mockResponse;
    }

    @Override
    public Flux<String> streamChat(String prompt) {
        if (mockStreamChunks != null && !mockStreamChunks.isEmpty()) {
            return Flux.fromIterable(mockStreamChunks);
        }
        // Default: emit once with the full mock response
        return Flux.just(chat(prompt));
    }
}

