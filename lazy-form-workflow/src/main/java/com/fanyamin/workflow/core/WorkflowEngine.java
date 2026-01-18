package com.fanyamin.workflow.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Executes a workflow graph starting from a given node.
 */
public class WorkflowEngine {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowEngine.class);
    
    private final WorkflowGraph graph;
    private final int maxIterations;

    public WorkflowEngine(WorkflowGraph graph) {
        this(graph, 100); // Default max iterations to prevent infinite loops
    }

    public WorkflowEngine(WorkflowGraph graph, int maxIterations) {
        this.graph = graph;
        this.maxIterations = maxIterations;
        // Validate graph on construction
        graph.validate();
    }

    /**
     * Execute the workflow starting from the given node ID.
     */
    public WorkflowResult execute(String startNodeId, WorkflowContext context) {
        logger.info("Starting workflow execution from node: {}", startNodeId);
        
        WorkflowNode currentNode = graph.getNode(startNodeId);
        if (currentNode == null) {
            throw new WorkflowDefinitionException("Start node not found: " + startNodeId);
        }

        int iterations = 0;
        while (currentNode != null && iterations < maxIterations) {
            iterations++;
            
            String nodeId = currentNode.getId();
            logger.debug("Executing node: {}", nodeId);
            
            try {
                // Execute the node
                NodeResult result = currentNode.execute(context);
                
                // Log the execution
                context.appendTrace(new TraceEntry(
                    nodeId,
                    currentNode.getClass().getSimpleName(),
                    null, // inputSummary can be added if needed
                    result.payload(),
                    result.status()
                ));
                
                // Check for terminal states
                if (result.status() == NodeStatus.FAILURE) {
                    logger.error("Node {} failed: {}", nodeId, result.payload());
                    return WorkflowResult.failure((String) result.payload(), context);
                }
                
                if (result.status() == NodeStatus.WAITING) {
                    logger.info("Node {} is waiting for external input", nodeId);
                    return WorkflowResult.waiting(nodeId, context);
                }
                
                // Find next node
                currentNode = findNextNode(nodeId, context, result);
                
            } catch (Exception e) {
                logger.error("Error executing node {}: {}", nodeId, e.getMessage(), e);
                context.appendTrace(new TraceEntry(nodeId, currentNode.getClass().getSimpleName(), null, e.getMessage()));
                return WorkflowResult.failure("Error in node " + nodeId + ": " + e.getMessage(), context);
            }
        }
        
        if (iterations >= maxIterations) {
            return WorkflowResult.failure("Max iterations reached (" + maxIterations + ")", context);
        }
        
        return WorkflowResult.success(context);
    }

    private WorkflowNode findNextNode(String currentNodeId, WorkflowContext context, NodeResult result) {
        List<WorkflowEdge> outgoingEdges = graph.getOutgoingEdges(currentNodeId);
        
        if (outgoingEdges.isEmpty()) {
            logger.debug("No outgoing edges from node {}, workflow complete", currentNodeId);
            return null; // End of workflow
        }
        
        // Find the first edge whose condition is satisfied
        for (WorkflowEdge edge : outgoingEdges) {
            if (edge.canTraverse(context, result)) {
                logger.debug("Taking edge from {} to {} (label: {})", 
                    currentNodeId, edge.getToNodeId(), edge.getLabel());
                return graph.getNode(edge.getToNodeId());
            }
        }
        
        logger.warn("No edge condition satisfied for node {}", currentNodeId);
        return null; // No condition matched, end workflow
    }
}



