package com.fanyamin.workflow.example;

import com.fanyamin.LazyFormInstructor;
import com.fanyamin.instructor.api.FieldResult;
import com.fanyamin.instructor.api.ParsingRequest;
import com.fanyamin.instructor.api.ParsingResult;
import com.fanyamin.instructor.llm.LlmClient;
import com.fanyamin.instructor.llm.LlmClientFactory;
import com.fanyamin.workflow.core.*;
import com.fanyamin.workflow.node.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Example demonstrating a Leave Request workflow with AI-powered validation and approval.
 * 
 * Workflow Flow:
 * 1. Start
 * 2. AI Validation (check if reason is valid)
 * 3. Logic Decision (check if high confidence)
 * 4. Auto-approve OR Human Review
 * 5. End
 */
public class LeaveRequestWorkflowExample {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("AI-Driven Workflow Engine - Leave Request Example");
        System.out.println("=".repeat(80));
        System.out.println();

        try {
            // Step 1: Parse leave request using LazyFormInstructor
            String leaveRequestSchema = """
                {
                  "type": "object",
                  "properties": {
                    "leave_type": {
                      "type": "string",
                      "enum": ["annual", "sick", "unpaid", "marriage"],
                      "description": "Type of leave"
                    },
                    "start_date": {
                      "type": "string",
                      "format": "date",
                      "description": "Leave start date"
                    },
                    "end_date": {
                      "type": "string",
                      "format": "date",
                      "description": "Leave end date"
                    },
                    "reason": {
                      "type": "string",
                      "description": "Reason for leave"
                    }
                  },
                  "required": ["leave_type", "start_date", "end_date", "reason"]
                }
                """;

            String userInput = "I need a week off starting December 15th for my wedding.";
            Map<String, Object> context = Map.of(
                "now", "2023-12-01T10:00:00Z",
                "user", Map.of("id", "emp_123", "managerId", "mgr_456")
            );

            LlmClient llmClient = LlmClientFactory.createFromEnvironment();
            LazyFormInstructor instructor = new LazyFormInstructor(llmClient);

            System.out.println("Step 1: Parsing leave request...");
            System.out.println("User Input: \"" + userInput + "\"");
            System.out.println();

            ParsingRequest parsingRequest = new ParsingRequest(leaveRequestSchema, userInput, context);
            ParsingResult parsingResult = instructor.parse(parsingRequest);

            System.out.println("Parsed Request:");
            if (parsingResult.fields() != null) {
                parsingResult.fields().forEach((key, value) -> {
                    System.out.printf("  %s: %s (confidence: %.2f)%n", 
                        key, value.value(), value.confidence());
                });
            }
            System.out.println();

            // Step 2: Build workflow graph
            System.out.println("Step 2: Building workflow graph...");
            WorkflowGraph graph = buildLeaveWorkflow(llmClient);
            System.out.println("Workflow graph created with AI validation and decision nodes.");
            System.out.println();

            // Step 3: Execute workflow
            System.out.println("Step 3: Executing workflow...");
            System.out.println("-".repeat(80));

            WorkflowContext workflowContext = new WorkflowContext(parsingResult);
            workflowContext.setVar("user_id", "emp_123");
            workflowContext.setVar("manager_id", "mgr_456");

            WorkflowEngine engine = new WorkflowEngine(graph);
            WorkflowResult result = engine.execute("start", workflowContext);

            System.out.println("-".repeat(80));
            System.out.println();

            // Step 4: Display results
            System.out.println("Step 4: Workflow Results");
            System.out.println("Status: " + result.status());
            System.out.println("Message: " + result.message());
            System.out.println();

            System.out.println("Execution Trace:");
            for (TraceEntry trace : result.context().getTraceLog()) {
                System.out.printf("  [%s] %s (%s) -> %s%n",
                    trace.timestamp(),
                    trace.nodeId(),
                    trace.nodeType(),
                    trace.status()
                );
            }
            System.out.println();

            System.out.println("=".repeat(80));
            System.out.println("Workflow completed successfully!");
            System.out.println("=".repeat(80));

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static WorkflowGraph buildLeaveWorkflow(LlmClient llmClient) {
        WorkflowGraph graph = new WorkflowGraph();

        // Define nodes
        StartNode start = new StartNode("start");

        // AI Validation Node
        String validationSchema = """
            {
              "type": "object",
              "properties": {
                "decision": {
                  "type": "string",
                  "enum": ["APPROVE", "REJECT", "ESCALATE"],
                  "description": "Validation decision"
                }
              },
              "required": ["decision"]
            }
            """;

        AiDecisionNode aiValidation = AiDecisionNode.builder()
            .id("ai_validation")
            .llmClient(llmClient)
            .systemPrompt("You are a leave request validator. Evaluate if the leave reason is valid and reasonable.")
            .outputSchema(validationSchema)
            .confidenceThreshold(0.8)
            .build();

        ActionNode autoApprove = new ActionNode("auto_approve", ctx -> {
            System.out.println("  -> Auto-approving leave request");
            ctx.setState("APPROVED");
            return "Leave request approved automatically";
        });

        ActionNode humanReview = new ActionNode("human_review", ctx -> {
            System.out.println("  -> Escalating to manager for review");
            ctx.setState("PENDING_APPROVAL");
            return "Leave request sent to manager";
        });

        EndNode end = new EndNode("end");

        // Add nodes to graph
        graph.addNode(start);
        graph.addNode(aiValidation);
        graph.addNode(autoApprove);
        graph.addNode(humanReview);
        graph.addNode(end);

        // Define edges
        graph.addEdge(new WorkflowEdge("start", "ai_validation"));

        // Branch based on AI decision and confidence
        graph.addEdge(new WorkflowEdge("ai_validation", "auto_approve", 
            (ctx, result) -> {
                if (result.payload() instanceof AiDecisionNode.DecisionPayload decision) {
                    return "APPROVE".equals(decision.decision) && decision.confidence >= 0.8;
                }
                return false;
            },
            "HIGH_CONFIDENCE_APPROVE",
            1
        ));

        graph.addEdge(new WorkflowEdge("ai_validation", "human_review",
            (ctx, result) -> {
                if (result.payload() instanceof AiDecisionNode.DecisionPayload decision) {
                    return !"APPROVE".equals(decision.decision) || decision.confidence < 0.8;
                }
                return true; // Default to human review
            },
            "LOW_CONFIDENCE_OR_REJECT",
            0
        ));

        graph.addEdge(new WorkflowEdge("auto_approve", "end"));
        graph.addEdge(new WorkflowEdge("human_review", "end"));

        return graph;
    }
}

