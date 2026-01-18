# Smart Form Instructor - Complete Setup Guide

## Summary of Changes

Successfully implemented real LLM integration with flexible configuration support:

### ✅ Completed Features

1. **OpenAI-Compatible LLM Client**
   - Single client works with OpenAI, Azure OpenAI, Ollama, LM Studio, vLLM, and more
   - Smart fallback to mock client when LLM is not configured

2. **Flexible Configuration** (Priority Order)
   - System environment variables (highest)
   - `.env` file (automatic discovery)
   - Default values (lowest)

3. **Updated Examples**
   - `LeaveRequestExample` - Auto-detects and uses real LLM or falls back to mock
   - `TaskRequestExample` - Auto-detects and uses real LLM or falls back to mock
   - `RealLlmExample` - Dedicated example for real LLM usage

4. **Comprehensive Documentation**
   - `llm/README.md` - Complete guide with all configuration options
   - `LLM_QUICK_REFERENCE.md` - Quick reference card
   - `LLM_INTEGRATION.md` - Integration summary
   - `env.example` - Configuration template

## Quick Start

### Option 1: Run with Mock LLM (No Setup Required)

```bash
./build.sh
./run-demo.sh all
```

Output:
```
ℹ️  Using mock LLM client (no LLM_API_KEY configured)
   To use real LLM, set LLM_API_KEY environment variable or create .env file
```

### Option 2: Run with Real OpenAI

```bash
# Create .env file
cat > .env << EOF
LLM_API_KEY=sk-your-actual-key-here
LLM_MODEL=gpt-4-turbo-preview
EOF

# Build and run
./build.sh
./run-demo.sh all
```

Output:
```
ℹ️  Using real LLM client
   Configuration loaded from .env file
   Model: gpt-4-turbo-preview
```

### Option 3: Run with Local Ollama

```bash
# Terminal 1: Start Ollama
ollama serve

# Terminal 2: Configure and run
cat > .env << EOF
LLM_BASE_URL=http://localhost:11434/v1/chat/completions
LLM_MODEL=llama2
EOF

./build.sh
./run-demo.sh all
```

## Configuration Options

### Environment Variables

```bash
export LLM_API_KEY="sk-..."           # API key (required for cloud)
export LLM_BASE_URL="https://..."     # API endpoint (optional)
export LLM_MODEL="gpt-4-turbo-preview" # Model name (optional)
export LLM_TEMPERATURE="0.7"          # Temperature (optional)
export LLM_MAX_TOKENS="4096"          # Max tokens (optional)
```

### .env File

```bash
# Copy example
cp env.example .env

# Edit with your configuration
vim .env
```

The `.env` file is automatically searched in:
- Current directory
- Up to 5 parent directories

**Security:** `.env` is git-ignored by default

## Available Examples

### 1. Leave Request Example

Demonstrates multi-day leave request with:
- Date calculations
- Context-aware field extraction
- Business rule validation
- Dependency handling

```bash
cd smart-form-example
mvn exec:java -Dexec.mainClass="com.fanyamin.example.LeaveRequestExample"
```

### 2. Task Request Example

Demonstrates task creation with:
- Priority inference from keywords
- Schedule time calculation
- Deadline extraction
- Confidence scoring

```bash
cd smart-form-example
mvn exec:java -Dexec.mainClass="com.fanyamin.example.TaskRequestExample"
```

### 3. Real LLM Example

Dedicated example for testing real LLM integration:

```bash
cd smart-form-example
mvn exec:java -Dexec.mainClass="com.fanyamin.example.RealLlmExample"
```

## Code Usage

### Basic Integration

```java
import com.fanyamin.SmartFormInstructor;
import com.fanyamin.instructor.api.*;
import com.fanyamin.instructor.llm.*;

// Auto-configure from environment or .env file
LlmClient client = LlmClientFactory.createFromEnvironment();

// Create instructor
SmartFormInstructor instructor = new SmartFormInstructor(client);

// Parse form
ParsingRequest request = new ParsingRequest(schemaJson, userInput, context);
ParsingResult result = instructor.parse(request);
```

### With Explicit Configuration

```java
// Build custom client
LlmClient client = new OpenAiLlmClient.Builder()
    .apiKey("sk-...")
    .model("gpt-4-turbo-preview")
    .temperature(0.7)
    .maxTokens(4096)
    .build();

SmartFormInstructor instructor = new SmartFormInstructor(client);
```

### With Mock for Testing

```java
// Create mock client
MockLlmClient mock = LlmClientFactory.createMock();
mock.setMockResponse("{...}");

SmartFormInstructor instructor = new SmartFormInstructor(mock);
```

