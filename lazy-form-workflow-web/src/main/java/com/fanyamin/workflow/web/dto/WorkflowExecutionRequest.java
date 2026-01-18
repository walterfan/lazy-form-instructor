package com.fanyamin.workflow.web.dto;

public class WorkflowExecutionRequest {
    private String userInput;
    private String workflowType;

    public WorkflowExecutionRequest() {
    }

    public WorkflowExecutionRequest(String userInput, String workflowType) {
        this.userInput = userInput;
        this.workflowType = workflowType;
    }

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }

    public String getWorkflowType() {
        return workflowType;
    }

    public void setWorkflowType(String workflowType) {
        this.workflowType = workflowType;
    }
}

