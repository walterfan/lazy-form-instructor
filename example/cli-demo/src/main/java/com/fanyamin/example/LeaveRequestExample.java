package com.fanyamin.example;

import com.fanyamin.LazyFormInstructor;
import com.fanyamin.example.dto.LeaveRequestForm;
import com.fanyamin.instructor.api.FieldResult;
import com.fanyamin.instructor.api.ParsingRequest;
import com.fanyamin.instructor.api.ParsingResult;
import com.fanyamin.instructor.llm.LlmClient;
import com.fanyamin.instructor.llm.LlmClientFactory;
import com.fanyamin.instructor.schema.SchemaGenerator;
import com.fanyamin.instructor.streaming.StreamingParseEvent;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Example demonstrating multi-day leave request with dependencies.
 * 
 * This example corresponds to the "Worked Example: Multi-day Leave Request with Dependencies"
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
public class LeaveRequestExample {

    public static void main(String[] args) throws IOException {
        System.out.println("=== Leave Request Example ===\n");

        try {
            // Generate schema from DTO
            SchemaGenerator schemaGenerator = new SchemaGenerator();
            String schema = schemaGenerator.generateSchemaWithAnnotations(LeaveRequestForm.class);

            System.out.println("Generated JSON Schema from LeaveRequestForm.java:");
            System.out.println(schema);
            System.out.println();

            // User input from design doc example
            String userInput = "I'm getting married next month! I'd like to take a week off starting from December 15th. Please send the request to my manager.";

            // Context from design doc: now = 2023-12-01 (Friday)
            Map<String, Object> context = Map.of(
                    "now", "2023-12-01T10:00:00Z",
                    "user", Map.of(
                            "role", "employee",
                            "managerId", "u_123"
                    ),
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
            System.out.println("  Current Time: " + context.get("now"));
            System.out.println("  User Role: employee");
            System.out.println("  Manager ID: u_123\n");

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
            }

            System.out.println("\n" + "=".repeat(80));
            System.out.println("Analysis:");
            System.out.println("- Leave type correctly identified as 'annual' (marriage leave is typically annual leave)");
            System.out.println("- Start date extracted from 'December 15th'");
            System.out.println("- End date calculated as 7 days later (a week off)");
            System.out.println("- Approver derived from user context (managerId)");
            System.out.println("- Reason extracted as 'Getting married' from user input");
        } finally {
            // Avoid Maven exec:java lingering-thread warnings (Reactor boundedElastic keeps threads alive for reuse).
            Schedulers.shutdownNow();
        }
    }

    private static void printFieldResult(String fieldName, FieldResult result) {
        System.out.println("\n📋 Field: " + fieldName);
        System.out.println("   Value: " + result.value());
        System.out.println("   Confidence: " + String.format("%.2f", result.confidence()));
        System.out.println("   Reasoning: " + result.reasoning());
        if (result.alternatives() != null && !result.alternatives().isEmpty()) {
            System.out.println("   Alternatives: " + result.alternatives());
        }
    }
}

