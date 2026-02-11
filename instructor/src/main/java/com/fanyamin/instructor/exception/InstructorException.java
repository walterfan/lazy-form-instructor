package com.fanyamin.instructor.exception;

/**
 * Base exception for all Instructor-related errors.
 */
public class InstructorException extends RuntimeException {

    public InstructorException(String message) {
        super(message);
    }

    public InstructorException(String message, Throwable cause) {
        super(message, cause);
    }
}


