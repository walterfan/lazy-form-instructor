package com.fanyamin.instructor.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicBoolean;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

/**
 * OpenAI-compatible API client using HTTP client.
 * 
 * Works with:
 * - OpenAI (GPT-4, GPT-3.5-turbo, etc.)
 * - OpenAI thinking models (o1-preview, o1-mini) - automatically filters reasoning
 * - Azure OpenAI
 * - Local models via Ollama (with openai-compatible endpoint)
 * - LM Studio
 * - vLLM
 * - Any other OpenAI API-compatible service
 * 
 * Configuration via environment variables:
 * - LLM_API_KEY: API key (required for cloud services, optional for local)
 * - LLM_BASE_URL: API endpoint (default: https://api.openai.com/v1/chat/completions)
 * - LLM_MODEL: Model name (default: gpt-4-turbo-preview)
 * - LLM_TEMPERATURE: Temperature (default: 0.7)
 * - LLM_MAX_TOKENS: Max tokens (default: 4096)
 * - LLM_SKIP_SSL_VERIFY: Skip SSL certificate verification for self-hosted/private LLMs (default: false)
 *   Set to "true", "yes", or "1" to disable SSL verification
 * - LLM_DEBUG: Enable debug logging for request/response (default: false)
 * 
 * Thinking Mode Support:
 * For models with reasoning capabilities (like o1), this client:
 * - Logs reasoning separately when debug mode is enabled
 * - Automatically extracts the final answer from content
 * - Supports various formats: XML tags, markdown sections, or separate fields
 * 
 * Examples:
 * 
 * OpenAI:
 *   LLM_API_KEY=sk-...
 *   LLM_BASE_URL=https://api.openai.com/v1/chat/completions
 *   LLM_MODEL=gpt-4-turbo-preview
 * 
 * OpenAI o1 (thinking model):
 *   LLM_API_KEY=sk-...
 *   LLM_BASE_URL=https://api.openai.com/v1/chat/completions
 *   LLM_MODEL=o1-preview
 *   LLM_DEBUG=true  # To see the reasoning process
 * 
 * Ollama (local):
 *   LLM_BASE_URL=http://localhost:11434/v1/chat/completions
 *   LLM_MODEL=llama2
 * 
 * Azure OpenAI:
 *   LLM_API_KEY=your-azure-key
 *   LLM_BASE_URL=https://your-resource.openai.azure.com/openai/deployments/your-deployment/chat/completions?api-version=2024-02-01
 *   LLM_MODEL=gpt-4
 */
