package com.fanyamin.workflow.web.controller;

import com.fanyamin.workflow.core.WorkflowResult;
import com.fanyamin.workflow.web.dto.WorkflowExecutionRequest;
import com.fanyamin.workflow.web.dto.WorkflowExecutionResponse;
import com.fanyamin.workflow.web.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class WorkflowController {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowController.class);

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping("/execute")
    public WorkflowExecutionResponse executeWorkflow(@RequestBody WorkflowExecutionRequest request) {
        logger.info("Received workflow execution request: type={}, input={}", 
                   request.getWorkflowType(), request.getUserInput());

        try {
            WorkflowResult result;

            // Route to different workflow types
            switch (request.getWorkflowType()) {
                case "leave_request":
                    result = workflowService.executeLeaveRequestWorkflow(request.getUserInput());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown workflow type: " + request.getWorkflowType());
            }

            // Build response
            WorkflowExecutionResponse response = new WorkflowExecutionResponse();
            response.setStatus(result.status());
            response.setMessage(result.message());
            response.setParsedFields(result.context().getRequest() != null ? 
                                    result.context().getRequest().fields() : null);
            response.setExecutionTrace(result.context().getTraceLog());
            
            // Extract final context state
            Map<String, Object> finalContext = new HashMap<>();
            finalContext.put("state", result.context().getState());
            finalContext.put("variables", result.context().getVars());
            response.setFinalContext(finalContext);

            return response;

        } catch (Exception e) {
            logger.error("Error executing workflow", e);
            WorkflowExecutionResponse errorResponse = new WorkflowExecutionResponse();
            errorResponse.setStatus(com.fanyamin.workflow.core.WorkflowStatus.FAILED);
            errorResponse.setMessage("Workflow execution failed: " + e.getMessage());
            return errorResponse;
        }
    }

    @GetMapping("/types")
    public Map<String, WorkflowTypeInfo> getWorkflowTypes() {
        Map<String, WorkflowTypeInfo> types = new HashMap<>();
        
        types.put("leave_request", new WorkflowTypeInfo(
            "Leave Request Workflow",
            "AI-powered leave request validation and approval workflow",
            "Example: I need annual leave from Dec 20 to Dec 25 for vacation"
        ));
        
        return types;
    }

    public static class WorkflowTypeInfo {
        private String name;
        private String description;
        private String exampleInput;

        public WorkflowTypeInfo(String name, String description, String exampleInput) {
            this.name = name;
            this.description = description;
            this.exampleInput = exampleInput;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getExampleInput() {
            return exampleInput;
        }

        public void setExampleInput(String exampleInput) {
            this.exampleInput = exampleInput;
        }
    }
}

