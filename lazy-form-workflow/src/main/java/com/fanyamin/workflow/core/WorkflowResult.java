package com.fanyamin.workflow.core;

/**
 * Result of a workflow execution.
 */
public record WorkflowResult(
    WorkflowStatus status,
    String message,
    String pausedAtNode,
    WorkflowContext context
) {
    public static WorkflowResult success(WorkflowContext context) {
        return new WorkflowResult(WorkflowStatus.COMPLETED, "Workflow completed successfully", null, context);
    }

    public static WorkflowResult failure(String message, WorkflowContext context) {
        return new WorkflowResult(WorkflowStatus.FAILED, message, null, context);
    }

    public static WorkflowResult waiting(String pausedAtNode, WorkflowContext context) {
        return new WorkflowResult(WorkflowStatus.WAITING, "Waiting for external input", pausedAtNode, context);
    }
}



