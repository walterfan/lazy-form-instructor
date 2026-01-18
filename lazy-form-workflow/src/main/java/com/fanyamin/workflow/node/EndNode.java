package com.fanyamin.workflow.node;

import com.fanyamin.workflow.core.NodeResult;
import com.fanyamin.workflow.core.WorkflowContext;
import com.fanyamin.workflow.core.WorkflowNode;

/**
 * Terminal node that marks the end of the workflow.
 */
public class EndNode implements WorkflowNode {
    private final String id;

    public EndNode(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public NodeResult execute(WorkflowContext ctx) {
        // EndNode marks completion
        return NodeResult.success("Completed");
    }
}



