package com.fanyamin.workflow.core;

/**
 * Base interface for all workflow nodes.
 */
public interface WorkflowNode {
    /**
     * Unique identifier for the node within the graph.
     */
    String getId();

    /**
     * Executes the node's logic.
     *
     * @param ctx The shared workflow context.
     * @return The result of the execution.
     */
    NodeResult execute(WorkflowContext ctx);
}



