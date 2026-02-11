package com.fanyamin.example;

import com.fanyamin.LazyFormInstructor;
import com.fanyamin.instructor.api.FieldResult;
import com.fanyamin.instructor.api.ParsingRequest;
import com.fanyamin.instructor.api.ParsingResult;
import com.fanyamin.instructor.llm.LlmClient;
import com.fanyamin.instructor.llm.LlmClientFactory;
import com.fanyamin.instructor.streaming.StreamingParseEvent;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Example using real LLM API configured via environment variables.
 * 
 * Configuration:
 * For OpenAI:
 *   export LLM_API_KEY="sk-..."
 *   export LLM_MODEL="gpt-4-turbo-preview"
 * 
 * For Ollama (local):
 *   export LLM_BASE_URL="http://localhost:11434/v1/chat/completions"
 *   export LLM_MODEL="llama2"
 * 
 * Run:
 *   mvn exec:java -Dexec.mainClass="com.fanyamin.example.RealLlmExample"
 */
public class RealLlmExample {

    public static void main(String[] args) {
        System.out.println("=== Real LLM API Example ===\n");

        try {
        try {
            // Check environment configuration
            printConfiguration();

            // Create LLM client from environment variables
            System.out.println("\n📡 Creating LLM client from environment variables...");
            LlmClient llmClient = LlmClientFactory.createFromEnvironment();
            System.out.println("✓ LLM client created successfully\n");

            // Create LazyFormInstructor
            LazyFormInstructor instructor = new LazyFormInstructor(llmClient);

            // Load schema
            String schema = loadResourceFile("leave-request-schema.json");

            // Example user input
            String userInput = "I'm sick and need 3 days off starting tomorrow. Please send to my manager.";

            // Context
            Map<String, Object> context = Map.of(
                    "now", "2023-12-08T10:00:00Z",
                    "user", Map.of(
                            "role", "employee",
                            "managerId", "u_manager_123"
                    )
            );

            System.out.println("User Input:");
            System.out.println("  \"" + userInput + "\"\n");

            System.out.println("Context:");
            System.out.println("  Current Time: " + context.get("now"));
            System.out.println("  User Role: employee");
            System.out.println("  Manager ID: u_manager_123\n");

            System.out.println("🤖 Calling LLM API...\n");

            // Parse the request
            ParsingRequest request = new ParsingRequest(schema, userInput, context);
            System.out.println("Streaming LLM output (raw JSON):");
            System.out.println("─".repeat(80));

            List<StreamingParseEvent> events = instructor.parseStreaming(request)
                    .doOnNext(evt -> {
                        if (evt instanceof StreamingParseEvent.RawChunk c) {
                            System.out.print(c.chunk());
                        } else if (evt instanceof StreamingParseEvent.AttemptFailed f) {
                            System.out.println("\n\nAttempt " + f.attempt() + " failed schema validation (" + f.schemaErrors().size() + " errors). Retrying...\n");
                        }
                    })
                    .collectList()
                    .block();

            ParsingResult result = events.stream()
                    .filter(e -> e instanceof StreamingParseEvent.FinalResult)
                    .map(e -> ((StreamingParseEvent.FinalResult) e).result())
                    .reduce((a, b) -> b)
                    .orElseThrow(() -> new IllegalStateException("No FinalResult emitted"));

            List<com.fanyamin.instructor.api.ValidationError> schemaErrors = events.stream()
                    .filter(e -> e instanceof StreamingParseEvent.FinalResult)
                    .map(e -> ((StreamingParseEvent.FinalResult) e).schemaErrors())
                    .reduce((a, b) -> b)
                    .orElse(List.of());

            System.out.println();
            System.out.println("─".repeat(80));
            if (!schemaErrors.isEmpty()) {
                System.out.println("Final schema validation errors (" + schemaErrors.size() + "):");
                schemaErrors.forEach(error ->
                        System.out.println("  - " + error.path() + ": " + error.message() + " (" + error.type() + ")")
                );
            } else {
                System.out.println("Final schema validation: OK\n");
            }

            // Display results
            System.out.println("Parsing Results:");
            System.out.println("─".repeat(80) + "\n");

            if (result.fields() != null && !result.fields().isEmpty()) {
                for (Map.Entry<String, FieldResult> entry : result.fields().entrySet()) {
                    FieldResult field = entry.getValue();
                    System.out.println("📋 Field: " + entry.getKey());
                    System.out.println("   Value: " + field.value());
                    System.out.println("   Confidence: " + getConfidenceEmoji(field.confidence()) + " " + field.confidence());
                    System.out.println("   Reasoning: " + field.reasoning());
                    if (field.alternatives() != null && !field.alternatives().isEmpty()) {
                        System.out.println("   Alternatives: " + field.alternatives());
                    }
                    System.out.println();
                }
            }

            if (result.errors() != null && !result.errors().isEmpty()) {
                System.out.println("Validation Errors:");
                result.errors().forEach(error ->
                        System.out.println("  ❌ " + error.path() + ": " + error.message() + " (" + error.type() + ")")
                );
            } else {
                System.out.println("✅ No validation errors\n");
            }

            System.out.println("═".repeat(80));
            System.out.println("✓ Example completed successfully!");

        } catch (RuntimeException e) {
            System.err.println("\n❌ Error: " + e.getMessage());
            System.err.println("\nPlease ensure you have configured the LLM client:");
            System.err.println("  For OpenAI:");
            System.err.println("    export LLM_API_KEY=\"sk-...\"");
            System.err.println("    export LLM_MODEL=\"gpt-4-turbo-preview\"");
            System.err.println("\n  For Ollama (local):");
            System.err.println("    export LLM_BASE_URL=\"http://localhost:11434/v1/chat/completions\"");
            System.err.println("    export LLM_MODEL=\"llama2\"");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("\n❌ Failed to load schema: " + e.getMessage());
            System.exit(1);
        }
        } finally {
            // Avoid Maven exec:java lingering-thread warnings (Reactor boundedElastic keeps threads alive for reuse).
            Schedulers.shutdownNow();
        }
    }

