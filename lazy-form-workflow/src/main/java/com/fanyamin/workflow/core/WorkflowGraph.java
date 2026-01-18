package com.fanyamin.workflow.core;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a Directed Acyclic Graph (DAG) of workflow nodes and edges.
 */
public class WorkflowGraph {
    private final Map<String, WorkflowNode> nodes;
    private final List<WorkflowEdge> edges;

    public WorkflowGraph() {
        this.nodes = new HashMap<>();
        this.edges = new ArrayList<>();
    }

    public void addNode(WorkflowNode node) {
        if (nodes.containsKey(node.getId())) {
            throw new IllegalArgumentException("Node with ID " + node.getId() + " already exists");
        }
        nodes.put(node.getId(), node);
    }

    public void addEdge(WorkflowEdge edge) {
        if (!nodes.containsKey(edge.getFromNodeId())) {
            throw new IllegalArgumentException("From node " + edge.getFromNodeId() + " does not exist");
        }
        if (!nodes.containsKey(edge.getToNodeId())) {
            throw new IllegalArgumentException("To node " + edge.getToNodeId() + " does not exist");
        }
        edges.add(edge);
    }

    public WorkflowNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public List<WorkflowEdge> getOutgoingEdges(String nodeId) {
        return edges.stream()
                .filter(edge -> edge.getFromNodeId().equals(nodeId))
                .sorted(Comparator.comparingInt(WorkflowEdge::getPriority).reversed())
                .collect(Collectors.toList());
    }

    public Collection<WorkflowNode> getAllNodes() {
        return nodes.values();
    }

    public List<WorkflowEdge> getAllEdges() {
        return new ArrayList<>(edges);
    }

    /**
     * Validate the graph for cycles using DFS.
     * @throws WorkflowDefinitionException if a cycle is detected.
     */
    public void validate() {
        Set<String> visited = new HashSet<>();
        Set<String> recStack = new HashSet<>();

        for (String nodeId : nodes.keySet()) {
            if (hasCycle(nodeId, visited, recStack)) {
                throw new WorkflowDefinitionException("Cycle detected in workflow graph");
            }
        }
    }

    private boolean hasCycle(String nodeId, Set<String> visited, Set<String> recStack) {
        if (recStack.contains(nodeId)) {
            return true; // Cycle detected
        }
        if (visited.contains(nodeId)) {
            return false; // Already processed
        }

        visited.add(nodeId);
        recStack.add(nodeId);

        for (WorkflowEdge edge : getOutgoingEdges(nodeId)) {
            if (hasCycle(edge.getToNodeId(), visited, recStack)) {
                return true;
            }
        }

        recStack.remove(nodeId);
        return false;
    }
}



