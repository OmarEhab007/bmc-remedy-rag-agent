package com.bmc.rag.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for CredentialValidationConfig.
 */
class CredentialValidationConfigTest {

    @Test
    void validateCredentials_shouldPassInDevProfile() {
        // Given
        CredentialValidationConfig config = new CredentialValidationConfig();
        ReflectionTestUtils.setField(config, "activeProfile", "dev");
        ReflectionTestUtils.setField(config, "dbPassword", "weak");

        // When & Then - should not throw
        config.validateCredentials();
    }

    @Test
    void validateCredentials_shouldRejectMissingDatabasePassword() {
        // Given
        CredentialValidationConfig config = new CredentialValidationConfig();
        ReflectionTestUtils.setField(config, "activeProfile", "prod");
        ReflectionTestUtils.setField(config, "dbPassword", "");
        ReflectionTestUtils.setField(config, "jwkSetUri", "https://example.com/jwks");

        // When & Then
        assertThatThrownBy(() -> config.validateCredentials())
            .isInstanceOf(CredentialValidationConfig.SecurityConfigurationException.class)
            .hasMessageContaining("Database password is required");
    }

    @Test
    void validateCredentials_shouldRejectWeakPasswords() {
        // Given
        CredentialValidationConfig config = new CredentialValidationConfig();
        ReflectionTestUtils.setField(config, "activeProfile", "prod");
        ReflectionTestUtils.setField(config, "dbPassword", "password");
        ReflectionTestUtils.setField(config, "jwkSetUri", "https://example.com/jwks");

        // When & Then
        assertThatThrownBy(() -> config.validateCredentials())
            .isInstanceOf(CredentialValidationConfig.SecurityConfigurationException.class)
            .hasMessageContaining("weak/default value");
    }

    @Test
    void validateCredentials_shouldRejectCommonWeakPasswords() {
        // Given
        String[] weakPasswords = {"123456", "admin", "root", "test", "demo", "secret", "changeme", "default"};

        for (String weak : weakPasswords) {
            CredentialValidationConfig config = new CredentialValidationConfig();
            ReflectionTestUtils.setField(config, "activeProfile", "prod");
            ReflectionTestUtils.setField(config, "dbPassword", weak);
            ReflectionTestUtils.setField(config, "jwkSetUri", "https://example.com/jwks");

            // When & Then
            assertThatThrownBy(() -> config.validateCredentials())
                .isInstanceOf(CredentialValidationConfig.SecurityConfigurationException.class)
                .hasMessageContaining("weak/default value");
        }
    }

    @Test
    void validateCredentials_shouldRejectMissingJwtConfiguration() {
        // Given
        CredentialValidationConfig config = new CredentialValidationConfig();
        ReflectionTestUtils.setField(config, "activeProfile", "prod");
        ReflectionTestUtils.setField(config, "dbPassword", "ENC(encrypted)");
        ReflectionTestUtils.setField(config, "jwkSetUri", "");

        // When & Then
        assertThatThrownBy(() -> config.validateCredentials())
            .isInstanceOf(CredentialValidationConfig.SecurityConfigurationException.class)
            .hasMessageContaining("JWT configuration missing");
    }

    @Test
    void validateCredentials_shouldAcceptEncryptedPasswords() {
        // Given
        CredentialValidationConfig config = new CredentialValidationConfig();
        ReflectionTestUtils.setField(config, "activeProfile", "prod");
        ReflectionTestUtils.setField(config, "dbPassword", "ENC(encrypted_value)");
        ReflectionTestUtils.setField(config, "jwkSetUri", "https://example.com/jwks");

        // When & Then - should not throw
        config.validateCredentials();
    }

    @Test
    void validateCredentials_shouldAcceptStrongPasswords() {
        // Given
        CredentialValidationConfig config = new CredentialValidationConfig();
        ReflectionTestUtils.setField(config, "activeProfile", "prod");
        ReflectionTestUtils.setField(config, "dbPassword", "VeryStr0ng!P@ssw0rd#2024");
        ReflectionTestUtils.setField(config, "jwkSetUri", "https://example.com/jwks");

        // When & Then - should not throw
        config.validateCredentials();
    }

    @Test
    void validateCredentials_shouldWarnAboutShortApiKeys() {
        // Given
        CredentialValidationConfig config = new CredentialValidationConfig();
        ReflectionTestUtils.setField(config, "activeProfile", "prod");
        ReflectionTestUtils.setField(config, "dbPassword", "ENC(encrypted)");
        ReflectionTestUtils.setField(config, "zaiApiKey", "short");
        ReflectionTestUtils.setField(config, "jwkSetUri", "https://example.com/jwks");

        // When & Then - should not throw, just warn
        config.validateCredentials();
    }

