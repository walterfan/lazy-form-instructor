package com.fanyamin.workflow.node;

import com.fanyamin.workflow.core.NodeResult;
import com.fanyamin.workflow.core.WorkflowContext;
import com.fanyamin.workflow.core.WorkflowNode;

import java.util.function.Predicate;

/**
 * Node that makes deterministic decisions based on context.
 */
public class LogicDecisionNode implements WorkflowNode {
    private final String id;
    private final Predicate<WorkflowContext> condition;

    public LogicDecisionNode(String id, Predicate<WorkflowContext> condition) {
        this.id = id;
        this.condition = condition;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public NodeResult execute(WorkflowContext ctx) {
        boolean result = condition.test(ctx);
        return NodeResult.success(result, result ? "TRUE" : "FALSE");
    }
}



