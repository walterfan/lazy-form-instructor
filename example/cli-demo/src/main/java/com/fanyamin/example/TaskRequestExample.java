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
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Example demonstrating task creation with dependencies.
 * 
 * This example corresponds to the "Worked Example: New Task Request with Dependencies"
 * in the design document.
 * 
 * Configuration:
 * - Set LLM_API_KEY environment variable or in .env file to use real LLM
 * - Otherwise, falls back to mock LLM client with predefined response
 * 
 * For real LLM usage:
 *   export LLM_API_KEY="sk-..."
 *   export LLM_MODEL="gpt-4-turbo-preview"
 * 
 * Or create .env file:
 *   LLM_API_KEY=sk-...
 *   LLM_MODEL=gpt-4-turbo-preview
 */
public class TaskRequestExample {

    public static void main(String[] args) throws IOException {
        System.out.println("=== Task Request Example ===\n");

        try {
            // Load schema from resources
            String schema = loadResourceFile("task-request-schema.json");

            // User input from design doc example
            String userInput = "tomorrow is monday, I have to finish the sending alert feature ASAP, the release date is next friday";

            // Context from design doc: now = 2023-10-27 (Friday)
            Map<String, Object> context = Map.of(
                    "now", "2023-10-27T10:00:00Z",
                    "locale", "en-US"
            );

            // Create LLM client from environment
            LlmClient llmClient = LlmClientFactory.createFromEnvironment();

            // Initialize instructor
            LazyFormInstructor instructor = new LazyFormInstructor(llmClient);

            // Parse the request
            ParsingRequest request = new ParsingRequest(schema, userInput, context);
            System.out.println("Streaming LLM output (raw JSON):");
            System.out.println("-".repeat(80));

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
            System.out.println("-".repeat(80));
            if (!schemaErrors.isEmpty()) {
                System.out.println("Final schema validation errors (" + schemaErrors.size() + "):");
                schemaErrors.forEach(error -> {
                    System.out.println("  - " + error.path() + ": " + error.message() + " (" + error.type() + ")");
                });
            } else {
                System.out.println("Final schema validation: OK");
            }

        // Display results
        System.out.println("User Input:");
        System.out.println("  \"" + userInput + "\"\n");

        System.out.println("Context:");
        System.out.println("  Current Time: " + context.get("now") + " (Friday)");
        System.out.println("  Locale: " + context.get("locale") + "\n");

        System.out.println("Parsing Results:");
        System.out.println("-".repeat(80));

        if (result.fields() != null) {
            result.fields().forEach((fieldName, fieldResult) -> {
                printFieldResult(fieldName, fieldResult);
            });
        }

        if (!result.errors().isEmpty()) {
            System.out.println("\nValidation Errors:");
            result.errors().forEach(error -> {
                System.out.println("  ❌ " + error.path() + ": " + error.message());
                System.out.println("     Type: " + error.type());
            });
        } else {
            System.out.println("\n✅ No validation errors");
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("Analysis:");
        System.out.println("- Task name extracted from 'sending alert feature'");
        System.out.println("- High priority inferred from 'ASAP' keyword");
        System.out.println("- Schedule time calculated from 'tomorrow is monday' relative to context");
        System.out.println("- Deadline calculated from 'next friday' (release date)");
        System.out.println("- Minutes estimate has low confidence - recommend asking user");
        System.out.println("- Alternative minutes values (120, 180) suggest 2-3 hour estimates");
        } finally {
            // Avoid Maven exec:java lingering-thread warnings (Reactor boundedElastic keeps threads alive for reuse).
            Schedulers.shutdownNow();
        }
    }

    private static void printFieldResult(String fieldName, FieldResult result) {
        System.out.println("\n📋 Field: " + fieldName);
        System.out.println("   Value: " + result.value());
        
        String confidenceIcon = result.confidence() >= 0.9 ? "🟢" : 
                               result.confidence() >= 0.7 ? "🟡" : "🔴";
        System.out.println("   Confidence: " + confidenceIcon + " " + String.format("%.2f", result.confidence()));
        System.out.println("   Reasoning: " + result.reasoning());
        
        if (result.alternatives() != null && !result.alternatives().isEmpty()) {
            System.out.println("   Alternatives: " + result.alternatives());
        }
    }

    private static String loadResourceFile(String filename) throws IOException {
        try (InputStream inputStream = TaskRequestExample.class.getClassLoader().getResourceAsStream(filename)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + filename);
            }
            return new String(inputStream.readAllBytes());
        }
    }
}

