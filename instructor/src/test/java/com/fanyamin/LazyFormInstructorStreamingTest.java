package com.fanyamin;

import com.fanyamin.instructor.api.ParsingRequest;
import com.fanyamin.instructor.api.ParsingResult;
import com.fanyamin.instructor.llm.LlmClient;
import com.fanyamin.instructor.streaming.StreamingParseEvent;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LazyFormInstructorStreamingTest {

    @Test
    void streamingEmitsRawChunksAndFinalResult_success() {
        String schema = """
            {
              "type": "object",
              "properties": {
                "name": { "type": "string" },
                "age": { "type": "integer" }
              },
              "required": ["name", "age"]
            }
            """;

        String json = """
            {
              "fields": {
                "name": { "value": "Alice", "confidence": 0.95, "reasoning": "Explicit", "alternatives": [] },
                "age":  { "value": 25, "confidence": 0.99, "reasoning": "Explicit", "alternatives": [] }
              },
              "errors": []
            }
            """;

        List<String> chunks = chunk(json, 40);

        LlmClient streamingClient = new LlmClient() {
            @Override
            public String chat(String prompt) {
                return json;
            }

            @Override
            public Flux<String> streamChat(String prompt) {
                return Flux.fromIterable(chunks);
            }

            @Override
            public boolean supportsStreaming() {
                return true;
            }
        };

        LazyFormInstructor instructor = new LazyFormInstructor(streamingClient, 0);
        ParsingRequest request = new ParsingRequest(schema, "Alice is 25", Map.of());

        List<StreamingParseEvent> events = instructor.parseStreaming(request).collectList().block();
        assertNotNull(events);
        assertFalse(events.isEmpty());

        long rawCount = events.stream().filter(e -> e instanceof StreamingParseEvent.RawChunk).count();
        assertEquals(chunks.size(), rawCount);

        StreamingParseEvent.FinalResult finalEvt = events.stream()
                .filter(e -> e instanceof StreamingParseEvent.FinalResult)
                .map(e -> (StreamingParseEvent.FinalResult) e)
                .findFirst()
                .orElseThrow();

        assertTrue(finalEvt.schemaErrors().isEmpty());
        ParsingResult result = finalEvt.result();
        assertNotNull(result.fields());
        assertEquals("Alice", result.fields().get("name").value());
        assertEquals(25, result.fields().get("age").value());
    }

    @Test
    void streamingFinalResultContainsSchemaErrors_whenInvalid() {
        String schema = """
            {
              "type": "object",
              "properties": {
                "age": { "type": "integer" }
              },
              "required": ["age"]
            }
            """;

        // age is string, schema requires integer
        String json = """
            {
              "fields": {
                "age": { "value": "twenty", "confidence": 0.5, "reasoning": "Guess", "alternatives": [] }
              },
              "errors": []
            }
            """;

        LlmClient client = new LlmClient() {
            @Override
            public String chat(String prompt) {
                return json;
            }

            @Override
            public Flux<String> streamChat(String prompt) {
                return Flux.just(json);
            }
        };

        LazyFormInstructor instructor = new LazyFormInstructor(client, 0);
        ParsingRequest request = new ParsingRequest(schema, "age twenty", Map.of());

        List<StreamingParseEvent> events = instructor.parseStreaming(request).collectList().block();
        assertNotNull(events);

        StreamingParseEvent.FinalResult finalEvt = events.stream()
                .filter(e -> e instanceof StreamingParseEvent.FinalResult)
                .map(e -> (StreamingParseEvent.FinalResult) e)
                .findFirst()
                .orElseThrow();

        assertFalse(finalEvt.schemaErrors().isEmpty());
    }

    @Test
    void streamingRetriesAndSucceeds_onSecondAttempt() {
        String schema = """
            {
              "type": "object",
              "properties": {
                "age": { "type": "integer" }
              },
              "required": ["age"]
            }
            """;

        String invalid = """
            { "fields": { "age": { "value": "bad", "confidence": 0.2, "reasoning": "bad", "alternatives": [] } }, "errors": [] }
            """;
        String valid = """
            { "fields": { "age": { "value": 30, "confidence": 0.9, "reasoning": "ok", "alternatives": [] } }, "errors": [] }
            """;

        LlmClient retryingClient = new LlmClient() {
            @Override
            public String chat(String prompt) {
                return prompt.contains("PREVIOUS ATTEMPT FAILED") ? valid : invalid;
            }

            @Override
            public Flux<String> streamChat(String prompt) {
                return Flux.just(chat(prompt));
            }
        };

        LazyFormInstructor instructor = new LazyFormInstructor(retryingClient, 1);
        ParsingRequest request = new ParsingRequest(schema, "age thirty", Map.of());

        List<StreamingParseEvent> events = instructor.parseStreaming(request).collectList().block();
        assertNotNull(events);

        long failedAttempts = events.stream().filter(e -> e instanceof StreamingParseEvent.AttemptFailed).count();
        assertEquals(1, failedAttempts);

        StreamingParseEvent.FinalResult finalEvt = events.stream()
                .filter(e -> e instanceof StreamingParseEvent.FinalResult)
                .map(e -> (StreamingParseEvent.FinalResult) e)
                .findFirst()
                .orElseThrow();

        assertTrue(finalEvt.schemaErrors().isEmpty());
        assertEquals(30, finalEvt.result().fields().get("age").value());
    }

    private static List<String> chunk(String s, int size) {
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            out.add(s.substring(i, Math.min(s.length(), i + size)));
            i += size;
        }
        return out;
    }
}