## Configuration Priority

The library reads configuration in this order:

1. **System Environment Variables** (highest priority)
   ```bash
   export LLM_API_KEY="sk-..."
   ```

2. **.env File** (in current or parent directories)
   ```bash
   LLM_API_KEY=sk-...
   ```

3. **Default Values** (lowest priority)
   - `LLM_BASE_URL`: `https://api.openai.com/v1/chat/completions`
   - `LLM_MODEL`: `gpt-4-turbo-preview`
   - `LLM_TEMPERATURE`: `0.7`
   - `LLM_MAX_TOKENS`: `4096`

## Provider-Specific Setup

### OpenAI
```bash
LLM_API_KEY=sk-...
LLM_MODEL=gpt-4-turbo-preview
```

### Azure OpenAI
```bash
LLM_API_KEY=your-azure-key
LLM_BASE_URL=https://your-resource.openai.azure.com/openai/deployments/gpt-4/chat/completions?api-version=2024-02-01
LLM_MODEL=gpt-4
```

### Ollama (Local)
```bash
LLM_BASE_URL=http://localhost:11434/v1/chat/completions
LLM_MODEL=llama2
```

### LM Studio
```bash
LLM_BASE_URL=http://localhost:1234/v1/chat/completions
LLM_MODEL=local-model
```

## Troubleshooting

### Examples Use Mock Instead of Real LLM

Check configuration:
```bash
# Verify environment variables
env | grep LLM

# Check .env file
cat .env

# Test in Java
import com.fanyamin.instructor.llm.LlmConfig;
System.out.println("Has .env: " + LlmConfig.hasDotEnv());
System.out.println("API Key: " + (LlmConfig.getEnv("LLM_API_KEY", null) != null));
```

### "OpenAI API error: 401 - invalid_api_key"

Your API key is invalid or expired:
1. Check your OpenAI account
2. Generate a new API key
3. Update `.env` or environment variable

### Connection to Ollama Fails

1. Ensure Ollama is running:
   ```bash
   curl http://localhost:11434/api/tags
   ```

2. Use correct endpoint:
   ```bash
   LLM_BASE_URL=http://localhost:11434/v1/chat/completions
   ```

3. Verify model is pulled:
   ```bash
   ollama list
   ollama pull llama2  # if not present
   ```

## Project Structure

```
smart-form-instructor/
├── smart-form-instructor/          # Core library
│   └── src/main/java/com/fanyamin/instructor/
│       ├── api/                    # Public API
│       ├── llm/                    # LLM clients
│       │   ├── LlmClient.java
│       │   ├── OpenAiLlmClient.java
│       │   ├── SpringAiLlmClient.java
│       │   ├── MockLlmClient.java
│       │   ├── LlmConfig.java      # Configuration loader
│       │   ├── LlmClientFactory.java
│       │   └── README.md           # Complete guide
│       ├── schema/                 # Schema validation
│       └── SmartFormInstructor.java
├── smart-form-example/             # Examples
│   └── src/main/java/com/fanyamin/example/
│       ├── LeaveRequestExample.java
│       ├── TaskRequestExample.java
│       └── RealLlmExample.java
├── env.example                     # Configuration template
├── build.sh                        # Build script
├── run-demo.sh                     # Demo runner
└── LLM_QUICK_REFERENCE.md         # This file
```

## Documentation

- **Complete Guide**: `smart-form-instructor/src/main/java/com/fanyamin/instructor/llm/README.md`
- **Quick Reference**: `LLM_QUICK_REFERENCE.md`
- **Integration Guide**: `LLM_INTEGRATION.md`
- **Main README**: `README.md`

## Security Best Practices

1. ✅ `.env` file is git-ignored
2. ✅ Never commit API keys
3. ✅ Use environment variables in production
4. ✅ Rotate keys regularly
5. ✅ Consider local Ollama for sensitive data

## Cost Optimization

1. Use **mock client** for development/testing (free)
2. Use **Ollama** for local development (free)
3. Use **gpt-3.5-turbo** for cheaper production
4. Use **gpt-4** only when quality is critical
5. Set appropriate `LLM_MAX_TOKENS` to control costs

## Next Steps

1. ✅ Build the project: `./build.sh`
2. ✅ Test with mock: `./run-demo.sh all`
3. ⚙️ Configure real LLM: Create `.env` file
4. 🚀 Test with real LLM: Run examples again
5. 📖 Read full documentation in `llm/README.md`

## Support

For issues or questions:
- Check the comprehensive `llm/README.md`
- Review troubleshooting section above
- Verify configuration with `LlmConfig.hasDotEnv()`
- Test connectivity to LLM endpoint

