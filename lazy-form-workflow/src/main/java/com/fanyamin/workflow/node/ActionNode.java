package com.fanyamin.workflow.node;

import com.fanyamin.workflow.core.NodeResult;
import com.fanyamin.workflow.core.WorkflowContext;
import com.fanyamin.workflow.core.WorkflowNode;

import java.util.function.Function;

/**
 * Node that executes deterministic, side-effectful work.
 */
public class ActionNode implements WorkflowNode {
    private final String id;
    private final Function<WorkflowContext, Object> action;

    public ActionNode(String id, Function<WorkflowContext, Object> action) {
        this.id = id;
        this.action = action;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public NodeResult execute(WorkflowContext ctx) {
        try {
            Object result = action.apply(ctx);
            return NodeResult.success(result);
        } catch (Exception e) {
            return NodeResult.failure(e.getMessage());
        }
    }
}



