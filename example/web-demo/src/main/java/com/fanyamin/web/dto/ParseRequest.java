package com.fanyamin.web.dto;

public class ParseRequest {
    private String formType;  // "leave" or "task"
    private String userInput;

    public String getFormType() {
        return formType;
    }

    public void setFormType(String formType) {
        this.formType = formType;
    }

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }
}

