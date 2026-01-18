# LLM Client Configuration Guide

This directory contains the LLM client implementation for the SmartFormInstructor library. The library uses an OpenAI-compatible API client that works with multiple LLM providers.

## Configuration Priority

The library reads configuration from multiple sources in this priority order:

1. **System Environment Variables** (highest priority)
2. **.env File** (in current directory or up to 5 parent directories)
3. **Default Values** (lowest priority)

This flexible approach allows you to:
- Use environment variables in production/CI
- Use `.env` files for local development
- Have sensible defaults for quick testing

## Supported Providers

The `OpenAiLlmClient` works with any OpenAI API-compatible service:

- **OpenAI** (GPT-4, GPT-3.5-turbo, etc.)
- **Azure OpenAI**
- **Ollama** (Local models: Llama 2, Mistral, CodeLlama, etc.)
- **LM Studio** (Local models with OpenAI-compatible API)
- **vLLM** (High-performance inference)
- **Together AI**, **Groq**, **Fireworks AI** and other compatible services
- **Spring AI** (For Spring Boot applications)
- **Mock** (For testing without API calls)

## Quick Start

### Option 1: Using .env File (Easiest for Local Development)

1. Copy the example file:
```bash
cp env.example .env
```

2. Edit `.env` with your configuration:
```bash
# For OpenAI
LLM_API_KEY=sk-...
LLM_MODEL=gpt-4-turbo-preview

# For Ollama (local)
LLM_BASE_URL=http://localhost:11434/v1/chat/completions
LLM_MODEL=llama2
```

3. Use in code (configuration loaded automatically):
```java
LlmClient client = LlmClientFactory.createFromEnvironment();
```

The library automatically searches for `.env` file in the current directory and up to 5 parent directories.

### Option 2: Using Environment Variables (Recommended for Production)

Configure your LLM client through environment variables:

```bash
# Required for cloud services, optional for local
export LLM_API_KEY="sk-..."

# Optional: defaults to OpenAI endpoint
export LLM_BASE_URL="https://api.openai.com/v1/chat/completions"

# Optional: defaults to gpt-4-turbo-preview
export LLM_MODEL="gpt-4-turbo-preview"

# Optional tuning parameters
export LLM_TEMPERATURE="0.7"  # 0.0-1.0, default: 0.7
export LLM_MAX_TOKENS="4096"  # default: 4096
```

Then create a client:

```java
import com.fanyamin.instructor.llm.LlmClientFactory;

// Auto-configure from environment variables
LlmClient client = LlmClientFactory.createFromEnvironment();
```

### Provider-Specific Configuration

#### OpenAI

```bash
export LLM_API_KEY="sk-..."
export LLM_MODEL="gpt-4-turbo-preview"
# LLM_BASE_URL uses default OpenAI endpoint
```

```java
LlmClient client = new OpenAiLlmClient("sk-...");
// or
LlmClient client = new OpenAiLlmClient("sk-...", "gpt-4-turbo-preview");
```

#### Azure OpenAI

```bash
export LLM_API_KEY="your-azure-key"
export LLM_BASE_URL="https://your-resource.openai.azure.com/openai/deployments/your-deployment/chat/completions?api-version=2024-02-01"
export LLM_MODEL="gpt-4"
```

#### Ollama (Local)

First, start Ollama with OpenAI-compatible API:

```bash
# Start Ollama server
ollama serve

# Pull a model
ollama pull llama2
```

Configure:

```bash
export LLM_BASE_URL="http://localhost:11434/v1/chat/completions"
export LLM_MODEL="llama2"
# No API key needed for local Ollama
```

```java
LlmClient client = LlmClientFactory.createFromEnvironment();
```

#### LM Studio

```bash
export LLM_BASE_URL="http://localhost:1234/v1/chat/completions"
export LLM_MODEL="local-model"
# No API key needed
```

## Environment Variables Reference

