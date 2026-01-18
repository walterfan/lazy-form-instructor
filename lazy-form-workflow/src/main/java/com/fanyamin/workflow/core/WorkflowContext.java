package com.fanyamin.workflow.core;

import com.fanyamin.instructor.api.ParsingResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A mutable blackboard shared across all nodes in the workflow execution.
 * Inspired by N8N's global execution state pattern.
 */
public class WorkflowContext {
    private final ParsingResult request;
    private String state;
    private final Map<String, Object> meta;
    private final Map<String, Object> vars;
    private final List<TraceEntry> traceLog;

    public WorkflowContext(ParsingResult request) {
        this.request = request;
        this.state = "REQUEST"; // Default initial state
        this.meta = new HashMap<>();
        this.vars = new HashMap<>();
        this.traceLog = new ArrayList<>();
    }

    public ParsingResult getRequest() {
        return request;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public Map<String, Object> getVars() {
        return vars;
    }

    public void setVar(String key, Object value) {
        this.vars.put(key, value);
    }

    public Object getVar(String key) {
        return this.vars.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getVar(String key, Class<T> type) {
        Object val = this.vars.get(key);
        if (val != null && type.isInstance(val)) {
            return (T) val;
        }
        return null;
    }

    public List<TraceEntry> getTraceLog() {
        return new ArrayList<>(traceLog);
    }

    public void appendTrace(TraceEntry entry) {
        this.traceLog.add(entry);
    }
}



