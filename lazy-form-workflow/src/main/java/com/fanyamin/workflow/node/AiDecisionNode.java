package com.fanyamin.workflow.node;

import com.fanyamin.LazyFormInstructor;
import com.fanyamin.instructor.api.ParsingRequest;
import com.fanyamin.instructor.api.ParsingResult;
import com.fanyamin.instructor.llm.LlmClient;
import com.fanyamin.workflow.core.NodeResult;
import com.fanyamin.workflow.core.WorkflowContext;
import com.fanyamin.workflow.core.WorkflowNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * AI-powered decision node that uses LLM to evaluate policies and make routing decisions.
 */
public class AiDecisionNode implements WorkflowNode {
    private static final Logger logger = LoggerFactory.getLogger(AiDecisionNode.class);
    
    private final String id;
    private final LlmClient llmClient;
    private final String systemPrompt;
    private final String outputSchema;
    private final double confidenceThreshold;
    private final ObjectMapper objectMapper;

    private AiDecisionNode(Builder builder) {
        this.id = builder.id;
        this.llmClient = builder.llmClient;
        this.systemPrompt = builder.systemPrompt;
        this.outputSchema = builder.outputSchema;
        this.confidenceThreshold = builder.confidenceThreshold;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public NodeResult execute(WorkflowContext ctx) {
        try {
            // Prepare user input from context
            String userInput = prepareUserInput(ctx);
            
            // Create context map
            Map<String, Object> context = new HashMap<>();
            context.put("state", ctx.getState());
            context.putAll(ctx.getVars());
            
            // Use LazyFormInstructor to get LLM decision
            LazyFormInstructor instructor = new LazyFormInstructor(llmClient);
            ParsingRequest request = new ParsingRequest(outputSchema, userInput, context);
            ParsingResult result = instructor.parse(request);
            
            // Extract decision and confidence
            DecisionPayload decision = extractDecision(result);
            
            // Store decision in context
            ctx.setVar(id + "_decision", decision);
            
            logger.info("AI Decision: {} with confidence {}", decision.decision, decision.confidence);
            
            return NodeResult.success(decision, decision.decision);
            
        } catch (Exception e) {
            logger.error("Error in AI decision node: {}", e.getMessage(), e);
            return NodeResult.failure(e.getMessage());
        }
    }

    private String prepareUserInput(WorkflowContext ctx) {
        // Create a summary of the request for the LLM
        StringBuilder input = new StringBuilder();
        input.append(systemPrompt).append("\n\n");
        input.append("Request Details:\n");
        
        // Add request fields
        if (ctx.getRequest() != null && ctx.getRequest().fields() != null) {
            ctx.getRequest().fields().forEach((key, fieldResult) -> {
                input.append("- ").append(key).append(": ").append(fieldResult.value()).append("\n");
            });
        }
        
        // Add relevant vars
        if (!ctx.getVars().isEmpty()) {
            input.append("\nContext Variables:\n");
            ctx.getVars().forEach((key, value) -> {
                input.append("- ").append(key).append(": ").append(value).append("\n");
            });
        }
        
        return input.toString();
    }

    private DecisionPayload extractDecision(ParsingResult result) {
        // Extract decision, confidence, and reasoning from parsing result
        String decision = "UNKNOWN";
        double confidence = 0.0;
        String reasoning = "No reasoning provided";
        
        if (result.fields() != null) {
            var decisionField = result.fields().get("decision");
            if (decisionField != null) {
                decision = String.valueOf(decisionField.value());
                confidence = decisionField.confidence();
                reasoning = decisionField.reasoning();
            }
        }
        
        return new DecisionPayload(decision, confidence, reasoning);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private LlmClient llmClient;
        private String systemPrompt;
        private String outputSchema;
        private double confidenceThreshold = 0.85;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder llmClient(LlmClient llmClient) {
            this.llmClient = llmClient;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder outputSchema(String outputSchema) {
            this.outputSchema = outputSchema;
            return this;
        }

        public Builder confidenceThreshold(double confidenceThreshold) {
            this.confidenceThreshold = confidenceThreshold;
            return this;
        }

        public AiDecisionNode build() {
            if (id == null) throw new IllegalStateException("id is required");
            if (llmClient == null) throw new IllegalStateException("llmClient is required");
            if (systemPrompt == null) throw new IllegalStateException("systemPrompt is required");
            if (outputSchema == null) throw new IllegalStateException("outputSchema is required");
            return new AiDecisionNode(this);
        }
    }

    public static class DecisionPayload {
        public final String decision;
        public final double confidence;
        public final String reasoning;

        public DecisionPayload(String decision, double confidence, String reasoning) {
            this.decision = decision;
            this.confidence = confidence;
            this.reasoning = reasoning;
        }
    }
}