public class OpenAiLlmClient implements LlmClient {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAiLlmClient.class);
    
    private final String apiKey;
    private final String model;
    private final String apiUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final double temperature;
    private final int maxTokens;
    private final boolean skipSslVerify;
    private final boolean debug;

    /**
     * Create client with configuration from environment variables.
     * API key is optional for local services (e.g., Ollama).
     */
    public OpenAiLlmClient() {
        this(LlmConfig.getEnv(LlmConfig.ENV_LLM_API_KEY, ""),
             LlmConfig.getEnv(LlmConfig.ENV_LLM_MODEL, LlmConfig.DEFAULT_OPENAI_MODEL),
             LlmConfig.getEnv(LlmConfig.ENV_LLM_BASE_URL, LlmConfig.DEFAULT_OPENAI_BASE_URL),
             LlmConfig.getEnvDouble(LlmConfig.ENV_LLM_TEMPERATURE, LlmConfig.DEFAULT_TEMPERATURE),
             LlmConfig.getEnvInt(LlmConfig.ENV_LLM_MAX_TOKENS, LlmConfig.DEFAULT_MAX_TOKENS),
             LlmConfig.getEnvBoolean(LlmConfig.ENV_LLM_SKIP_SSL_VERIFY, LlmConfig.DEFAULT_SKIP_SSL_VERIFY),
             LlmConfig.getEnvBoolean(LlmConfig.ENV_LLM_DEBUG, LlmConfig.DEFAULT_DEBUG));
    }

    public OpenAiLlmClient(String apiKey) {
        this(apiKey,
             LlmConfig.getEnv(LlmConfig.ENV_LLM_MODEL, LlmConfig.DEFAULT_OPENAI_MODEL),
             LlmConfig.getEnv(LlmConfig.ENV_LLM_BASE_URL, LlmConfig.DEFAULT_OPENAI_BASE_URL),
             LlmConfig.getEnvDouble(LlmConfig.ENV_LLM_TEMPERATURE, LlmConfig.DEFAULT_TEMPERATURE),
             LlmConfig.getEnvInt(LlmConfig.ENV_LLM_MAX_TOKENS, LlmConfig.DEFAULT_MAX_TOKENS),
             LlmConfig.getEnvBoolean(LlmConfig.ENV_LLM_SKIP_SSL_VERIFY, LlmConfig.DEFAULT_SKIP_SSL_VERIFY),
             LlmConfig.getEnvBoolean(LlmConfig.ENV_LLM_DEBUG, LlmConfig.DEFAULT_DEBUG));
    }

    public OpenAiLlmClient(String apiKey, String model) {
        this(apiKey, model, 
             LlmConfig.getEnv(LlmConfig.ENV_LLM_BASE_URL, LlmConfig.DEFAULT_OPENAI_BASE_URL),
             LlmConfig.getEnvDouble(LlmConfig.ENV_LLM_TEMPERATURE, LlmConfig.DEFAULT_TEMPERATURE),
             LlmConfig.getEnvInt(LlmConfig.ENV_LLM_MAX_TOKENS, LlmConfig.DEFAULT_MAX_TOKENS),
             LlmConfig.getEnvBoolean(LlmConfig.ENV_LLM_SKIP_SSL_VERIFY, LlmConfig.DEFAULT_SKIP_SSL_VERIFY),
             LlmConfig.getEnvBoolean(LlmConfig.ENV_LLM_DEBUG, LlmConfig.DEFAULT_DEBUG));
    }

    public OpenAiLlmClient(String apiKey, String model, String apiUrl, double temperature, int maxTokens) {
        this(apiKey, model, apiUrl, temperature, maxTokens, false, false);
    }

    public OpenAiLlmClient(String apiKey, String model, String apiUrl, double temperature, int maxTokens, boolean skipSslVerify) {
        this(apiKey, model, apiUrl, temperature, maxTokens, skipSslVerify, false);
    }

    public OpenAiLlmClient(String apiKey, String model, String apiUrl, double temperature, int maxTokens, boolean skipSslVerify, boolean debug) {
        this.apiKey = apiKey;
        this.model = model;
        this.apiUrl = apiUrl;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.skipSslVerify = skipSslVerify;
        this.debug = debug;
        this.httpClient = createHttpClient(skipSslVerify);
        this.objectMapper = new ObjectMapper();
        
        // Dynamically set log level to DEBUG when debug mode is enabled
        if (debug) {
            setLogLevel(ch.qos.logback.classic.Level.DEBUG);
            logger.info("🐛 DEBUG MODE: Request/Response logging enabled");
        }
        
        if (skipSslVerify) {
            logger.warn("⚠️  WARNING: SSL certificate verification is disabled!");
            logger.warn("   This should only be used for development/testing with self-signed certificates.");
        }
    }

    /**
     * Programmatically set the log level for this client's logger.
     */
    private void setLogLevel(ch.qos.logback.classic.Level level) {
        if (logger instanceof ch.qos.logback.classic.Logger) {
            ((ch.qos.logback.classic.Logger) logger).setLevel(level);
        }
    }

    /**
     * Create HTTP client with optional SSL verification skip for self-hosted LLMs.
     */
    private static HttpClient createHttpClient(boolean skipSslVerify) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2);
        
        if (skipSslVerify) {
            try {
                // Create a trust manager that accepts all certificates
                TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
                };
                
                // Install the all-trusting trust manager
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                
                builder.sslContext(sslContext);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                logger.error("Failed to disable SSL verification: {}", e.getMessage());
                logger.error("Falling back to default SSL verification");
            }
        }
        
        return builder.build();
    }

    @Override
    public String chat(String prompt) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);
            
            ArrayNode messages = requestBody.putArray("messages");
            ObjectNode userMessage = messages.addObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            String requestBodyStr = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestBody);
            
            if (debug) {
                logger.debug("\n{}", "=".repeat(80));
                logger.debug("🔵 HTTP REQUEST");
                logger.debug("{}", "=".repeat(80));
                logger.debug("URL: {}", apiUrl);
                logger.debug("Method: POST");
                logger.debug("Headers:");
                logger.debug("  Content-Type: application/json");
                if (apiKey != null && !apiKey.isEmpty()) {
                    logger.debug("  Authorization: Bearer {}", maskApiKey(apiKey));
                }
                logger.debug("\nBody:");
                logger.debug("{}", requestBodyStr);
                logger.debug("{}\n", "=".repeat(80));
            }

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyStr));
            
            // Add Authorization header only if API key is provided (not needed for local Ollama)
            if (apiKey != null && !apiKey.isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + apiKey);
            }
            
            HttpRequest request = requestBuilder.build();

            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long duration = System.currentTimeMillis() - startTime;

            if (debug) {
                logger.debug("🔵 HTTP RESPONSE");
                logger.debug("{}", "=".repeat(80));
                logger.debug("Status: {}", response.statusCode());
                logger.debug("Duration: {}ms", duration);
                logger.debug("\nHeaders:");
                response.headers().map().forEach((key, values) -> 
                    logger.debug("  {}: {}", key, String.join(", ", values))
                );
                logger.debug("\nBody:");
                try {
                    JsonNode responseJson = objectMapper.readTree(response.body());
                    logger.debug("{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseJson));
                } catch (Exception e) {
                    // If not JSON, print as-is
                    logger.debug("{}", response.body());
                }
                logger.debug("{}\n", "=".repeat(80));
            }

            if (response.statusCode() != 200) {
                String errorMsg = "OpenAI API error: " + response.statusCode() + " - " + response.body();
                logger.error("❌ {}", errorMsg);
                throw new RuntimeException(errorMsg);
            }

            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode firstChoice = responseJson.path("choices").get(0);
            JsonNode message = firstChoice.path("message");
            
            // Extract content - standard field for most models
            String content = message.path("content").asText();
            
            // For thinking models (like o1), there might be additional fields
            // Check if there's a "reasoning" or "thinking" field to log separately
            if (message.has("reasoning")) {
                String reasoning = message.path("reasoning").asText();
                if (debug && reasoning != null && !reasoning.isEmpty()) {
                    logger.debug("🧠 Model Reasoning ({} chars):", reasoning.length());
                    logger.debug("{}", "─".repeat(80));
                    logger.debug("{}", reasoning);
                    logger.debug("{}\n", "─".repeat(80));
                }
            }
            
            // Extract the actual answer, filtering out any thinking/reasoning content
            String answer = extractAnswerFromContent(content);
            
            if (debug) {
                if (!answer.equals(content)) {
                    logger.debug("📝 Raw content ({} chars):", content.length());
                    logger.debug("{}", "─".repeat(80));
                    logger.debug("{}", content);
                    logger.debug("{}\n", "─".repeat(80));
                }
                logger.debug("✅ Extracted answer ({} chars):", answer.length());
                logger.debug("{}", "─".repeat(80));
                logger.debug("{}", answer);
                logger.debug("{}\n", "─".repeat(80));
            }
            
            return answer;

        } catch (IOException | InterruptedException e) {
            String errorMsg = "Failed to call OpenAI API: " + e.getMessage();
            logger.error("❌ {}", errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    @Override
    public Flux<String> streamChat(String prompt) {
        return Flux.<String>create(sink -> startStreamingRequest(prompt, sink), FluxSink.OverflowStrategy.BUFFER)
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    private void startStreamingRequest(String prompt, FluxSink<String> sink) {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        sink.onCancel(() -> cancelled.set(true));

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("stream", true);

            ArrayNode messages = requestBody.putArray("messages");
            ObjectNode userMessage = messages.addObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            String requestBodyStr = objectMapper.writeValueAsString(requestBody);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyStr));

            if (apiKey != null && !apiKey.isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + apiKey);
            }

            HttpRequest request = requestBuilder.build();

            // Stream as lines (SSE)
            HttpResponse<java.util.stream.Stream<String>> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofLines());

            if (response.statusCode() != 200) {
                String errorMsg = "OpenAI streaming API error: " + response.statusCode();
                logger.error("❌ {} body not captured in streaming mode", errorMsg);
                sink.error(new RuntimeException(errorMsg));
                return;
            }

            response.body().forEach(line -> {
                if (cancelled.get() || sink.isCancelled()) {
                    return;
                }
                if (line == null) {
                    return;
                }
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    return;
                }
                if (!trimmed.startsWith("data:")) {
                    return;
                }

                String data = trimmed.substring("data:".length()).trim();
                if ("[DONE]".equals(data)) {
                    sink.complete();
                    return;
                }

                try {
                    JsonNode evt = objectMapper.readTree(data);
                    JsonNode choice0 = evt.path("choices").path(0);

                    // OpenAI streaming: choices[0].delta.content
                    String delta = null;
                    JsonNode deltaNode = choice0.path("delta");
                    if (!deltaNode.isMissingNode() && deltaNode.has("content")) {
                        delta = deltaNode.path("content").asText();
                    }

                    // Some compatible providers may stream in message.content
                    if ((delta == null || delta.isEmpty()) && choice0.has("message")) {
                        JsonNode msg = choice0.path("message");
                        if (msg.has("content")) {
                            delta = msg.path("content").asText();
                        }
                    }

                    if (delta != null && !delta.isEmpty()) {
                        sink.next(delta);
                    }
                } catch (Exception parseEx) {
                    // Don't fail the whole stream on a single malformed line; log and continue.
                    if (debug) {
                        logger.debug("Ignoring non-JSON SSE data line: {}", data, parseEx);
                    }
                }
            });

            if (!sink.isCancelled() && !cancelled.get()) {
                sink.complete();
            }
        } catch (IOException | InterruptedException e) {
            String errorMsg = "Failed to call OpenAI streaming API: " + e.getMessage();
            logger.error("❌ {}", errorMsg, e);
            sink.error(new RuntimeException(errorMsg, e));
        } catch (Exception e) {
            logger.error("❌ Unexpected streaming error: {}", e.getMessage(), e);
            sink.error(e);
        }
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * Extract the actual response content from a model that may include thinking/reasoning.
     * This handles various formats:
     * 1. Standard models: just return the content as-is
     * 2. Thinking models with structured format: extract the answer section
     * 3. Models with inline thinking markers: filter out thinking sections
     * 4. Models with <think> tags: extract content after </think>
     */
    private String extractAnswerFromContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        // Pattern 1: <think>...</think> tags (used by some Qwen models)
        // Extract everything after </think>
        if (content.contains("</think>")) {
            int thinkEnd = content.indexOf("</think>");
            if (thinkEnd != -1) {
                String answer = content.substring(thinkEnd + 8).trim();
                if (!answer.isEmpty()) {
                    logger.debug("🔍 Extracted answer after </think> tag (filtered {} chars of thinking)", thinkEnd + 8);
                    return answer;
                }
            }
        }
        
        // Pattern 2: Some models wrap thinking in XML-like tags
        // Example: <thinking>...</thinking>\n<answer>...</answer>
        if (content.contains("<thinking>") && content.contains("<answer>")) {
            int answerStart = content.indexOf("<answer>");
            int answerEnd = content.indexOf("</answer>");
            if (answerStart != -1 && answerEnd != -1) {
                String answer = content.substring(answerStart + 8, answerEnd).trim();
                logger.debug("🔍 Extracted answer from XML-tagged content");
                return answer;
            }
        }
        
        // Pattern 3: Markdown-style sections
        // Example: ## Thinking\n...\n## Answer\n...
        if (content.contains("## Thinking") && content.contains("## Answer")) {
            int answerStart = content.indexOf("## Answer");
            if (answerStart != -1) {
                String answer = content.substring(answerStart + 9).trim();
                // Remove the "## Answer" header if it's still there
                if (answer.startsWith("\n")) {
                    answer = answer.substring(1).trim();
                }
                logger.debug("🔍 Extracted answer from markdown-sectioned content");
                return answer;
            }
        }

        // Pattern 4: For o1-like models, the content itself is the answer
        // The thinking is in a separate field (handled in chat method)
        // So we just return the content as-is
        return content;
    }

    public static class Builder {
        private String apiKey = LlmConfig.getEnv(LlmConfig.ENV_LLM_API_KEY, "");
        private String model = LlmConfig.getEnv(LlmConfig.ENV_LLM_MODEL, LlmConfig.DEFAULT_OPENAI_MODEL);
        private String apiUrl = LlmConfig.getEnv(LlmConfig.ENV_LLM_BASE_URL, LlmConfig.DEFAULT_OPENAI_BASE_URL);
        private double temperature = LlmConfig.getEnvDouble(LlmConfig.ENV_LLM_TEMPERATURE, LlmConfig.DEFAULT_TEMPERATURE);
        private int maxTokens = LlmConfig.getEnvInt(LlmConfig.ENV_LLM_MAX_TOKENS, LlmConfig.DEFAULT_MAX_TOKENS);
        private boolean skipSslVerify = LlmConfig.getEnvBoolean(LlmConfig.ENV_LLM_SKIP_SSL_VERIFY, LlmConfig.DEFAULT_SKIP_SSL_VERIFY);
        private boolean debug = LlmConfig.getEnvBoolean(LlmConfig.ENV_LLM_DEBUG, LlmConfig.DEFAULT_DEBUG);

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder apiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
            return this;
        }

        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder skipSslVerify(boolean skipSslVerify) {
            this.skipSslVerify = skipSslVerify;
            return this;
        }

        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public OpenAiLlmClient build() {
            return new OpenAiLlmClient(apiKey, model, apiUrl, temperature, maxTokens, skipSslVerify, debug);
        }
    }
}

