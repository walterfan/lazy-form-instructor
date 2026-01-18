package com.fanyamin.workflow.core;

/**
 * Exception thrown when workflow definition is invalid.
 */
public class WorkflowDefinitionException extends RuntimeException {
    public WorkflowDefinitionException(String message) {
        super(message);
    }

    public WorkflowDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
}



