# Quick Fix for 405 Method Not Allowed Error

## Problem
Your `.env` file has an incomplete endpoint URL:
```bash
LLM_BASE_URL=https://your-llm-server.com/api
```

This causes a "405 Method Not Allowed" error because the URL is missing the OpenAI-compatible endpoint path.

## Solution

Update your `.env` file to include the full endpoint path:

```bash
LLM_BASE_URL=https://your-llm-server.com/api/v1/chat/completions
```

### Full .env Configuration

```bash
# LLM configuration
LLM_API_KEY=sk-your-actual-key-here
LLM_BASE_URL=https://api.openai.com/v1/chat/completions
LLM_MODEL=gpt-4-turbo-preview
LLM_TEMPERATURE=0.7
LLM_MAX_TOKENS=8192
LLM_SKIP_SSL_VERIFY=false
```

### If That Doesn't Work

Your LLM server might use a different endpoint path. Try these alternatives:

**Option 1: Without /v1/**
```bash
LLM_BASE_URL=https://your-llm-server.com/api/chat/completions
```

**Option 2: With different API version**
```bash
LLM_BASE_URL=https://your-llm-server.com/api/v2/chat/completions
```

**Option 3: OpenAI-style path**
```bash
LLM_BASE_URL=https://your-llm-server.com/v1/chat/completions
```

### How to Find the Correct Endpoint

1. **Check your LLM provider's documentation** for the API endpoint

2. **Ask your team** about the correct OpenAI-compatible endpoint

3. **Test with curl**:
```bash
curl -X POST https://your-llm-server.com/api/v1/chat/completions \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "your-model-name",
    "messages": [{"role": "user", "content": "Hello"}]
  }'
```

If you get a 404 or 405, try different paths until you find the working one.

### Common Endpoint Patterns

| Provider | Endpoint Pattern |
|----------|------------------|
| OpenAI | `https://api.openai.com/v1/chat/completions` |
| Azure | `https://xxx.openai.azure.com/openai/deployments/xxx/chat/completions?api-version=2024-02-01` |
| Ollama | `http://localhost:11434/v1/chat/completions` |
| LM Studio | `http://localhost:1234/v1/chat/completions` |
| Generic | `https://your-server/api/v1/chat/completions` |

### After Fixing

1. Update your `.env` file
2. Run the diagnostic again:
```bash
cd lazy-form-example
mvn exec:java -Dexec.mainClass="com.fanyamin.example.ConfigDiagnostic"
```

3. Run the example:
```bash
cd ..
./run-demo.sh leave
```

You should see the warning go away and the example should work!
