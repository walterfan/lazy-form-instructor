package com.fanyamin.instructor.api;

import java.util.Map;

public record ParsingRequest(
    String schema, // JSON Schema string
    String userInput,
    Map<String, Object> context
) {}