All variables can be set via:
- System environment variables: `export LLM_API_KEY="..."`
- `.env` file: `LLM_API_KEY=...`

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `LLM_API_KEY` | API key | - | Yes for cloud services |
| `LLM_BASE_URL` | API endpoint URL | `https://api.openai.com/v1/chat/completions` | No |
| `LLM_MODEL` | Model name | `gpt-4-turbo-preview` | No |
| `LLM_TEMPERATURE` | Response randomness (0.0-1.0) | `0.7` | No |
| `LLM_MAX_TOKENS` | Maximum response length | `4096` | No |

### .env File Format

```bash
# Comments are supported
LLM_API_KEY=sk-...
LLM_MODEL=gpt-4-turbo-preview

# Quotes are optional
LLM_BASE_URL="https://api.openai.com/v1/chat/completions"

# Or single quotes
LLM_TEMPERATURE='0.7'
```

**Security Note:** The `.env` file is automatically git-ignored. Never commit API keys to version control!

## Code Configuration

### Basic Usage

```java
// From environment variables
LlmClient client = LlmClientFactory.createFromEnvironment();

// With API key
LlmClient client = LlmClientFactory.create("sk-...");

// With API key and model
LlmClient client = LlmClientFactory.create("sk-...", "gpt-4-turbo-preview");
```

### Advanced Configuration with Builder

```java
LlmClient client = new OpenAiLlmClient.Builder()
    .apiKey("sk-...")
    .model("gpt-4-turbo-preview")
    .apiUrl("https://api.openai.com/v1/chat/completions")
    .temperature(0.7)
    .maxTokens(4096)
    .build();
```

### Mock Client for Testing

```java
MockLlmClient mock = LlmClientFactory.createMock();
mock.setMockResponse("""
    {
      "fields": {
        "name": {
          "value": "John Doe",
          "confidence": 0.99,
          "reasoning": "Test"
        }
      }
    }
    """);

SmartFormInstructor instructor = new SmartFormInstructor(mock);
```

## Usage with SmartFormInstructor

```java
import com.fanyamin.SmartFormInstructor;
import com.fanyamin.instructor.api.*;
import com.fanyamin.instructor.llm.*;

// Create LLM client
LlmClient llmClient = LlmClientFactory.createFromEnvironment();

// Create SmartFormInstructor
SmartFormInstructor instructor = new SmartFormInstructor(llmClient);

// Parse user input
ParsingRequest request = new ParsingRequest(
    schemaJson,
    "I need a week off starting next Monday",
    Map.of("now", "2023-10-27T10:00:00Z")
);

ParsingResult result = instructor.parse(request);
```

## Using Spring AI (for Spring Boot applications)

If you're using Spring Boot, you can leverage Spring AI:

### 1. Add dependency

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0-M1</version>
</dependency>
```

### 2. Configure in `application.properties`

```properties
spring.ai.openai.api-key=${LLM_API_KEY}
spring.ai.openai.chat.options.model=${LLM_MODEL:gpt-4-turbo-preview}
spring.ai.openai.chat.options.temperature=${LLM_TEMPERATURE:0.7}
```

### 3. Use in your service

```java
@Service
public class FormParsingService {
    private final SmartFormInstructor instructor;

    public FormParsingService(ChatClient.Builder chatClientBuilder) {
        ChatClient chatClient = chatClientBuilder.build();
        LlmClient llmClient = new SpringAiLlmClient(chatClient);
        this.instructor = new SmartFormInstructor(llmClient);
    }

    public ParsingResult parseForm(ParsingRequest request) {
        return instructor.parse(request);
    }
}
```

## Recommended Models

### For Production (Best Quality)

- **OpenAI**: `gpt-4-turbo-preview` or `gpt-4`
- **Azure OpenAI**: `gpt-4`

### For Development (Cost-Effective)

- **OpenAI**: `gpt-3.5-turbo`
- **Ollama**: `llama2`, `mistral`, `codellama` (free, local)

### For Local Development (Free, Private)

Run locally with Ollama:

```bash
# Install Ollama: https://ollama.ai

