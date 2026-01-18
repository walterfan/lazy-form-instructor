# LazyFormInstructor Examples

This folder contains command-line examples demonstrating the LazyFormInstructor library's capabilities based on the worked examples from the design document.

## Examples

### 1. Leave Request Example (`LeaveRequestExample.java`)

Demonstrates a multi-day leave request with dependencies and business rules.

**User Input:**
> "I'm feeling really sick, I need a week off starting next Monday. Please send it to my manager."

**Features Demonstrated:**
- Date calculation from relative time references ("next Monday")
- Context-aware field extraction (manager ID from user profile)
- Business rule validation (medical certificate requirement)
- Confidence scoring for each field
- Alternative values for ambiguous fields
- Streaming LLM output (raw JSON chunks) with final JSON Schema validation

**Run:**
```bash
cd lazy-form-example
mvn clean compile exec:java -Dexec.mainClass="com.fanyamin.example.LeaveRequestExample"
```

### 2. Task Request Example (`TaskRequestExample.java`)

Demonstrates task creation from natural language with priority inference and deadline calculation.

**User Input:**
> "tomorrow is monday, I have to finish the sending alert feature ASAP, the release date is next friday"

**Features Demonstrated:**
- Priority inference from keywords ("ASAP" → high priority)
- Multiple date calculations (schedule time and deadline)
- Handling missing fields with low confidence
- Alternative value suggestions
- Default value application from schema
- Streaming LLM output (raw JSON chunks) with final JSON Schema validation

**Run:**
```bash
cd lazy-form-example
mvn clean compile exec:java -Dexec.mainClass="com.fanyamin.example.TaskRequestExample"
```

## Project Structure

```
lazy-form-example/
├── pom.xml
├── src/
│   └── main/
│       ├── java/com/fanyamin/example/
│       │   ├── LeaveRequestExample.java
│       │   └── TaskRequestExample.java
│       └── resources/
│           ├── leave-request-schema.json
│           └── task-request-schema.json
└── README.md
```

## Key Concepts Illustrated

1. **Schema-First Design**: Each example starts with a JSON Schema defining the form structure.

2. **Context-Aware Parsing**: User context (current time, user profile) influences field extraction.

3. **Confidence Scoring**: Each extracted field includes a confidence score (0.0 - 1.0).

4. **Reasoning Transparency**: The system explains why it extracted each value.

5. **Alternative Values**: For ambiguous inputs, multiple candidate values are provided.

6. **Validation & Business Rules**: Schema validation plus business logic (e.g., medical certificate requirement).

## Understanding the Output

Each example displays:

```
📋 Field: field_name
   Value: extracted_value
   Confidence: 0.95 (🟢 high / 🟡 medium / 🔴 low)
   Reasoning: Explanation of extraction logic
   Alternatives: [other_possible_values]
```

Validation errors show:
```
❌ field_name: Error message
   Type: error_type
```

## Extending the Examples

To create your own example:

1. Define a JSON Schema in `src/main/resources/`
2. Create a new class extending the pattern:
   - Load schema
   - Define user input and context
   - Create mock LLM response (or use real LLM)
   - Parse and display results
3. Update this README with your example

## Note on Mock Responses

These examples use `MockLlmClient` with predefined responses to demonstrate the expected behavior without requiring an actual LLM API key. In production, replace with `SpringAiLlmClient` connected to a real LLM provider.

