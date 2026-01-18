# Thinking Mode Support

This document explains how SmartFormInstructor handles LLM models with thinking/reasoning capabilities.

## Overview

Some advanced LLM models (like OpenAI's o1-preview and o1-mini) generate intermediate reasoning or "thinking" steps before providing their final answer. This library automatically handles these models to extract the clean answer while optionally logging the reasoning process.

## How It Works

### 1. Response Formats Supported

The `OpenAiLlmClient` handles multiple response formats:

#### Format 1: Separate Reasoning Field
Some APIs return reasoning in a dedicated field:
```json
{
  "choices": [
    {
      "message": {
        "reasoning": "Let me analyze the user's request step by step...",
        "content": "{ \"name\": \"John\", \"age\": 30 }"
      }
    }
  ]
}
```

#### Format 2: XML-Tagged Content
Some models embed thinking within the content using XML-like tags:
```
<thinking>
Let me analyze what the user is asking for...
1. They mentioned "next Monday"
2. Today is Friday, October 27
3. So next Monday would be October 30
</thinking>

<answer>
{
  "start_date": "2023-10-30",
  "confidence": 0.95,
  "reasoning": "Calculated from context"
}
</answer>
```

#### Format 3: Markdown Sections
Some models use markdown-style headers:
```
## Thinking
The user wants to schedule a task...

## Answer
{
  "name": "Finish alert feature",
  "priority": 5
}
```

### 2. Automatic Extraction

The `OpenAiLlmClient.extractAnswerFromContent()` method automatically:
1. Detects the response format
2. Extracts the clean answer (filtering out thinking sections)
3. Returns only the actionable result to the application

### 3. Debug Mode

Enable debug mode to see the thinking process:

```bash
# In .env file
LLM_DEBUG=true
```

Or via environment variable:
```bash
export LLM_DEBUG=true
```

When debug mode is enabled, you'll see detailed logs:
```
🔵 HTTP REQUEST
================================================================================
URL: https://api.openai.com/v1/chat/completions
Method: POST
Body: { "model": "o1-preview", "messages": [...] }
================================================================================

🔵 HTTP RESPONSE
================================================================================
Status: 200
Duration: 3240ms
Body: { ... }
================================================================================

🧠 Model Reasoning (1842 chars):
────────────────────────────────────────────────────────────────────────────────
Let me carefully analyze this leave request...
1. User mentioned "sick" - clearly a sick leave
2. User said "next Monday" - need to calculate from current date
...
────────────────────────────────────────────────────────────────────────────────

✅ Extracted answer (287 chars):
────────────────────────────────────────────────────────────────────────────────
{
  "fields": {
    "leave_type": { "value": "sick", "confidence": 0.99, ... }
  }
}
────────────────────────────────────────────────────────────────────────────────
```

## Configuration

### Using Thinking Models

To use OpenAI o1 or similar thinking models:

```bash
# .env file
LLM_API_KEY=sk-your-openai-key
LLM_MODEL=o1-preview        # or o1-mini
LLM_DEBUG=true             # Optional: see the reasoning
```

### Logging Configuration

The library uses SLF4J with Logback. The default configuration in `logback.xml` provides:
- INFO level for general operation
- DEBUG level details when `LLM_DEBUG=true`

To customize logging, create your own `logback.xml` in your application's resources:

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Set to DEBUG to see all LLM interactions -->
    <logger name="com.fanyamin.instructor.llm.OpenAiLlmClient" level="DEBUG">
        <appender-ref ref="CONSOLE" />
    </logger>
</configuration>
```

## Benefits

### 1. Transparency
When debugging is enabled, you can see exactly how the LLM reasoned about the user's input, helping you understand:
- Why certain fields were extracted
- How dates were calculated
- Where ambiguities were identified

### 2. Cleaner Output
The application always receives clean JSON responses, regardless of whether the model used intermediate reasoning steps.

### 3. No Code Changes Required
Your application code remains the same whether using:
- Standard models (GPT-4, GPT-3.5)
- Thinking models (o1-preview, o1-mini)
- Local models (Ollama, LM Studio)

## Performance Considerations

### Response Time
Thinking models typically take longer to respond because they generate reasoning steps:
- GPT-4: ~2-5 seconds
- o1-preview: ~5-15 seconds
- o1-mini: ~3-8 seconds

The library logs the duration of each request when debug mode is enabled.

### Token Usage
Thinking models consume more tokens:
- Input tokens: Similar to other models
- Output tokens: Higher due to reasoning (internal, not always billed)

Check your LLM provider's billing documentation for specifics on how reasoning tokens are charged.

## Troubleshooting

### Reasoning Not Showing in Debug Mode

If you've enabled `LLM_DEBUG=true` but don't see reasoning output:

1. **Check the model**: Only certain models generate separate reasoning
   - o1-preview ✅
   - o1-mini ✅
   - gpt-4 ❌ (no separate reasoning field)
   - gpt-3.5-turbo ❌ (no separate reasoning field)

2. **Verify debug is enabled**: Check that the environment variable is set:
   ```bash
   echo $LLM_DEBUG
   ```

3. **Check log level**: Ensure Logback is configured for DEBUG level on the OpenAiLlmClient

### Incorrect Answer Extraction

If the extracted answer seems wrong or incomplete:

1. **Enable debug mode** to see the raw response
2. **Check the response format** - the model might be using a format not yet supported
3. **File an issue** with the raw response (with sensitive data removed)

## Example Usage

```java
import com.fanyamin.SmartFormInstructor;
import com.fanyamin.instructor.llm.OpenAiLlmClient;

// Create client (reads from .env)
OpenAiLlmClient llmClient = new OpenAiLlmClient();

// Or create with explicit configuration
OpenAiLlmClient llmClient = new OpenAiLlmClient.Builder()
    .apiKey("sk-...")
    .model("o1-preview")
    .debug(true)  // Enable debug logging
    .build();

// Use with SmartFormInstructor
SmartFormInstructor instructor = new SmartFormInstructor(llmClient);
ParsingResult result = instructor.parse(request);

// Result contains clean extracted data, regardless of thinking mode
System.out.println(result.fields().get("leave_type").value());
```

## Future Enhancements

Planned improvements:
- [ ] Support for streaming responses with incremental reasoning
- [ ] Reasoning quality metrics
- [ ] Configurable reasoning vs answer extraction strategies
- [ ] Support for multi-turn reasoning conversations

## References

- [OpenAI o1 Documentation](https://platform.openai.com/docs/models/o1)
- [SLF4J Documentation](http://www.slf4j.org/manual.html)
- [Logback Configuration](https://logback.qos.ch/manual/configuration.html)

