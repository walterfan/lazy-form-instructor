# Configuration Guide for Smart Form Web

## Environment Variables

The Smart Form Web application uses **custom environment variables** that are different from Spring AI's default configuration.

### Our Configuration (LLM_*)

We use the `LlmClientFactory` which reads from:

```bash
# Required
LLM_API_KEY=your-api-key

# Optional (with defaults)
LLM_BASE_URL=https://api.openai.com/v1/chat/completions
LLM_MODEL=gpt-4-turbo-preview
LLM_TEMPERATURE=0.7
LLM_MAX_TOKENS=4096
LLM_SKIP_SSL_VERIFY=false
LLM_DEBUG=false
```

### Why Not Spring AI Properties?

Spring Boot + Spring AI typically uses:
```properties
spring.ai.openai.api-key=xxx
spring.ai.openai.chat.options.model=xxx
```

However, we've **disabled Spring AI auto-configuration** because:

1. ✅ We use our own `OpenAiLlmClient` and `LlmClientFactory`
2. ✅ Our implementation supports more LLM providers (not just OpenAI)
3. ✅ We have custom features (SSL skip, debug mode, thinking mode filtering)
4. ✅ Consistent configuration across all modules (command-line examples use same vars)

### Setting Up .env File

Create a `.env` file in the **project root** (not in `smart-form-web/`):

```
/Users/walter.fan/workspace/walter/smart-form-instructor/
├── .env                          ← Put it here!
├── smart-form-instructor/
├── smart-form-example/
└── smart-form-web/
```

Example `.env` file:

```bash
# OpenAI
LLM_API_KEY=sk-proj-xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
LLM_MODEL=gpt-4-turbo-preview

# Or Azure OpenAI
# LLM_API_KEY=your-azure-key
# LLM_BASE_URL=https://your-resource.openai.azure.com/openai/deployments/your-deployment/chat/completions?api-version=2024-02-01

# Or Ollama (local)
# LLM_BASE_URL=http://localhost:11434/v1/chat/completions
# LLM_MODEL=llama2

# Or Private LLM
# LLM_API_KEY=your-key
# LLM_BASE_URL=https://your-llm-server.com/v1/chat/completions
# LLM_SKIP_SSL_VERIFY=true  # Only for self-signed certs!
# LLM_DEBUG=true            # Enable request/response logging
```

### Verification

Check if configuration is loaded:

```bash
cd smart-form-web
mvn spring-boot:run
```

Look for this output:
```
ℹ️  Using real LLM client
   Configuration loaded from .env file
   Model: gpt-4-turbo-preview
```

If you see:
```
ℹ️  Using mock LLM client (no LLM_API_KEY or custom LLM_BASE_URL configured)
```

Then the `.env` file is not being read. Make sure:
1. `.env` file is in the project root
2. File has correct syntax (no quotes needed for values)
3. No spaces around `=` sign

### Troubleshooting

#### Error: "OpenAI API key must be set"

**Solution**: We've disabled Spring AI auto-configuration. This error should not appear with our current setup.

If you still see it, verify `SmartFormWebApplication.java` has:

```java
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration.class
})
```

#### Error: "Failed to create LLM client"

**Possible causes:**
1. `.env` file not found or not in project root
2. Invalid `LLM_API_KEY`
3. Network issues connecting to LLM service
4. Invalid `LLM_BASE_URL` format

**Debug steps:**
1. Enable debug logging: `LLM_DEBUG=true`
2. Check `.env` file location and contents
3. Test API key with curl:
   ```bash
   curl https://api.openai.com/v1/models \
     -H "Authorization: Bearer $LLM_API_KEY"
   ```

#### MockLlmClient being used instead of real LLM

**This happens when:**
- No `LLM_API_KEY` is set
- AND no custom `LLM_BASE_URL` is set

**Solution:**
- For cloud LLMs: Set `LLM_API_KEY`
- For local LLMs (Ollama): Set `LLM_BASE_URL` (API key optional)

### Using Spring AI ChatClient (Alternative)

If you prefer to use Spring AI's auto-configuration:

1. **Remove the exclusion** in `SmartFormWebApplication.java`:
```java
@SpringBootApplication  // Remove exclude parameter
```

2. **Create application.properties**:
```properties
spring.ai.openai.api-key=${LLM_API_KEY}
spring.ai.openai.chat.options.model=${LLM_MODEL:gpt-4-turbo-preview}
spring.ai.openai.chat.options.temperature=${LLM_TEMPERATURE:0.7}
```

3. **Update SmartFormConfig.java**:
```java
@Bean
public LlmClient llmClient(ChatClient.Builder chatClientBuilder) {
    ChatClient chatClient = chatClientBuilder.build();
    return new SpringAiLlmClient(chatClient);
}
```

However, we **recommend keeping the current setup** for consistency and flexibility.

## Configuration Priority

1. **System environment variables** (highest priority)
2. **`.env` file** in project root
3. **Default values** in `LlmConfig`

Example:
```bash
# Set via environment
export LLM_API_KEY=sk-env-key

# Or via .env file
echo "LLM_API_KEY=sk-file-key" > .env

# Or via command line
LLM_API_KEY=sk-inline-key mvn spring-boot:run
```

## Testing Configuration

Create a simple test:

```bash
# Set environment variable
export LLM_API_KEY=sk-test-123
export LLM_MODEL=gpt-4

# Run application
cd smart-form-web
mvn spring-boot:run

# Check startup logs for:
# "✓ Loaded X configuration(s) from: ..."
```

## Production Configuration

For production deployments:

1. **Use environment variables** (not `.env` file)
2. **Store secrets securely** (AWS Secrets Manager, Azure Key Vault, etc.)
3. **Set appropriate values**:
   ```bash
   LLM_API_KEY=<from-secrets-manager>
   LLM_BASE_URL=https://api.openai.com/v1/chat/completions
   LLM_MODEL=gpt-4-turbo-preview
   LLM_TEMPERATURE=0.7
   LLM_MAX_TOKENS=4096
   LLM_DEBUG=false  # Disable in production!
   ```

## Summary

✅ Use `LLM_*` environment variables (not `spring.ai.*`)
✅ Put `.env` file in project root
✅ Spring AI auto-configuration is disabled
✅ We use our own `OpenAiLlmClient` via `LlmClientFactory`
✅ Configuration is consistent across all modules

