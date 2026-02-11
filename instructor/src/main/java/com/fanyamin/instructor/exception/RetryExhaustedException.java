package com.fanyamin.instructor.exception;

import com.fanyamin.instructor.api.ValidationError;
import java.util.Collections;
import java.util.List;

/**
 * Thrown when the maximum number of retry attempts is exhausted.
 */
public class RetryExhaustedException extends InstructorException {

    private final int attempts;
    private final List<ValidationError> lastErrors;

    public RetryExhaustedException(String message, int attempts, List<ValidationError> lastErrors) {
        super(message);
        this.attempts = attempts;
        this.lastErrors = lastErrors != null ? lastErrors : Collections.emptyList();
    }

    public int getAttempts() {
        return attempts;
    }

    public List<ValidationError> getLastErrors() {
        return lastErrors;
    }
}


