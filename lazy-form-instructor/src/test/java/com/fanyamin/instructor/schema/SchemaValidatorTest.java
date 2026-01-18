package com.fanyamin.instructor.schema;

import org.junit.jupiter.api.Test;

import com.fanyamin.instructor.api.ValidationError;
import com.fanyamin.instructor.schema.SchemaValidator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SchemaValidatorTest {

    private final SchemaValidator validator = new SchemaValidator();

    @Test
    void testValidJson() {
        String schema = """
            {
              "type": "object",
              "properties": {
                "name": { "type": "string" },
                "age": { "type": "integer" }
              },
              "required": ["name"]
            }
            """;
        String instance = """
            {
              "name": "John",
              "age": 30
            }
            """;

        List<ValidationError> errors = validator.validate(schema, instance);
        assertTrue(errors.isEmpty());
    }

    @Test
    void testInvalidJson() {
        String schema = """
            {
              "type": "object",
              "properties": {
                "age": { "type": "integer" }
              }
            }
            """;
        String instance = """
            {
              "age": "thirty"
            }
            """;

        List<ValidationError> errors = validator.validate(schema, instance);
        assertFalse(errors.isEmpty());
        assertEquals("type", errors.get(0).type());
    }
}

