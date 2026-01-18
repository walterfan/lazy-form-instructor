# Using SpringAiLlmClient in Spring Boot Applications

This guide shows how to use `SpringAiLlmClient` in a Spring Boot application.

## Why SpringAiLlmClient?

`SpringAiLlmClient` is designed for Spring Boot applications that want to leverage Spring AI's unified interface across multiple LLM providers (OpenAI, Anthropic, Azure OpenAI, Ollama, etc.).

For standalone applications (like the examples in `smart-form-example`), use `OpenAiLlmClient` instead, which provides the same functionality (thinking mode support, debug logging) without requiring Spring Boot.

## Setup

### 1. Add Dependencies

```xml
<dependencies>
    <!-- SmartFormInstructor -->
    <dependency>
        <groupId>com.fanyamin</groupId>
        <artifactId>smart-form-instructor</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>
    
    <!-- Spring Boot Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    
    <!-- Spring AI (already included in smart-form-instructor) -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
        <version>1.0.0-M1</version>
    </dependency>
</dependencies>

<repositories>
    <repository>
        <id>spring-milestones</id>
        <name>Spring Milestones</name>
        <url>https://repo.spring.io/milestone</url>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </repository>
</repositories>
```

### 2. Configure Spring AI

**application.properties:**

```properties
# OpenAI Configuration
spring.ai.openai.api-key=${SPRING_AI_OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4-turbo-preview
spring.ai.openai.chat.options.temperature=0.7
spring.ai.openai.chat.options.max-tokens=4096

# For custom base URL (e.g., Azure OpenAI)
#spring.ai.openai.base-url=https://your-resource.openai.azure.com

# Enable debug mode
llm.debug=${LLM_DEBUG:false}
```

**Or use environment variables:**

```bash
export SPRING_AI_OPENAI_API_KEY=sk-...
export LLM_DEBUG=true
```

### 3. Create Configuration Class

```java
package com.example.smartform.config;

import com.fanyamin.SmartFormInstructor;
import com.fanyamin.instructor.llm.SpringAiLlmClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SmartFormConfig {

    @Value("${llm.debug:false}")
    private boolean debug;

    /**
     * Create SpringAiLlmClient bean.
     * The ChatClient.Builder is auto-configured by Spring AI starter.
     */
    @Bean
    public SpringAiLlmClient springAiLlmClient(ChatClient.Builder chatClientBuilder) {
        ChatClient chatClient = chatClientBuilder.build();
        return new SpringAiLlmClient(chatClient, debug);
    }

    /**
     * Create SmartFormInstructor bean.
     */
    @Bean
    public SmartFormInstructor smartFormInstructor(SpringAiLlmClient llmClient) {
        return new SmartFormInstructor(llmClient);
    }
}
```

### 4. Use in Your Service

```java
package com.example.smartform.service;

import com.fanyamin.SmartFormInstructor;
import com.fanyamin.instructor.api.ParsingRequest;
import com.fanyamin.instructor.api.ParsingResult;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FormParsingService {

    private final SmartFormInstructor instructor;

    public FormParsingService(SmartFormInstructor instructor) {
        this.instructor = instructor;
    }

    public ParsingResult parseLeaveRequest(String userInput) {
        String schema = loadLeaveRequestSchema(); // Load from resources
        
        Map<String, Object> context = Map.of(
            "now", java.time.Instant.now().toString(),
            "user", Map.of(
                "id", "user_123",
                "role", "employee"
            )
        );

        ParsingRequest request = new ParsingRequest(schema, userInput, context);
        return instructor.parse(request);
    }

    private String loadLeaveRequestSchema() {
        // Load from resources
        try (var stream = getClass().getResourceAsStream("/schemas/leave-request-schema.json")) {
            return new String(stream.readAllBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load schema", e);
        }
    }
}
```

### 5. Create REST Controller