# Pull and run a model
ollama pull llama2
ollama serve
```

Then configure:
```bash
export LLM_BASE_URL="http://localhost:11434/v1/chat/completions"
export LLM_MODEL="llama2"
```

## Common Configuration Examples

### OpenAI GPT-4

```bash
export LLM_API_KEY="sk-..."
export LLM_MODEL="gpt-4-turbo-preview"
```

### Azure OpenAI

```bash
export LLM_API_KEY="your-azure-key"
export LLM_BASE_URL="https://your-resource.openai.azure.com/openai/deployments/gpt-4/chat/completions?api-version=2024-02-01"
export LLM_MODEL="gpt-4"
```

### Ollama with Llama 2

```bash
export LLM_BASE_URL="http://localhost:11434/v1/chat/completions"
export LLM_MODEL="llama2"
```

### Ollama with Mistral

```bash
export LLM_BASE_URL="http://localhost:11434/v1/chat/completions"
export LLM_MODEL="mistral"
```

### LM Studio

```bash
export LLM_BASE_URL="http://localhost:1234/v1/chat/completions"
export LLM_MODEL="TheBloke/Mistral-7B-Instruct-v0.2-GGUF"
```

## Error Handling

```java
try {
    ParsingResult result = instructor.parse(request);
    // Process result
} catch (RuntimeException e) {
    if (e.getMessage().contains("API key")) {
        System.err.println("Authentication error. Check your LLM_API_KEY.");
    } else if (e.getMessage().contains("rate limit")) {
        System.err.println("Rate limit exceeded. Please retry later.");
    } else if (e.getMessage().contains("API error")) {
        System.err.println("LLM API error: " + e.getMessage());
    } else {
        System.err.println("Unexpected error: " + e.getMessage());
    }
}
```

## Troubleshooting

### "No LLM provider configured" Error

Set the required configuration via environment variables OR .env file:

**Option 1: Environment variable**
```bash
export LLM_API_KEY="your-api-key"
```

**Option 2: .env file**
```bash
echo "LLM_API_KEY=your-api-key" > .env
```

**Option 3: For Ollama (local)**
```bash
export LLM_BASE_URL="http://localhost:11434/v1/chat/completions"
# or in .env file:
echo "LLM_BASE_URL=http://localhost:11434/v1/chat/completions" > .env
```

### .env File Not Loading

The library searches for `.env` in:
1. Current working directory
2. Up to 5 parent directories

To verify .env file is found:
```java
import com.fanyamin.instructor.llm.LlmConfig;

if (LlmConfig.hasDotEnv()) {
    System.out.println(".env file loaded successfully");
} else {
    System.out.println(".env file not found");
}
```

To reload .env file after changes:
```java
LlmConfig.reloadDotEnv();
```

### Connection Errors with Ollama

1. Check if Ollama is running:
   ```bash
   curl http://localhost:11434/api/tags
   ```

2. Make sure you're using the correct endpoint:
   ```bash
   export LLM_BASE_URL="http://localhost:11434/v1/chat/completions"
   ```

3. Verify the model is pulled:
   ```bash
   ollama list
   ```

### Rate Limiting

If you hit rate limits:
- Use a lower-tier model for development
- Implement exponential backoff
- Use local Ollama for development
- Cache responses when possible

## Security Best Practices

1. **Never commit API keys** to version control
2. Use **environment variables** or secure secret management (e.g., HashiCorp Vault, AWS Secrets Manager)
3. **Rotate keys** regularly
4. **Monitor usage** and set billing alerts
5. Consider using **local models** (Ollama) for sensitive data
6. Use **minimal permissions** for API keys when available

## Performance Tips

1. **Model Selection**: Smaller/faster models for development, larger for production
2. **Temperature**: Lower (0.1-0.3) for deterministic parsing, higher (0.7-1.0) for creative tasks
3. **Max Tokens**: Set appropriately to avoid truncation while controlling costs
4. **Local Models**: Use Ollama for development to save costs and improve iteration speed
5. **Caching**: Cache schema-based prompts when possible
6. **Batch Processing**: Process multiple forms in parallel when possible

## Cost Optimization

1. **Use gpt-3.5-turbo** for development and testing
2. **Use local Ollama** models (completely free)
3. **Set reasonable max_tokens** to avoid unnecessary costs
4. **Cache responses** for identical requests
5. **Monitor usage** with provider dashboards
6. **Use cheaper providers** for non-critical workloads (Groq, Together AI)