    @Test
    void validateCredentials_shouldAcceptValidApiKeys() {
        // Given
        CredentialValidationConfig config = new CredentialValidationConfig();
        ReflectionTestUtils.setField(config, "activeProfile", "prod");
        ReflectionTestUtils.setField(config, "dbPassword", "ENC(encrypted)");
        ReflectionTestUtils.setField(config, "zaiApiKey", "valid-api-key-with-enough-length");
        ReflectionTestUtils.setField(config, "jwkSetUri", "https://example.com/jwks");

        // When & Then - should not throw
        config.validateCredentials();
    }

    @Test
    void validateCredentials_shouldAcceptEnvironmentVariableStyle() {
        // Given
        CredentialValidationConfig config = new CredentialValidationConfig();
        ReflectionTestUtils.setField(config, "activeProfile", "prod");
        ReflectionTestUtils.setField(config, "dbPassword", "STRONG_CRED_FROM_VAULT_X9k2m");
        ReflectionTestUtils.setField(config, "jwkSetUri", "https://example.com/jwks");

        // When & Then - should not throw
        config.validateCredentials();
    }

    @Test
    void validateCredentials_shouldHandleNullApiKey() {
        // Given
        CredentialValidationConfig config = new CredentialValidationConfig();
        ReflectionTestUtils.setField(config, "activeProfile", "prod");
        ReflectionTestUtils.setField(config, "dbPassword", "ENC(encrypted)");
        ReflectionTestUtils.setField(config, "zaiApiKey", null);
        ReflectionTestUtils.setField(config, "jwkSetUri", "https://example.com/jwks");

        // When & Then - should not throw
        config.validateCredentials();
    }

    @Test
    void validateCredentials_shouldHandleBlankApiKey() {
        // Given
        CredentialValidationConfig config = new CredentialValidationConfig();
        ReflectionTestUtils.setField(config, "activeProfile", "prod");
        ReflectionTestUtils.setField(config, "dbPassword", "ENC(encrypted)");
        ReflectionTestUtils.setField(config, "zaiApiKey", "");
        ReflectionTestUtils.setField(config, "jwkSetUri", "https://example.com/jwks");

        // When & Then - should not throw
        config.validateCredentials();
    }

    @Test
    void securityConfigurationException_shouldHaveCorrectMessage() {
        // Given
        String errorMessage = "Test error";

        // When
        CredentialValidationConfig.SecurityConfigurationException exception =
            new CredentialValidationConfig.SecurityConfigurationException(errorMessage);

        // Then
        assertThat(exception.getMessage()).contains("SECURITY CONFIGURATION ERROR");
        assertThat(exception.getMessage()).contains(errorMessage);
    }

    @Test
    void validateCredentials_shouldRejectPasswordContainingWeakTerms() {
        // Given
        CredentialValidationConfig config = new CredentialValidationConfig();
        ReflectionTestUtils.setField(config, "activeProfile", "prod");
        ReflectionTestUtils.setField(config, "dbPassword", "MyPassword123");
        ReflectionTestUtils.setField(config, "jwkSetUri", "https://example.com/jwks");

        // When & Then
        assertThatThrownBy(() -> config.validateCredentials())
            .isInstanceOf(CredentialValidationConfig.SecurityConfigurationException.class)
            .hasMessageContaining("weak/default value");
    }

    @Test
    void validateCredentials_shouldHandleNullJwkSetUri() {
        // Given
        CredentialValidationConfig config = new CredentialValidationConfig();
        ReflectionTestUtils.setField(config, "activeProfile", "prod");
        ReflectionTestUtils.setField(config, "dbPassword", "ENC(encrypted)");
        ReflectionTestUtils.setField(config, "jwkSetUri", null);

        // When & Then
        assertThatThrownBy(() -> config.validateCredentials())
            .isInstanceOf(CredentialValidationConfig.SecurityConfigurationException.class)
            .hasMessageContaining("JWT configuration missing");
    }

    @Test
    void validateCredentials_shouldHandleMixedCaseActiveProfile() {
        // Given
        CredentialValidationConfig config = new CredentialValidationConfig();
        ReflectionTestUtils.setField(config, "activeProfile", "DEV");
        ReflectionTestUtils.setField(config, "dbPassword", "weak");

        // When & Then - should not throw (case-insensitive dev check)
        config.validateCredentials();
    }
}
