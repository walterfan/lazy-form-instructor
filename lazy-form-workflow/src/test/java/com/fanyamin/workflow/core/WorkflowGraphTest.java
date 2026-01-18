package com.fanyamin.workflow.core;

import com.fanyamin.instructor.api.FieldResult;
import com.fanyamin.instructor.api.ParsingResult;
import com.fanyamin.workflow.node.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowGraphTest {

    @Test
    void testCycleDetection() {
        WorkflowGraph graph = new WorkflowGraph();
        
        StartNode start = new StartNode("start");
        ActionNode nodeA = new ActionNode("nodeA", ctx -> "A");
        ActionNode nodeB = new ActionNode("nodeB", ctx -> "B");
        
        graph.addNode(start);
        graph.addNode(nodeA);
        graph.addNode(nodeB);
        
        // Create a cycle: A -> B -> A
        graph.addEdge(new WorkflowEdge("start", "nodeA"));
        graph.addEdge(new WorkflowEdge("nodeA", "nodeB"));
        graph.addEdge(new WorkflowEdge("nodeB", "nodeA")); // This creates a cycle
        
        assertThrows(WorkflowDefinitionException.class, graph::validate,
            "Should throw exception when cycle is detected");
    }

    @Test
    void testValidGraph() {
        WorkflowGraph graph = new WorkflowGraph();
        
        StartNode start = new StartNode("start");
        ActionNode nodeA = new ActionNode("nodeA", ctx -> "A");
        EndNode end = new EndNode("end");
        
        graph.addNode(start);
        graph.addNode(nodeA);
        graph.addNode(end);
        
        graph.addEdge(new WorkflowEdge("start", "nodeA"));
        graph.addEdge(new WorkflowEdge("nodeA", "end"));
        
        // Should not throw any exception
        assertDoesNotThrow(graph::validate, "Valid graph should not throw exception");
    }

    @Test
    void testDeterministicFlow() {
        WorkflowGraph graph = new WorkflowGraph();
        
        // Create request with amount
        Map<String, FieldResult> fields = new HashMap<>();
        fields.put("amount", new FieldResult(150, 1.0, "Amount", null));
        ParsingResult request = new ParsingResult(fields, List.of());
        
        // Build graph: Start -> LogicDecision (amount > 100) -> ActionA/ActionB -> End
        StartNode start = new StartNode("start");
        LogicDecisionNode decision = new LogicDecisionNode("decision", 
            ctx -> {
                FieldResult amountField = ctx.getRequest().fields().get("amount");
                return amountField != null && (int) amountField.value() > 100;
            });
        
        ActionNode actionA = new ActionNode("actionA", ctx -> {
            ctx.setVar("result", "A_executed");
            return "A";
        });
        
        ActionNode actionB = new ActionNode("actionB", ctx -> {
            ctx.setVar("result", "B_executed");
            return "B";
        });
        
        EndNode end = new EndNode("end");
        
        graph.addNode(start);
        graph.addNode(decision);
        graph.addNode(actionA);
        graph.addNode(actionB);
        graph.addNode(end);
        
        graph.addEdge(new WorkflowEdge("start", "decision"));
        
        // Route to ActionA if result is true
        graph.addEdge(new WorkflowEdge("decision", "actionA", 
            (ctx, result) -> result.payload().equals(true), "TRUE", 1));
        
        // Route to ActionB if result is false
        graph.addEdge(new WorkflowEdge("decision", "actionB", 
            (ctx, result) -> result.payload().equals(false), "FALSE", 0));
        
        graph.addEdge(new WorkflowEdge("actionA", "end"));
        graph.addEdge(new WorkflowEdge("actionB", "end"));
        
        // Execute
        WorkflowContext context = new WorkflowContext(request);
        WorkflowEngine engine = new WorkflowEngine(graph);
        WorkflowResult result = engine.execute("start", context);
        
        // Verify
        assertEquals(WorkflowStatus.COMPLETED, result.status());
        assertEquals("A_executed", context.getVar("result"));
        
        // Verify trace: Start -> Decision -> ActionA -> End
        var trace = result.context().getTraceLog();
        assertEquals(4, trace.size());
        assertEquals("start", trace.get(0).nodeId());
        assertEquals("decision", trace.get(1).nodeId());
        assertEquals("actionA", trace.get(2).nodeId());
        assertEquals("end", trace.get(3).nodeId());
    }

    @Test
    void testMaxIterationsPreventsInfiniteLoop() {
        WorkflowGraph graph = new WorkflowGraph();
        
        Map<String, FieldResult> fields = new HashMap<>();
        ParsingResult request = new ParsingResult(fields, List.of());
        
        // Create a graph that could loop (though not a cycle in structure)
        StartNode start = new StartNode("start");
        ActionNode loopNode = new ActionNode("loopNode", ctx -> {
            int count = (int) ctx.getVars().getOrDefault("count", 0);
            ctx.setVar("count", count + 1);
            return "looping";
        });
        
        graph.addNode(start);
        graph.addNode(loopNode);
        
        graph.addEdge(new WorkflowEdge("start", "loopNode"));
        // Self-loop edge (which is actually a cycle)
        graph.addEdge(new WorkflowEdge("loopNode", "loopNode"));
        
        // Graph validation should catch this cycle
        assertThrows(WorkflowDefinitionException.class, graph::validate);
    }
}

