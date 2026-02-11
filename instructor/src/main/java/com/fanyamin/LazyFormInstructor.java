package com.fanyamin;

import com.fanyamin.instructor.api.ParsingRequest;
import com.fanyamin.instructor.api.ParsingResult;
import com.fanyamin.instructor.api.ValidationError;
import com.fanyamin.instructor.llm.LlmClient;
import com.fanyamin.instructor.llm.PromptManager;
import com.fanyamin.instructor.schema.SchemaValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fanyamin.instructor.streaming.StreamingParseEvent;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LazyFormInstructor {

    private final LlmClient llmClient;
    private final PromptManager promptManager;
    private final SchemaValidator schemaValidator;
    private final ObjectMapper objectMapper;
    private final int maxRetries;

    public LazyFormInstructor(LlmClient llmClient) {
        this(llmClient, 3);
    }

    public LazyFormInstructor(LlmClient llmClient, int maxRetries) {
        this.llmClient = llmClient;
        this.maxRetries = maxRetries;
        this.promptManager = new PromptManager();
        this.schemaValidator = new SchemaValidator();
        this.objectMapper = new ObjectMapper();
    }

    public ParsingResult parse(ParsingRequest request) {
        String prompt = promptManager.generateSystemPrompt(request);
        String lastResponse = null;
        List<ValidationError> validationErrors = new ArrayList<>();

        for (int i = 0; i <= maxRetries; i++) {
            if (i > 0) {
                // Retry logic: append error context to prompt
                prompt = generateRetryPrompt(prompt, lastResponse, validationErrors);
            }

            String jsonResponse = llmClient.chat(prompt);
            lastResponse = jsonResponse;

            try {
                // 1. Parse LLM response to ParsingResult object
                // The LLM is instructed to return { fields: ..., errors: ... }
                ParsingResult result = objectMapper.readValue(jsonResponse, ParsingResult.class);

                // 2. Extract the "value" part to validate against strict JSON Schema
                // We need to construct a simplified JSON object of just values for validation
                String valueOnlyJson = extractValuesJson(result);

                // 3. Validate against the provided JSON Schema
                List<ValidationError> schemaErrors = schemaValidator.validate(request.schema(), valueOnlyJson);

                if (schemaErrors.isEmpty()) {
                    return result;
                } else {
                    validationErrors = schemaErrors;
                }

            } catch (JsonProcessingException e) {
                validationErrors = List.of(new ValidationError("root", "Invalid JSON format: " + e.getMessage(), "json_error"));
            }
        }

        // If retries exhausted, return the best effort (or failure) with errors
        return new ParsingResult(null, validationErrors);
    }

    /**
     * Streaming variant of {@link #parse(ParsingRequest)}.
     *
     * <p>Emits:
     * <ul>
     *   <li>RawChunk events as LLM text arrives</li>
     *   <li>Snapshot events when the buffered JSON becomes parseable as {@link ParsingResult}</li>
     *   <li>AttemptFailed events when schema validation fails for an attempt</li>
     *   <li>FinalResult event when schema validation passes (or retries are exhausted)</li>
     * </ul>
     *
     * <p>Schema validation is performed only on the final assembled JSON for each attempt.</p>
     */
    public Flux<StreamingParseEvent> parseStreaming(ParsingRequest request) {
        String basePrompt = promptManager.generateSystemPrompt(request);
        return Flux.defer(() -> attemptStreaming(request, 1, basePrompt, null, null));
    }

    private Flux<StreamingParseEvent> attemptStreaming(
            ParsingRequest originalRequest,
            int attempt,
            String prompt,
            String lastResponse,
            List<ValidationError> lastSchemaErrors
    ) {
        if (attempt > (maxRetries + 1)) {
            // Exhausted: return best-effort final result with last known schema errors.
            List<ValidationError> errs = lastSchemaErrors != null ? lastSchemaErrors : List.of();
            return Flux.just(new StreamingParseEvent.FinalResult(new ParsingResult(null, errs), errs, attempt - 1));
        }

        String effectivePrompt = prompt;
        if (attempt > 1 && lastSchemaErrors != null) {
            // Keep behavior aligned with parse(): append error context to prompt.
            effectivePrompt = generateRetryPrompt(prompt, lastResponse, lastSchemaErrors);
        }

        final StringBuilder buffer = new StringBuilder();
        final AtomicInteger lastSnapshotAtLen = new AtomicInteger(0);
        final int snapshotCharThreshold = 256;

        Flux<StreamingParseEvent> started = Flux.just(new StreamingParseEvent.AttemptStarted(attempt));

        Flux<StreamingParseEvent> stream = llmClient.streamChat(effectivePrompt)
                .flatMap(chunk -> {
                    buffer.append(chunk);
                    List<StreamingParseEvent> out = new ArrayList<>(2);
                    out.add(new StreamingParseEvent.RawChunk(chunk, attempt));

                    // Best-effort snapshot parsing (not schema validated)
                    if (buffer.length() - lastSnapshotAtLen.get() >= snapshotCharThreshold) {
                        try {
                            ParsingResult partial = objectMapper.readValue(buffer.toString(), ParsingResult.class);
                            out.add(new StreamingParseEvent.Snapshot(partial, attempt));
                            lastSnapshotAtLen.set(buffer.length());
                        } catch (Exception ignored) {
                            // Expected for partial JSON; no snapshot emitted.
                        }
                    }

                    return Flux.fromIterable(out);
                })
                .concatWith(Flux.defer(() -> {
                    // Attempt completion: parse full JSON and validate schema once.
                    ParsingResult parsed;
                    try {
                        parsed = objectMapper.readValue(buffer.toString(), ParsingResult.class);
                    } catch (Exception e) {
                        List<ValidationError> jsonErr = List.of(new ValidationError(
                                "root",
                                "Invalid JSON format: " + e.getMessage(),
                                "json_error"
                        ));
                        return Flux.concat(
                                Flux.just(new StreamingParseEvent.AttemptFailed(jsonErr, attempt)),
                                attemptStreaming(originalRequest, attempt + 1, prompt, buffer.toString(), jsonErr)
                        );
                    }

                    // Emit a final snapshot (parseable JSON) before validation/final result.
                    Flux<StreamingParseEvent> finalSnapshot = Flux.just(new StreamingParseEvent.Snapshot(parsed, attempt));

                    List<ValidationError> schemaErrors;
                    try {
                        String valueOnlyJson = extractValuesJson(parsed);
                        schemaErrors = schemaValidator.validate(originalRequest.schema(), valueOnlyJson);
                    } catch (Exception e) {
                        schemaErrors = List.of(new ValidationError("root", "Validation error: " + e.getMessage(), "validation_error"));
                    }

                    if (schemaErrors == null || schemaErrors.isEmpty()) {
                        return Flux.concat(finalSnapshot, Flux.just(new StreamingParseEvent.FinalResult(parsed, List.of(), attempt)));
                    }

                    return Flux.concat(
                            finalSnapshot,
                            Flux.just(new StreamingParseEvent.AttemptFailed(schemaErrors, attempt)),
                            attemptStreaming(originalRequest, attempt + 1, prompt, buffer.toString(), schemaErrors)
                    );
                }))
                .onErrorResume(e ->
                        Flux.just(new StreamingParseEvent.Error(e, attempt))
                );

        return Flux.concat(started, stream);
    }

    private String generateRetryPrompt(String originalPrompt, String lastResponse, List<ValidationError> errors) {
        StringBuilder sb = new StringBuilder(originalPrompt);
        sb.append("\n\n### PREVIOUS ATTEMPT FAILED\n");
        sb.append("Your previous response was invalid.\n");
        sb.append("Response: ").append(lastResponse).append("\n");
        sb.append("Errors:\n");
        for (ValidationError error : errors) {
            sb.append("- ").append(error.path()).append(": ").append(error.message()).append("\n");
        }
        sb.append("\nPlease fix these errors and return the valid JSON.");
        return sb.toString();
    }

    private String extractValuesJson(ParsingResult result) throws JsonProcessingException {
        // Create a simple Map<String, Object> where keys are field names and values are result.fields().get(key).value()
        // This is a simplification. For nested schemas, we'd need recursive extraction.
        // For MVP, assuming flat structure or basic map. 
        // NOTE: The JSON Schema validator needs the structure to match the schema.
        // If schema has "properties": {"name": ...}, valid JSON is {"name": "John"}
        
        if (result.fields() == null) return "{}";
        
        var valuesMap = result.fields().entrySet().stream()
                .filter(e -> e.getValue().value() != null)  // Filter out null values
                .collect(java.util.stream.Collectors.toMap(
                        java.util.Map.Entry::getKey,
                        e -> e.getValue().value()
                ));
        
        return objectMapper.writeValueAsString(valuesMap);
    }
}

