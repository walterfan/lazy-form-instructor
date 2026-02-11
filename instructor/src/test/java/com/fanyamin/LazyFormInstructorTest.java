package com.fanyamin;

import org.junit.jupiter.api.Test;

import com.fanyamin.instructor.api.ParsingRequest;
import com.fanyamin.instructor.api.ParsingResult;
import com.fanyamin.instructor.llm.MockLlmClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LazyFormInstructorTest {

    @Test
    void testHappyPath() {
        MockLlmClient mockLlm = new MockLlmClient();
        mockLlm.setMockResponse("""
            {
              "fields": {
                "name": {
                  "value": "Alice",
                  "confidence": 0.95,
                  "reasoning": "Explicitly mentioned",
                  "alternatives": []
                },
                "age": {
                  "value": 25,
                  "confidence": 0.99,
                  "reasoning": "Explicitly mentioned",
                  "alternatives": []
                }
              },
              "errors": []
            }
            """);

        LazyFormInstructor instructor = new LazyFormInstructor(mockLlm);
        
        String schema = """
            {
              "type": "object",
              "properties": {
                "name": { "type": "string" },
                "age": { "type": "integer" }
              }
            }
            """;
            
        ParsingRequest request = new ParsingRequest(schema, "Alice is 25", Map.of());
        ParsingResult result = instructor.parse(request);

        assertNotNull(result);
        assertEquals("Alice", result.fields().get("name").value());
        assertEquals(25, result.fields().get("age").value());
        assertTrue(result.errors().isEmpty());
    }
}

