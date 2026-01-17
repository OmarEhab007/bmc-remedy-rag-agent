package com.bmc.rag.api.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Validates critical credentials at application startup.
 * Ensures no plaintext or default credentials are used in production.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "security.enabled", havingValue = "true", matchIfMissing = true)
public class CredentialValidationConfig {

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${remedy.password:}")
    private String remedyPassword;

    @Value("${zai.api-key:}")
    private String zaiApiKey;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String jwkSetUri;

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    // Common weak passwords to reject
    private static final String[] WEAK_PASSWORDS = {
        "password", "123456", "admin", "root", "test", "demo",
        "ragpassword", "secret", "changeme", "default"
    };

    @PostConstruct
    public void validateCredentials() {
        boolean isDevProfile = "dev".equalsIgnoreCase(activeProfile);

        if (isDevProfile) {
            log.warn("=== DEVELOPMENT MODE ===");
            log.warn("Credential validation is relaxed in dev profile.");
            log.warn("DO NOT use dev profile in production!");
            return;
        }

        log.info("Validating production credentials...");

        // Validate database password
        validatePassword("Database password", dbPassword, true);

        // Validate JWT configuration
        if (jwkSetUri == null || jwkSetUri.isBlank()) {
            throw new SecurityConfigurationException(
                "JWT configuration missing in production. " +
                "Set spring.security.oauth2.resourceserver.jwt.jwk-set-uri or use issuer-uri"
            );
        }

        // Validate API keys if services are enabled
        if (zaiApiKey != null && !zaiApiKey.isBlank()) {
            if (zaiApiKey.length() < 20) {
                log.warn("Z.AI API key appears to be too short - verify it's correct");
            }
        }

        log.info("Production credential validation passed");
    }

    private void validatePassword(String name, String password, boolean required) {
        if (password == null || password.isBlank()) {
            if (required) {
                throw new SecurityConfigurationException(
                    name + " is required in production. " +
                    "Use ENC(encrypted_value) for Jasypt encryption or environment variables."
                );
            }
            return;
        }

        // Check for weak passwords
        String lowerPassword = password.toLowerCase();
        for (String weak : WEAK_PASSWORDS) {
            if (lowerPassword.equals(weak) || lowerPassword.contains(weak)) {
                throw new SecurityConfigurationException(
                    name + " contains a weak/default value. " +
                    "Use strong credentials in production."
                );
            }
        }

        // Check if encrypted with Jasypt (starts with ENC())
        if (!password.startsWith("ENC(") && !isEnvironmentVariable(password)) {
            log.warn("{} appears to be plaintext. Consider using ENC() for Jasypt encryption.", name);
        }
    }

    private boolean isEnvironmentVariable(String value) {
        // Check if value looks like it came from an environment variable
        // (typically environment variables are all caps with underscores)
        return value != null && value.matches("^[A-Z][A-Z0-9_]*$");
    }

    /**
     * Exception thrown when security configuration is invalid.
     */
    public static class SecurityConfigurationException extends RuntimeException {
        public SecurityConfigurationException(String message) {
            super("SECURITY CONFIGURATION ERROR: " + message);
        }
    }
}
