package com.fanyamin.instructor.llm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration helper for LLM clients.
 * Reads configuration from:
 * 1. Environment variables (highest priority)
 * 2. .env file in current directory or parent directories
 * 3. Default values (lowest priority)
 * 
 * .env file format:
 *   LLM_API_KEY=sk-...
 *   LLM_BASE_URL=https://api.openai.com/v1/chat/completions
 *   LLM_MODEL=gpt-4-turbo-preview
 *   # Comments are supported
 */
public class LlmConfig {

    // Environment variables
    public static final String ENV_LLM_API_KEY = "LLM_API_KEY";
    public static final String ENV_LLM_BASE_URL = "LLM_BASE_URL";
    public static final String ENV_LLM_MODEL = "LLM_MODEL";
    public static final String ENV_LLM_TEMPERATURE = "LLM_TEMPERATURE";
    public static final String ENV_LLM_MAX_TOKENS = "LLM_MAX_TOKENS";
    public static final String ENV_LLM_SKIP_SSL_VERIFY = "LLM_SKIP_SSL_VERIFY";
    public static final String ENV_LLM_DEBUG = "LLM_DEBUG";

    // Default values
    public static final String DEFAULT_OPENAI_BASE_URL = "https://api.openai.com/v1/chat/completions";
    public static final String DEFAULT_OPENAI_MODEL = "gpt-4-turbo-preview";

    public static final double DEFAULT_TEMPERATURE = 0.7;
    public static final int DEFAULT_MAX_TOKENS = 4096;
    public static final boolean DEFAULT_SKIP_SSL_VERIFY = false;
    public static final boolean DEFAULT_DEBUG = false;

    // Cached .env file properties
    private static Map<String, String> dotEnvCache = null;
    private static boolean dotEnvLoaded = false;

    /**
     * Get configuration value from environment variable, .env file, or default.
     * Priority: System env > .env file > default value
     */
    public static String getEnv(String key, String defaultValue) {
        // Try system environment variable first
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }

        // Try .env file
        value = getDotEnvValue(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }

        // Return default
        return defaultValue;
    }

    /**
     * Get double environment variable with fallback to default value.
     */
    public static double getEnvDouble(String key, double defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                System.err.println("Invalid double value for " + key + ": " + value + ", using default: " + defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * Get integer environment variable with fallback to default value.
     */
    public static int getEnvInt(String key, int defaultValue) {
        String value = getEnv(key, null);
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.err.println("Invalid integer value for " + key + ": " + value + ", using default: " + defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * Get boolean environment variable with fallback to default value.
     * Accepts: true/false, yes/no, 1/0 (case-insensitive)
     */
    public static boolean getEnvBoolean(String key, boolean defaultValue) {
        String value = getEnv(key, null);
        if (value != null && !value.isEmpty()) {
            value = value.trim().toLowerCase();
            if ("true".equals(value) || "yes".equals(value) || "1".equals(value)) {
                return true;
            } else if ("false".equals(value) || "no".equals(value) || "0".equals(value)) {
                return false;
            } else {
                System.err.println("Invalid boolean value for " + key + ": " + value + ", using default: " + defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * Get value from .env file.
     */
    private static String getDotEnvValue(String key) {
        if (!dotEnvLoaded) {
            loadDotEnv();
        }
        return dotEnvCache != null ? dotEnvCache.get(key) : null;
    }

    /**
     * Load .env file from current directory or parent directories.
     * Searches up to 5 levels up from current directory.
     */
    private static synchronized void loadDotEnv() {
        if (dotEnvLoaded) {
            return;
        }
        
        dotEnvLoaded = true;
        dotEnvCache = new HashMap<>();

        Path dotEnvPath = findDotEnvFile();
        if (dotEnvPath == null) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(dotEnvPath.toFile()))) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // Parse KEY=VALUE format
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();
                    
                    // Remove quotes if present
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    
                    dotEnvCache.put(key, value);
                } else {
                    System.err.println("Warning: Invalid .env file format at line " + lineNumber + ": " + line);
                }
            }
            
            if (!dotEnvCache.isEmpty()) {
                System.out.println("✓ Loaded " + dotEnvCache.size() + " configuration(s) from: " + dotEnvPath);
            }
        } catch (IOException e) {
            System.err.println("Warning: Failed to read .env file: " + e.getMessage());
        }
    }

    /**
     * Find .env file in current directory or parent directories.
     * Searches up to 5 levels up.
     */
    private static Path findDotEnvFile() {
        Path currentPath = Paths.get("").toAbsolutePath();
        
        // Search current directory and up to 5 parent directories
        for (int i = 0; i < 5; i++) {
            Path dotEnvPath = currentPath.resolve(".env");
            if (Files.exists(dotEnvPath) && Files.isRegularFile(dotEnvPath)) {
                return dotEnvPath;
            }
            
            Path parent = currentPath.getParent();
            if (parent == null) {
                break;
            }
            currentPath = parent;
        }
        
        return null;
    }

    /**
     * Check if .env file is loaded and has values.
     */
    public static boolean hasDotEnv() {
        if (!dotEnvLoaded) {
            loadDotEnv();
        }
        return dotEnvCache != null && !dotEnvCache.isEmpty();
    }

    /**
     * Reload .env file (useful for testing or if file changes).
     */
    public static synchronized void reloadDotEnv() {
        dotEnvLoaded = false;
        dotEnvCache = null;
        loadDotEnv();
    }
}

