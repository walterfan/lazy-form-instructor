package com.fanyamin.example;

import com.fanyamin.instructor.llm.LlmConfig;

/**
 * Diagnostic tool to check LLM configuration.
 * Run this to verify your .env file settings.
 */
public class ConfigDiagnostic {

    public static void main(String[] args) {
        System.out.println("=== LLM Configuration Diagnostic ===\n");

        // Check if .env file is loaded
        if (LlmConfig.hasDotEnv()) {
            System.out.println("✓ .env file found and loaded\n");
        } else {
            System.out.println("⚠️  No .env file found\n");
        }

        // Display all LLM configuration
        System.out.println("Configuration Values:");
        System.out.println("─".repeat(80));

        String apiKey = LlmConfig.getEnv(LlmConfig.ENV_LLM_API_KEY, null);
        String baseUrl = LlmConfig.getEnv(LlmConfig.ENV_LLM_BASE_URL, null);
        String model = LlmConfig.getEnv(LlmConfig.ENV_LLM_MODEL, null);
        String temperature = LlmConfig.getEnv(LlmConfig.ENV_LLM_TEMPERATURE, null);
        String maxTokens = LlmConfig.getEnv(LlmConfig.ENV_LLM_MAX_TOKENS, null);
        String skipSsl = LlmConfig.getEnv(LlmConfig.ENV_LLM_SKIP_SSL_VERIFY, null);

        System.out.println("LLM_API_KEY: " + maskValue(apiKey));
        System.out.println("LLM_BASE_URL: " + (baseUrl != null ? baseUrl : "(not set - will use default)"));
        System.out.println("LLM_MODEL: " + (model != null ? model : "(not set - will use default)"));
        System.out.println("LLM_TEMPERATURE: " + (temperature != null ? temperature : "(not set - will use default)"));
        System.out.println("LLM_MAX_TOKENS: " + (maxTokens != null ? maxTokens : "(not set - will use default)"));
        System.out.println("LLM_SKIP_SSL_VERIFY: " + (skipSsl != null ? skipSsl : "(not set - will use default)"));

        System.out.println("\n" + "─".repeat(80));
        System.out.println("\nEndpoint Analysis:");
        System.out.println("─".repeat(80));

        if (baseUrl != null) {
            analyzeEndpoint(baseUrl);
        } else {
            System.out.println("Using default OpenAI endpoint: " + LlmConfig.DEFAULT_OPENAI_BASE_URL);
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("\nCommon Endpoint Formats:");
        System.out.println("─".repeat(80));
        System.out.println("OpenAI:");
        System.out.println("  https://api.openai.com/v1/chat/completions");
        System.out.println("\nAzure OpenAI:");
        System.out.println("  https://YOUR-RESOURCE.openai.azure.com/openai/deployments/YOUR-DEPLOYMENT/chat/completions?api-version=2024-02-01");
        System.out.println("\nOllama:");
        System.out.println("  http://localhost:11434/v1/chat/completions");
        System.out.println("\nLM Studio:");
        System.out.println("  http://localhost:1234/v1/chat/completions");
        System.out.println("\nPrivate OpenAI-compatible LLM:");
        System.out.println("  https://your-server.com/v1/chat/completions");
        System.out.println("  OR");
        System.out.println("  https://your-server.com/api/v1/chat/completions");
        System.out.println("\n" + "=".repeat(80));
    }

    private static void analyzeEndpoint(String url) {
        System.out.println("Current endpoint: " + url);
        System.out.println();

        // Check for common issues
        if (!url.contains("/chat/completions")) {
            System.out.println("⚠️  WARNING: Endpoint doesn't contain '/chat/completions'");
            System.out.println("   Common issue: Missing the OpenAI-compatible endpoint path");
            System.out.println();
            
            if (url.endsWith("/")) {
                System.out.println("💡 Try: " + url + "v1/chat/completions");
            } else {
                System.out.println("💡 Try: " + url + "/v1/chat/completions");
            }
            System.out.println();
        }

        if (url.contains("/api/chat") && !url.contains("/v1/")) {
            System.out.println("⚠️  WARNING: Endpoint might be using non-standard API path");
            System.out.println("   This might be a provider-specific endpoint");
            System.out.println();
        }

        if (url.startsWith("http://") && !url.contains("localhost") && !url.contains("127.0.0.1")) {
            System.out.println("⚠️  WARNING: Using unencrypted HTTP for remote server");
            System.out.println("   Consider using HTTPS for security");
            System.out.println();
        }

        if (url.contains(":443") || url.contains(":8443")) {
            System.out.println("ℹ️  Note: Using explicit HTTPS port");
            System.out.println("   This is fine, but sometimes the port can be omitted");
            System.out.println();
        }

        // Parse the endpoint
        try {
            java.net.URI uri = new java.net.URI(url);
            System.out.println("Parsed components:");
            System.out.println("  Protocol: " + uri.getScheme());
            System.out.println("  Host: " + uri.getHost());
            System.out.println("  Port: " + (uri.getPort() > 0 ? uri.getPort() : "(default)"));
            System.out.println("  Path: " + uri.getPath());
            if (uri.getQuery() != null) {
                System.out.println("  Query: " + uri.getQuery());
            }
        } catch (Exception e) {
            System.out.println("⚠️  WARNING: Invalid URL format: " + e.getMessage());
        }
    }

    private static String maskValue(String value) {
        if (value == null || value.isEmpty()) {
            return "(not set)";
        }
        if (value.length() <= 8) {
            return "***";
        }
        return value.substring(0, 4) + "..." + value.substring(value.length() - 4) + " (" + value.length() + " chars)";
    }
}

