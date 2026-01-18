package com.fanyamin.workflow.node;

import com.fanyamin.workflow.core.NodeResult;
import com.fanyamin.workflow.core.WorkflowContext;
import com.fanyamin.workflow.core.WorkflowNode;

/**
 * Entry point node that initializes the workflow.
 */
public class StartNode implements WorkflowNode {
    private final String id;

    public StartNode(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public NodeResult execute(WorkflowContext ctx) {
        // StartNode just passes control to the next node
        return NodeResult.success("Started");
    }
}



