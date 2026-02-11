package com.fanyamin.instructor.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

/**
 * Spring AI LLM client implementation.
 * Uses Spring AI's ChatClient which provides a unified interface across multiple LLM providers.
 * 
 * Supports OpenAI, Anthropic, Azure OpenAI, Ollama, and more.
 * 
 * Includes support for:
 * - Thinking mode extraction (filtering reasoning content)
 * - Debug logging (when enabled via constructor or setDebug)
 */
public class SpringAiLlmClient implements LlmClient {

    private static final Logger logger = LoggerFactory.getLogger(SpringAiLlmClient.class);

    private final ChatClient chatClient;
    private boolean debug;

    /**
     * Create client with a pre-configured Spring AI ChatClient.
     * The ChatClient should be injected via Spring dependency injection.
     */
    public SpringAiLlmClient(ChatClient chatClient) {
        this(chatClient, false);
    }

    public SpringAiLlmClient(ChatClient chatClient, boolean debug) {
        this.chatClient = chatClient;
        this.debug = debug;
        
        if (debug) {
            setLogLevel(ch.qos.logback.classic.Level.DEBUG);
            logger.info("🐛 DEBUG MODE: Request/Response logging enabled (Spring AI)");
        }
    }
    
    public void setDebug(boolean debug) {
        this.debug = debug;
        if (debug) {
            setLogLevel(ch.qos.logback.classic.Level.DEBUG);
            logger.info("🐛 DEBUG MODE: Request/Response logging enabled (Spring AI)");
        }
    }

    @Override
    public String chat(String promptText) {
        long startTime = System.currentTimeMillis();
        
        if (debug) {
            logger.debug("\n{}", "=".repeat(80));
            logger.debug("🔵 SPRING AI REQUEST");
            logger.debug("{}", "=".repeat(80));
            logger.debug("Prompt:");
            logger.debug("{}", promptText);
            logger.debug("{}\n", "=".repeat(80));
        }

        try {
            // Use Spring AI's fluent API
            String content = chatClient.prompt()
                    .user(promptText)
                .call()
                .content();
            
            long duration = System.currentTimeMillis() - startTime;

            if (debug) {
                logger.debug("🔵 SPRING AI RESPONSE");
                logger.debug("{}", "=".repeat(80));
                logger.debug("Duration: {}ms", duration);
                
                // Note: Spring AI abstraction might hide headers/status, so we log content
                logger.debug("\nRaw Content:");
                logger.debug("{}", content);
                logger.debug("{}\n", "=".repeat(80));
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
            
        } catch (Exception e) {
            String errorMsg = "Spring AI error: " + e.getMessage();
            logger.error("❌ {}", errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    @Override
    public Flux<String> streamChat(String promptText) {
        // Minimal, always-correct fallback: emit once with the full response.
        // If/when Spring AI streaming API is wired in, this can be upgraded to emit chunks.
        return Flux.defer(() -> Flux.just(chat(promptText)));
    }

    @Override
    public boolean supportsStreaming() {
        // Current implementation emits a single chunk (fallback).
        // Return false until we implement true chunked streaming via Spring AI.
        return false;
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
}
