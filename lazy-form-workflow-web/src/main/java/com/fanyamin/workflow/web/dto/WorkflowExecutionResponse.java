package com.fanyamin.workflow.web.dto;

import com.fanyamin.instructor.api.FieldResult;
import com.fanyamin.workflow.core.TraceEntry;
import com.fanyamin.workflow.core.WorkflowStatus;

import java.util.List;
import java.util.Map;

public class WorkflowExecutionResponse {
    private WorkflowStatus status;
    private String message;
    private Map<String, FieldResult> parsedFields;
    private List<TraceEntry> executionTrace;
    private Map<String, Object> finalContext;

    public WorkflowExecutionResponse() {
    }

    public WorkflowExecutionResponse(WorkflowStatus status, String message,
                                     Map<String, FieldResult> parsedFields,
                                     List<TraceEntry> executionTrace,
                                     Map<String, Object> finalContext) {
        this.status = status;
        this.message = message;
        this.parsedFields = parsedFields;
        this.executionTrace = executionTrace;
        this.finalContext = finalContext;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, FieldResult> getParsedFields() {
        return parsedFields;
    }

    public void setParsedFields(Map<String, FieldResult> parsedFields) {
        this.parsedFields = parsedFields;
    }

    public List<TraceEntry> getExecutionTrace() {
        return executionTrace;
    }

    public void setExecutionTrace(List<TraceEntry> executionTrace) {
        this.executionTrace = executionTrace;
    }

    public Map<String, Object> getFinalContext() {
        return finalContext;
    }

    public void setFinalContext(Map<String, Object> finalContext) {
        this.finalContext = finalContext;
    }
}

