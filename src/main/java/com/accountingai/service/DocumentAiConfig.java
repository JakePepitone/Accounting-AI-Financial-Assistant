package com.accountingai.service;

import java.time.Duration;
import java.util.Locale;

/**
 * Runtime configuration for document AI analysis.
 *
 * <p>Local analysis is the default. Set {@code ACCOUNTING_AI_AI_PROVIDER=openai}
 * and provide an API key to enable the OpenAI-compatible remote analyzer.</p>
 */
public class DocumentAiConfig {

    private static final String DEFAULT_PROVIDER = "local";
    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    private static final String DEFAULT_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final int DEFAULT_TIMEOUT_SECONDS = 20;

    private final String provider;
    private final String apiKey;
    private final String model;
    private final String endpoint;
    private final Duration timeout;

    /**
     * Creates config from JVM system properties and environment variables.
     */
    public DocumentAiConfig() {
        this(
                readSetting("accountingai.ai.provider", "ACCOUNTING_AI_AI_PROVIDER", DEFAULT_PROVIDER),
                firstNonBlank(
                        readSetting("accountingai.ai.apiKey", "ACCOUNTING_AI_API_KEY", null),
                        readSetting("openai.apiKey", "OPENAI_API_KEY", null)),
                readSetting("accountingai.ai.model", "ACCOUNTING_AI_MODEL", DEFAULT_MODEL),
                readSetting("accountingai.ai.endpoint", "ACCOUNTING_AI_API_URL", DEFAULT_ENDPOINT),
                Duration.ofSeconds(readIntSetting(
                        "accountingai.ai.timeoutSeconds",
                        "ACCOUNTING_AI_AI_TIMEOUT_SECONDS",
                        DEFAULT_TIMEOUT_SECONDS)));
    }

    /**
     * Explicit constructor for tests and dependency injection.
     */
    public DocumentAiConfig(String provider, String apiKey, String model,
                            String endpoint, Duration timeout) {
        this.provider = normalize(provider, DEFAULT_PROVIDER).toLowerCase(Locale.ROOT);
        this.apiKey = blankToNull(apiKey);
        this.model = normalize(model, DEFAULT_MODEL);
        this.endpoint = normalize(endpoint, DEFAULT_ENDPOINT);
        this.timeout = timeout == null || timeout.isNegative() || timeout.isZero()
                ? Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS)
                : timeout;
    }

    public boolean isRemoteAiEnabled() {
        return "openai".equals(provider) && apiKey != null;
    }

    public String getProvider() {
        return provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public Duration getTimeout() {
        return timeout;
    }

    private static String readSetting(String propertyName, String envName, String fallback) {
        return firstNonBlank(System.getProperty(propertyName), System.getenv(envName), fallback);
    }

    private static int readIntSetting(String propertyName, String envName, int fallback) {
        String value = readSetting(propertyName, envName, null);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String normalize(String value, String fallback) {
        String normalized = blankToNull(value);
        return normalized == null ? fallback : normalized;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
