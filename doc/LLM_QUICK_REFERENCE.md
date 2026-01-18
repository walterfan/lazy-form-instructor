# LLM Client Quick Reference

## Configuration Methods

The library supports three ways to configure LLM clients (in priority order):

1. **Environment Variables** (highest priority)
2. **.env file** (in current or parent directories)
3. **Default values** (lowest priority)

## Environment Variables

```bash
# Required for cloud services
export LLM_API_KEY="sk-..."

# Optional (with defaults)
export LLM_BASE_URL="https://api.openai.com/v1/chat/completions"  # API endpoint
export LLM_MODEL="gpt-4-turbo-preview"                             # Model name
export LLM_TEMPERATURE="0.7"                                        # 0.0-1.0
export LLM_MAX_TOKENS="4096"                                        # Max response length
```

## Using .env File

Create a `.env` file in your project root:

```bash
# Copy from example
cp env.example .env

# Edit with your configuration
LLM_API_KEY=sk-...
LLM_MODEL=gpt-4-turbo-preview
LLM_TEMPERATURE=0.7
```

The library will automatically search for `.env` file in:
- Current directory
- Up to 5 parent directories

**Note:** `.env` files are git-ignored by default for security.

## Provider Setup

### OpenAI
```bash
export LLM_API_KEY="sk-..."
export LLM_MODEL="gpt-4-turbo-preview"
```

### Azure OpenAI
```bash
export LLM_API_KEY="your-azure-key"
export LLM_BASE_URL="https://your-resource.openai.azure.com/openai/deployments/your-deployment/chat/completions?api-version=2024-02-01"
export LLM_MODEL="gpt-4"
```

### Ollama (Local)
```bash
# Start: ollama serve
export LLM_BASE_URL="http://localhost:11434/v1/chat/completions"
export LLM_MODEL="llama2"
```

### LM Studio
```bash
export LLM_BASE_URL="http://localhost:1234/v1/chat/completions"
export LLM_MODEL="local-model"
```

## Code Examples

### Basic Usage
```java
// Auto-configure from environment
LlmClient client = LlmClientFactory.createFromEnvironment();
SmartFormInstructor instructor = new SmartFormInstructor(client);
```

### With Builder
```java
LlmClient client = new OpenAiLlmClient.Builder()
    .apiKey("sk-...")
    .model("gpt-4-turbo-preview")
    .temperature(0.7)
    .build();
```

### Mock for Testing
```java
MockLlmClient mock = LlmClientFactory.createMock();
mock.setMockResponse("{...}");
```

## Running Examples

### Mock (No API Key)
```bash
./run-demo.sh all
```

### Real OpenAI
```bash
export LLM_API_KEY="sk-..."
cd smart-form-example
mvn exec:java -Dexec.mainClass="com.fanyamin.example.RealLlmExample"
```

### Real Ollama
```bash
ollama serve  # Terminal 1
export LLM_BASE_URL="http://localhost:11434/v1/chat/completions"
export LLM_MODEL="llama2"
mvn exec:java -Dexec.mainClass="com.fanyamin.example.RealLlmExample"  # Terminal 2
```

## Recommended Models

| Use Case | Model | Provider |
|----------|-------|----------|
| Production | `gpt-4-turbo-preview` | OpenAI |
| Development | `gpt-3.5-turbo` | OpenAI |
| Local/Free | `llama2`, `mistral` | Ollama |
| Fast/Cheap | `gpt-3.5-turbo` | OpenAI or Groq |

## Troubleshooting

| Error | Solution |
|-------|----------|
| "No LLM provider configured" | Set `LLM_API_KEY` or `LLM_BASE_URL` |
| "API key is required" | `export LLM_API_KEY="..."` |
| Connection to Ollama fails | Check `ollama serve` is running |
| Rate limit | Use cheaper model or local Ollama |

## Full Documentation

See `smart-form-instructor/src/main/java/com/fanyamin/instructor/llm/README.md`

