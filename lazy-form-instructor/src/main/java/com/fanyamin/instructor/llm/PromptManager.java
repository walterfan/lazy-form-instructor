package com.fanyamin.instructor.llm;

import com.fanyamin.instructor.api.ParsingRequest;

public class PromptManager {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
        You are a smart form filling assistant. Your goal is to extract structured data from user input based on a provided JSON Schema.
        
        ### INSTRUCTIONS
        1. **Analyze the Input**: detailedly read the User Input and Context.
        2. **Follow the Schema**: The output MUST adhere to the provided JSON Schema.
        3. **Extraction Rules**:
           - **Value**: Extract the value for each field. If a field is missing, do not invent it unless a default is clear from context.
           - **Confidence**: Assign a confidence score (0.0 to 1.0) for each extracted value.
           - **Reasoning**: Explain why you extracted this value or why you are unsure.
           - **Alternatives**: If the input is ambiguous, list alternative values.
        4. **Validation**:
           - Ensure types match the schema (e.g., integers for numbers).
           - Respect enums and constraints.
        5. **Output Format**:
           You must return a JSON object strictly matching this structure:
           {
             "fields": {
               "fieldName": {
                 "value": <extracted_value>,
                 "confidence": <0.0-1.0>,
                 "reasoning": "<explanation>",
                 "alternatives": [<alt1>, <alt2>]
               }
             },
             "errors": [
                { "path": "<field_path>", "message": "<error_message>", "type": "<error_type>" }
             ]
           }

        ### CONTEXT
        %s

        ### FORM SCHEMA
        %s

        ### USER INPUT
        %s

        Answer strictly in JSON.
        """;

    public String generateSystemPrompt(ParsingRequest request) {
        String contextStr = request.context() != null ? request.context().toString() : "{}";
        return String.format(SYSTEM_PROMPT_TEMPLATE, contextStr, request.schema(), request.userInput());
    }
}

