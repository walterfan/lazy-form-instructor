package com.fanyamin.instructor.api;

import java.util.List;

public record FieldResult(
    Object value,
    double confidence,
    String reasoning,
    List<Object> alternatives
) {}

