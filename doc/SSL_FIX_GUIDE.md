# SSL Certificate Issue Fix

## Problem

When using private/self-hosted LLM deployments with self-signed SSL certificates, you may encounter this error:

```
Failed to call OpenAI API: PKIX path building failed: 
sun.security.provider.certpath.SunCertPathBuilderException: 
unable to find valid certification path to requested target
```

## Solution

### Option 1: Skip SSL Verification (Quick Fix for Development)

Add to your `.env` file or environment variables:

```bash
LLM_SKIP_SSL_VERIFY=true
```

**Full example for private LLM**:
```bash
# .env file
LLM_BASE_URL=https://your-internal-llm.company.com/v1/chat/completions
LLM_API_KEY=your-api-key
LLM_MODEL=your-model-name
LLM_SKIP_SSL_VERIFY=true
```

Or as environment variables:
```bash
export LLM_BASE_URL="https://your-internal-llm.company.com/v1/chat/completions"
export LLM_API_KEY="your-api-key"
export LLM_MODEL="your-model-name"
export LLM_SKIP_SSL_VERIFY=true
```

### Option 2: Import the Certificate (Production Recommended)

For production environments, import the certificate into Java's trust store:

```bash
# Get the certificate
openssl s_client -showcerts -connect your-internal-llm.company.com:443 </dev/null 2>/dev/null | \
  openssl x509 -outform PEM > llm-cert.pem

# Import to Java keystore
sudo keytool -import -alias llm-cert -file llm-cert.pem \
  -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit

# Verify
keytool -list -keystore $JAVA_HOME/lib/security/cacerts | grep llm-cert
```

## Security Warning

⚠️ **WARNING**: `LLM_SKIP_SSL_VERIFY=true` disables SSL certificate verification!

**Only use this for**:
- Development environments
- Testing with self-signed certificates
- Trusted internal networks

**Never use in production** unless you fully understand the security implications.

## Verification

After configuration, run your example:

```bash
cd smart-form-example
mvn exec:java -Dexec.mainClass="com.fanyamin.example.RealLlmExample"
```

You should see:
```
✓ Loaded 5 configuration(s) from: /path/to/.env
⚠️  WARNING: SSL certificate verification is disabled!
   This should only be used for development/testing with self-signed certificates.
ℹ️  Using real LLM client
   Configuration loaded from .env file
   Model: your-model-name
```

## Accepted Values for LLM_SKIP_SSL_VERIFY

The following values are accepted (case-insensitive):
- `true`, `yes`, `1` → Skip SSL verification
- `false`, `no`, `0` → Enable SSL verification (default)

## Alternative: Use HTTP Instead of HTTPS

If your LLM supports it, use HTTP (no SSL):

```bash
LLM_BASE_URL=http://your-internal-llm.company.com/v1/chat/completions
```

This avoids SSL entirely, but should only be used in secure internal networks.

## Troubleshooting

### Still Getting Certificate Errors?

1. **Verify .env is loaded**:
   ```bash
   grep LLM_SKIP_SSL_VERIFY .env
   ```

2. **Check the value**:
   Ensure it's `true`, not `True` or `TRUE` (though all should work)

3. **Rebuild the project**:
   ```bash
   ./build.sh
   ```

4. **Check for typos**:
   The variable name is `LLM_SKIP_SSL_VERIFY`, not `LLM_SKIP_SSL_VERIFICATION`

### Certificate Error Even with Skip Enabled?

Make sure you're using the latest build:
```bash
./build.sh
cd smart-form-example
mvn clean package
```

## Code Usage

You can also set this programmatically:

```java
import com.fanyamin.instructor.llm.OpenAiLlmClient;

LlmClient client = new OpenAiLlmClient.Builder()
    .apiKey("your-key")
    .apiUrl("https://your-internal-llm.company.com/v1/chat/completions")
    .model("your-model")
    .skipSslVerify(true)  // Disable SSL verification
    .build();
```

## Production Best Practices

1. ✅ **Import certificates** into Java trust store
2. ✅ Use environment-specific configuration
3. ✅ Never skip SSL in production
4. ✅ Use proper certificate management
5. ✅ Monitor certificate expiration
6. ✅ Rotate certificates regularly

## Related Documentation

- `env.example` - Configuration template with SSL skip example
- `LLM_QUICK_REFERENCE.md` - Quick configuration reference
- `llm/README.md` - Complete LLM configuration guide

