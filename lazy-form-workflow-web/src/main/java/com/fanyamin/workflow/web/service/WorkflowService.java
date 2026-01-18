package com.fanyamin.workflow.web.service;

import com.fanyamin.LazyFormInstructor;
import com.fanyamin.instructor.api.ParsingRequest;
import com.fanyamin.instructor.api.ParsingResult;
import com.fanyamin.instructor.llm.LlmClient;
import com.fanyamin.workflow.core.*;
import com.fanyamin.workflow.node.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class WorkflowService {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowService.class);

    private final LazyFormInstructor instructor;
    private final LlmClient llmClient;

    public WorkflowService(LazyFormInstructor instructor, LlmClient llmClient) {
        this.instructor = instructor;
        this.llmClient = llmClient;
    }

    public WorkflowResult executeLeaveRequestWorkflow(String userInput) {
        logger.info("Executing leave request workflow for input: {}", userInput);

        try {
            // Step 1: Parse the leave request
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

            Map<String, Object> context = Map.of(
                "now", Instant.now().toString(),
                "user", Map.of("id", "emp_123", "managerId", "mgr_456")
            );

            ParsingRequest parsingRequest = new ParsingRequest(leaveRequestSchema, userInput, context);
            ParsingResult parsingResult = instructor.parse(parsingRequest);

            // Step 2: Build and execute workflow
            WorkflowGraph graph = buildLeaveWorkflow();
            WorkflowContext workflowContext = new WorkflowContext(parsingResult);
            workflowContext.setVar("user_id", "emp_123");
            workflowContext.setVar("manager_id", "mgr_456");

            WorkflowEngine engine = new WorkflowEngine(graph);
            return engine.execute("start", workflowContext);

        } catch (Exception e) {
            logger.error("Error executing leave request workflow", e);
            throw new RuntimeException("Failed to execute workflow: " + e.getMessage(), e);
        }
    }

    private WorkflowGraph buildLeaveWorkflow() {
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
                },
                "reasoning": {
                  "type": "string",
                  "description": "Explanation for the decision"
                }
              },
              "required": ["decision", "reasoning"]
            }
            """;

        AiDecisionNode aiValidation = AiDecisionNode.builder()
            .id("ai_validation")
            .llmClient(llmClient)
            .systemPrompt("You are a leave request validator. Evaluate if the leave reason is valid and reasonable. " +
                         "Consider: Is the reason legitimate? Is the timing appropriate? " +
                         "APPROVE for valid requests, REJECT for invalid ones, ESCALATE if uncertain.")
            .outputSchema(validationSchema)
            .confidenceThreshold(0.7)
            .build();

        ActionNode autoApprove = new ActionNode("auto_approve", ctx -> {
            logger.info("Auto-approving leave request");
            ctx.setState("APPROVED");
            return "Leave request approved automatically";
        });

        ActionNode humanReview = new ActionNode("human_review", ctx -> {
            logger.info("Escalating to manager for review");
            ctx.setState("PENDING_APPROVAL");
            return "Leave request sent to manager for review";
        });

        ActionNode autoReject = new ActionNode("auto_reject", ctx -> {
            logger.info("Auto-rejecting leave request");
            ctx.setState("REJECTED");
            return "Leave request rejected automatically";
        });

        EndNode end = new EndNode("end");

        // Add nodes to graph
        graph.addNode(start);
        graph.addNode(aiValidation);
        graph.addNode(autoApprove);
        graph.addNode(humanReview);
        graph.addNode(autoReject);
        graph.addNode(end);

        // Define edges
        graph.addEdge(new WorkflowEdge("start", "ai_validation"));

        // Branch based on AI decision and confidence
        graph.addEdge(new WorkflowEdge("ai_validation", "auto_approve",
            (ctx, result) -> {
                if (result.payload() instanceof AiDecisionNode.DecisionPayload decision) {
                    return "APPROVE".equals(decision.decision) && decision.confidence >= 0.85;
                }
                return false;
            },
            "HIGH_CONFIDENCE_APPROVE",
            1
        ));

        graph.addEdge(new WorkflowEdge("ai_validation", "auto_reject",
            (ctx, result) -> {
                if (result.payload() instanceof AiDecisionNode.DecisionPayload decision) {
                    return "REJECT".equals(decision.decision) && decision.confidence >= 0.85;
                }
                return false;
            },
            "HIGH_CONFIDENCE_REJECT",
            2
        ));

        graph.addEdge(new WorkflowEdge("ai_validation", "human_review",
            (ctx, result) -> {
                if (result.payload() instanceof AiDecisionNode.DecisionPayload decision) {
                    return "ESCALATE".equals(decision.decision) || decision.confidence < 0.85;
                }
                return true; // Default to human review
            },
            "LOW_CONFIDENCE_OR_ESCALATE",
            0
        ));

        graph.addEdge(new WorkflowEdge("auto_approve", "end"));
        graph.addEdge(new WorkflowEdge("auto_reject", "end"));
        graph.addEdge(new WorkflowEdge("human_review", "end"));

        return graph;
    }
}