    private static void printConfiguration() {
        System.out.println("Configuration:");
        System.out.println("─".repeat(80));
        
        String apiKey = System.getenv("LLM_API_KEY");
        String baseUrl = System.getenv("LLM_BASE_URL");
        String model = System.getenv("LLM_MODEL");
        String temperature = System.getenv("LLM_TEMPERATURE");
        String maxTokens = System.getenv("LLM_MAX_TOKENS");

        System.out.println("  LLM_API_KEY: " + (apiKey != null ? maskApiKey(apiKey) : "(not set - ok for local models)"));
        System.out.println("  LLM_BASE_URL: " + (baseUrl != null ? baseUrl : "(using default: OpenAI)"));
        System.out.println("  LLM_MODEL: " + (model != null ? model : "(using default: gpt-4-turbo-preview)"));
        System.out.println("  LLM_TEMPERATURE: " + (temperature != null ? temperature : "(using default: 0.7)"));
        System.out.println("  LLM_MAX_TOKENS: " + (maxTokens != null ? maxTokens : "(using default: 4096)"));
        System.out.println("─".repeat(80));
    }

    private static String maskApiKey(String apiKey) {
        if (apiKey.length() <= 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
    }

    private static String getConfidenceEmoji(double confidence) {
        if (confidence >= 0.9) return "🟢";
        if (confidence >= 0.7) return "🟡";
        return "🔴";
    }

    private static String loadResourceFile(String filename) throws IOException {
        try (var inputStream = RealLlmExample.class.getClassLoader().getResourceAsStream(filename)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + filename);
            }
            return new String(inputStream.readAllBytes());
        }
    }
}

