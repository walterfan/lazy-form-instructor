package com.fanyamin.instructor.exception;

import com.fanyamin.instructor.api.ValidationError;
import java.util.Collections;
import java.util.List;

/**
 * Thrown when validation fails (schema or custom validation).
 */
public class ValidationFailedException extends InstructorException {

    private final List<ValidationError> errors;

    public ValidationFailedException(String message, List<ValidationError> errors) {
        super(message);
        this.errors = errors != null ? errors : Collections.emptyList();
    }

    public List<ValidationError> getErrors() {
        return errors;
    }
}


