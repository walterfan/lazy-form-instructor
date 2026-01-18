package com.fanyamin.instructor.exception;

/**
 * Thrown when the LLM output is incomplete (e.g., truncated due to token limit).
 */
public class IncompleteOutputException extends InstructorException {

    private final String partialResponse;

    public IncompleteOutputException(String message, String partialResponse) {
        super(message);
        this.partialResponse = partialResponse;
    }

    public String getPartialResponse() {
        return partialResponse;
    }
}