```java
package com.example.smartform.controller;

import com.fanyamin.instructor.api.ParsingResult;
import com.example.smartform.service.FormParsingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forms")
public class FormController {

    private final FormParsingService formParsingService;

    public FormController(FormParsingService formParsingService) {
        this.formParsingService = formParsingService;
    }

    @PostMapping("/leave-request")
    public ParsingResult parseLeaveRequest(@RequestBody LeaveRequestInput input) {
        return formParsingService.parseLeaveRequest(input.getUserInput());
    }

    record LeaveRequestInput(String userInput) {}
}
```

## Complete Spring Boot Application Example

```java
package com.example.smartform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmartFormApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartFormApplication.class, args);
    }
}
```

## Running the Application

```bash
# Set environment variables
export SPRING_AI_OPENAI_API_KEY=sk-...
export LLM_DEBUG=true

# Run the Spring Boot application
mvn spring-boot:run

# Test the endpoint
curl -X POST http://localhost:8080/api/forms/leave-request \
  -H "Content-Type: application/json" \
  -d '{"userInput": "I need sick leave from next Monday for 3 days"}'
```

## Features Available

When using `SpringAiLlmClient`:

✅ **Thinking Mode Support**: Automatically filters reasoning content from thinking models (o1, Qwen, etc.)

✅ **Debug Logging**: Set `LLM_DEBUG=true` to see detailed request/response logs

✅ **Multi-Provider Support**: Switch between OpenAI, Anthropic, Azure OpenAI, Ollama by changing Spring AI configuration

✅ **Spring Integration**: Full dependency injection, configuration management, and Spring Boot features

## Comparison: SpringAiLlmClient vs OpenAiLlmClient

| Feature | SpringAiLlmClient | OpenAiLlmClient |
|---------|-------------------|-----------------|
| **Use Case** | Spring Boot apps | Standalone apps |
| **Configuration** | Spring properties | Environment variables / .env |
| **Providers** | Multiple (via Spring AI) | OpenAI-compatible only |
| **Dependency Injection** | ✅ Yes | ❌ No |
| **Thinking Mode** | ✅ Yes | ✅ Yes |
| **Debug Logging** | ✅ Yes | ✅ Yes |
| **SSL Skip** | Via Spring config | ✅ Direct support |
| **Setup Complexity** | Medium (Spring Boot) | Low (just env vars) |

## When to Use Each

**Use SpringAiLlmClient when:**
- You have a Spring Boot application
- You want to leverage Spring's dependency injection
- You need to switch between multiple LLM providers
- You want centralized Spring configuration management

**Use OpenAiLlmClient when:**
- You have a standalone Java application
- You want simple environment variable configuration
- You only need OpenAI-compatible APIs
- You need direct SSL verification control

## Troubleshooting

### Spring AI ChatClient Bean Not Found

**Error:**
```
Parameter 0 of method springAiLlmClient in SmartFormConfig required a bean of type 'ChatClient.Builder' that could not be found.
```

**Solution:**
Ensure you have the Spring AI starter dependency:
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0-M1</version>
</dependency>
```

### Debug Logs Not Showing

**Solution 1**: Set debug via configuration:
```properties
llm.debug=true
```

**Solution 2**: Set programmatically:
```java
@Bean
public SpringAiLlmClient springAiLlmClient(ChatClient.Builder builder) {
    ChatClient chatClient = builder.build();
    SpringAiLlmClient client = new SpringAiLlmClient(chatClient);
    client.setDebug(true);  // Enable debug mode
    return client;
}
```

### Thinking Content Not Filtered

Ensure your debug mode is enabled to see what's happening:
```properties
llm.debug=true
```

Check the logs for:
- `🔍 Extracted answer after </think> tag` - Qwen-style thinking
- `🔍 Extracted answer from XML-tagged content` - XML-style thinking
- `🔍 Extracted answer from markdown-sectioned content` - Markdown-style thinking

## See Also

- [THINKING_MODE.md](THINKING_MODE.md) - Detailed thinking mode documentation
- [QUICKSTART.md](QUICKSTART.md) - Quick start guide for standalone usage
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)

