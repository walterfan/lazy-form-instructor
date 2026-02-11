package com.fanyamin.instructor.api;

public record ValidationError(
    String path,
    String message,
    String type
) {}

