package com.fanyamin.workflow.core;

import java.time.Instant;

/**
 * Represents a single execution of a node for traceability.
 */
public record TraceEntry(
    String nodeId,
    String nodeType,
    Instant timestamp,
    Object inputSummary,
    Object outputSummary,
    NodeStatus status,
    String error
) {
    public TraceEntry(String nodeId, String nodeType, Object inputSummary, Object outputSummary, NodeStatus status) {
        this(nodeId, nodeType, Instant.now(), inputSummary, outputSummary, status, null);
    }

    public TraceEntry(String nodeId, String nodeType, Object inputSummary, String error) {
        this(nodeId, nodeType, Instant.now(), inputSummary, null, NodeStatus.FAILURE, error);
    }
}



