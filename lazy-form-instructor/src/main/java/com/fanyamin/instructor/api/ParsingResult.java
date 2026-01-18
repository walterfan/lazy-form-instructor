package com.fanyamin.instructor.api;

import java.util.Map;
import java.util.List;

public record ParsingResult(
    Map<String, FieldResult> fields,
    List<ValidationError> errors
) {}
