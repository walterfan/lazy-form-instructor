package com.fanyamin.workflow.core;

import java.util.function.BiPredicate;

/**
 * Directed connection between two nodes with optional condition.
 */
public class WorkflowEdge {
    private final String fromNodeId;
    private final String toNodeId;
    private final BiPredicate<WorkflowContext, NodeResult> condition;
    private final String label;
    private final int priority;

    public WorkflowEdge(String fromNodeId, String toNodeId) {
        this(fromNodeId, toNodeId, (ctx, res) -> true, "DEFAULT", 0);
    }

    public WorkflowEdge(String fromNodeId, String toNodeId, BiPredicate<WorkflowContext, NodeResult> condition) {
        this(fromNodeId, toNodeId, condition, "CONDITIONAL", 0);
    }

    public WorkflowEdge(String fromNodeId, String toNodeId, BiPredicate<WorkflowContext, NodeResult> condition, String label, int priority) {
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.condition = condition;
        this.label = label;
        this.priority = priority;
    }

    public String getFromNodeId() {
        return fromNodeId;
    }

    public String getToNodeId() {
        return toNodeId;
    }

    public boolean canTraverse(WorkflowContext ctx, NodeResult result) {
        return condition.test(ctx, result);
    }

    public String getLabel() {
        return label;
    }

    public int getPriority() {
        return priority;
    }
}



