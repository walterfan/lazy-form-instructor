# LLM-driven Lazy Form Engine (LazyFormInstructor)

A Java library for extracting structured data from LLMs with built-in validation, confidence scoring, and retry logic. Inspired by Python's `instructor`.

## Features

- **Schema-First**: Define your data structure using standard JSON Schema.
- **Decoupled**: Separates Business Logic (Schema) from AI Logic (Prompting/Parsing).
- **Validation Loop**: Automatically retries with the LLM if the output doesn't match the schema.
- **Rich Output**: Returns values, confidence scores, reasoning, and alternatives.
- **Provider Agnostic**: Built on Spring AI to support OpenAI, Azure, and more.
- **Complex Forms**: Supports nested objects, arrays, and complex cross-field constraints.
- **Context-Aware**: Uses `Intent Context` (user profile, history, environment) to inform defaults and logic.

## Core Concepts (Universal Smart Form Protocol - USFP)

1. **Form Schema**: JSON Schema describing the form structure (fields, types, constraints, dependencies).
2. **Intent Context**: Runtime context (e.g., current time, user role, location).
3. **Parsing Result**: Standardized output containing:
   - `value`: The extracted value.
   - `confidence`: 0.0 - 1.0 score.
   - `reasoning`: Explanation of the extraction logic.
   - `alternatives`: Other possible candidate values for ambiguity resolution.

## Architecture

- **Input**: `Schema` + `User Input` + `Context`.
- **Engine**:
    1. **Prompt Generation**: Dynamically builds system prompts based on schema and context.
    2. **LLM Interaction**: Calls LLM via Spring AI abstraction.
    3. **Parsing & Validation**: Maps output to JSON, validates against Schema.
    4. **Retry Loop**: If validation fails, feeds errors back to LLM for correction.
- **Output**: `ParsingResult` with fields and potential structural/business errors.

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.fanyamin</groupId>
    <artifactId>lazy-form-instructor</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Usage Example

### 1. Define Schema (JSON Schema)

```json
{
  "type": "object",
  "properties": {
    "name": { "type": "string" },
    "age": { "type": "integer" }
  },
  "required": ["name"]
}
```

### 2. Instantiate Instructor

```java
ChatClient chatClient = ...; // Spring AI ChatClient
LlmClient llmClient = new SpringAiLlmClient(chatClient);
LazyFormInstructor instructor = new LazyFormInstructor(llmClient);
```

### 3. Parse Input

```java
String schema = "..."; 
String input = "John is 30 years old";
Map<String, Object> context = Map.of("now", "2023-10-27");

ParsingRequest request = new ParsingRequest(schema, input, context);
ParsingResult result = instructor.parse(request);

if (result.errors().isEmpty()) {
    System.out.println("Name: " + result.fields().get("name").value());
    System.out.println("Reasoning: " + result.fields().get("name").reasoning());
}
```

## Streaming Mode (new)

If your `LlmClient` supports streaming (for example `OpenAiLlmClient`), you can consume incremental output while the model is generating.

The streaming API emits events:
- Raw JSON text chunks as they arrive
- Best-effort parsed snapshots (when the buffered JSON becomes parseable)
- A final, schema-validated result (schema validation is performed only at the end of an attempt)

Example:

```java
LazyFormInstructor instructor = new LazyFormInstructor(llmClient);

ParsingRequest request = new ParsingRequest(schema, input, context);

instructor.parseStreaming(request)
    .doOnNext(evt -> {
        if (evt instanceof com.fanyamin.instructor.streaming.StreamingParseEvent.RawChunk c) {
            System.out.print(c.chunk());
        } else if (evt instanceof com.fanyamin.instructor.streaming.StreamingParseEvent.Snapshot s) {
            // Best-effort snapshot (not schema-validated)
            System.out.println("Snapshot fields: " + (s.partial().fields() != null ? s.partial().fields().keySet() : "null"));
        } else if (evt instanceof com.fanyamin.instructor.streaming.StreamingParseEvent.FinalResult f) {
            if (f.schemaErrors().isEmpty()) {
                System.out.println("Final OK: " + f.result().fields());
            } else {
                System.out.println("Final schema errors: " + f.schemaErrors());
            }
        }
    })
    .blockLast();
```

## Advanced Capabilities

### Complex Form Support
- **Nested Objects**: Handle sub-forms (e.g., `attendees[]`).
- **Repeatable Arrays**: Manage lists of items (e.g., `expense_items[]`).
- **Dependencies**: Model logic like "If Leave Type is Sick, Medical Certificate is required".

### Multi-step / Wizard Support
- **Partial Fills**: Can fill current step fields and infer future step values from the same input.
- **Ambiguity Handling**: Returns alternatives when the user's intent is unclear (e.g., "next Friday" could mean this coming Friday or the one after).

## Roadmap
- [x] Core API & JSON Protocol
- [x] JSON Schema Validation
- [x] LLM Integration (Spring AI)
- [x] Basic Retry Logic
- [ ] Advanced Complex Schema Extensions (Groups, Cross-field constraints)
- [ ] Multi-step Form State Management

## License

MIT
