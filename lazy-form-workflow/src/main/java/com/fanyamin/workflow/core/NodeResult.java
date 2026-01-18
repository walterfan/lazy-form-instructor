package com.fanyamin.workflow.core;

/**
 * Represents the result of a node execution.
 */
public record NodeResult(
    NodeStatus status,
    Object payload,
    String nextEdgeHint
) {
    public static NodeResult success(Object payload) {
        return new NodeResult(NodeStatus.SUCCESS, payload, null);
    }
    
    public static NodeResult success(Object payload, String nextEdgeHint) {
        return new NodeResult(NodeStatus.SUCCESS, payload, nextEdgeHint);
    }

    public static NodeResult failure(String message) {
        return new NodeResult(NodeStatus.FAILURE, message, null);
    }

    public static NodeResult waiting() {
        return new NodeResult(NodeStatus.WAITING, null, null);
    }
}



