package com.fanyamin.instructor.schema;

import com.fanyamin.instructor.api.ValidationError;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SchemaValidator {

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;

    public SchemaValidator() {
        this.objectMapper = new ObjectMapper();
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    public List<ValidationError> validate(String schemaJson, String instanceJson) {
        try {
            JsonNode schemaNode = objectMapper.readTree(schemaJson);
            JsonNode instanceNode = objectMapper.readTree(instanceJson);

            JsonSchema schema = schemaFactory.getSchema(schemaNode);
            Set<ValidationMessage> messages = schema.validate(instanceNode);

            if (messages.isEmpty()) {
                return Collections.emptyList();
            }

            return messages.stream()
                .map(msg -> new ValidationError(
                    msg.getMessage(), // Using message as path for now if path is missing
                    msg.getMessage(),
                    msg.getType()
                ))
                .collect(Collectors.toList());

        } catch (Exception e) {
            return List.of(new ValidationError("$", "Malformed JSON: " + e.getMessage(), "parsing_error"));
        }
    }
}
