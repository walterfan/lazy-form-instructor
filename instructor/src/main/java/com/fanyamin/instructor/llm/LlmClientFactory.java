package com.fanyamin.instructor.llm;

/**
 * Factory for creating LLM clients based on configuration.
 * Uses OpenAI-compatible API which works with multiple providers.
 */
public class LlmClientFactory {

    /**
     * Create an LLM client from environment variables.
     * Uses OpenAI-compatible API client.
     * 
     * Configuration:
     * - LLM_API_KEY: API key (optional for local services)
     * - LLM_BASE_URL: API endpoint (default: OpenAI)
     * - LLM_MODEL: Model name (default: gpt-4-turbo-preview)
     * - LLM_TEMPERATURE: Temperature (default: 0.7)
     * - LLM_MAX_TOKENS: Max tokens (default: 4096)
     * 
     * @return OpenAiLlmClient instance configured from environment
     */
    public static LlmClient createFromEnvironment() {
        return new OpenAiLlmClient();
    }

    /**
     * Create an OpenAI client with default environment configuration.
     */
    public static OpenAiLlmClient create() {
        return new OpenAiLlmClient();
    }

    /**
     * Create an OpenAI client with specific API key.
     */
    public static OpenAiLlmClient create(String apiKey) {
        return new OpenAiLlmClient(apiKey);
    }

    /**
     * Create an OpenAI client with specific API key and model.
     */
    public static OpenAiLlmClient create(String apiKey, String model) {
        return new OpenAiLlmClient(apiKey, model);
    }

    /**
     * Create a builder for custom configuration.
     */
    public static OpenAiLlmClient.Builder builder() {
        return new OpenAiLlmClient.Builder();
    }

    /**
     * Create a mock client for testing.
     */
    public static MockLlmClient createMock() {
        return new MockLlmClient();
    }

    /**
     * Create a mock client with predefined response.
     */
    public static MockLlmClient createMock(String mockResponse) {
        MockLlmClient mock = new MockLlmClient();
        mock.setMockResponse(mockResponse);
        return mock;
    }
}

