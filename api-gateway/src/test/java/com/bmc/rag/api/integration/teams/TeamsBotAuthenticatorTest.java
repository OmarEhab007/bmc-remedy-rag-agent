package com.bmc.rag.api.integration.teams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for TeamsBotAuthenticator.
 */
@ExtendWith(MockitoExtension.class)
class TeamsBotAuthenticatorTest {

    @Mock
    private TeamsBotConfig config;

    private TeamsBotAuthenticator authenticator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        authenticator = new TeamsBotAuthenticator(config);

        // Use lenient stubbing for common mocks
        lenient().when(config.isEnabled()).thenReturn(true);
        lenient().when(config.getAppId()).thenReturn("app-123");
        lenient().when(config.getTenantId()).thenReturn("");
    }

    @Test
    void validateRequest_shouldAllowWhenAuthDisabled() {
        // Given
        lenient().when(config.isEnabled()).thenReturn(false);
        JsonNode activity = objectMapper.createObjectNode();

        // When
        boolean result = authenticator.validateRequest(null, activity);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void validateRequest_shouldRejectMissingAuthHeader() {
        // Given
        JsonNode activity = objectMapper.createObjectNode();

        // When
        boolean result = authenticator.validateRequest(null, activity);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void validateRequest_shouldRejectInvalidAuthHeaderFormat() {
        // Given
        JsonNode activity = objectMapper.createObjectNode();

        // When
        boolean result = authenticator.validateRequest("InvalidFormat", activity);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void validateRequest_shouldRejectInvalidJwtFormat() {
        // Given
        JsonNode activity = objectMapper.createObjectNode();
        String invalidToken = "Bearer invalid.token";

        // When
        boolean result = authenticator.validateRequest(invalidToken, activity);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void validateRequest_shouldRejectWrongAudience() {
        // Given
        String token = createJwtToken("wrong-app-id", "https://api.botframework.com", 9999999999L);
        JsonNode activity = objectMapper.createObjectNode();

        // When
        boolean result = authenticator.validateRequest("Bearer " + token, activity);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void validateRequest_shouldRejectExpiredToken() {
        // Given
        String token = createJwtToken("app-123", "https://api.botframework.com", 1000000000L);
        JsonNode activity = objectMapper.createObjectNode();

        // When
        boolean result = authenticator.validateRequest("Bearer " + token, activity);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void validateRequest_shouldRejectInvalidIssuer() {
        // Given
        String token = createJwtToken("app-123", "https://invalid.issuer.com", 9999999999L);
        JsonNode activity = objectMapper.createObjectNode();

        // When
        boolean result = authenticator.validateRequest("Bearer " + token, activity);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void validateRequest_shouldAcceptValidBotFrameworkToken() {
        // Given
        String token = createJwtToken("app-123", "https://api.botframework.com", 9999999999L);
        JsonNode activity = createActivity();

        // When
        boolean result = authenticator.validateRequest("Bearer " + token, activity);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void validateRequest_shouldAcceptValidAzureAdToken() {
        // Given
        String token = createJwtToken("app-123", "https://sts.windows.net/tenant-id/", 9999999999L);
        JsonNode activity = createActivity();

        // When
        boolean result = authenticator.validateRequest("Bearer " + token, activity);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void validateRequest_shouldAcceptLoginMicrosoftOnlineIssuer() {
        // Given
        String token = createJwtToken("app-123", "https://login.microsoftonline.com/tenant-id/v2.0", 9999999999L);
        JsonNode activity = createActivity();

        // When
        boolean result = authenticator.validateRequest("Bearer " + token, activity);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void validateRequest_shouldVerifyTenantIdWhenConfigured() {
        // Given
        lenient().when(config.getTenantId()).thenReturn("tenant-123");
        String token = createJwtToken("app-123", "https://sts.windows.net/tenant-123/", 9999999999L);
        JsonNode activity = createActivity();

        // When
        boolean result = authenticator.validateRequest("Bearer " + token, activity);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void validateRequest_shouldRejectWrongTenantIdWhenConfigured() {
        // Given
        lenient().when(config.getTenantId()).thenReturn("tenant-123");
        String token = createJwtToken("app-123", "https://sts.windows.net/tenant-456/", 9999999999L);
        JsonNode activity = createActivity();

        // When
        boolean result = authenticator.validateRequest("Bearer " + token, activity);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void validateRequest_shouldHandleServiceUrl() {
        // Given
        String token = createJwtToken("app-123", "https://api.botframework.com", 9999999999L);
        ObjectNode activity = createActivity();
        activity.put("serviceUrl", "https://smba.trafficmanager.net/teams/");

        // When
        boolean result = authenticator.validateRequest("Bearer " + token, activity);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void validateRequest_shouldHandleTokenWithoutExpiration() {
        // Given
        String token = createJwtTokenWithoutExp("app-123", "https://api.botframework.com");
        JsonNode activity = createActivity();

        // When
        boolean result = authenticator.validateRequest("Bearer " + token, activity);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void validateRequest_shouldHandleMalformedJwtPayload() {
        // Given
        String token = "header." + Base64.getUrlEncoder().encodeToString("invalid json".getBytes()) + ".signature";
        JsonNode activity = createActivity();

        // When
        boolean result = authenticator.validateRequest("Bearer " + token, activity);

        // Then
        assertThat(result).isFalse();
    }

    // Helper methods

    private String createJwtToken(String audience, String issuer, long expiration) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("aud", audience);
            payload.put("iss", issuer);
            payload.put("exp", expiration);

            String payloadJson = objectMapper.writeValueAsString(payload);
            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            // Create a fake JWT (header.payload.signature)
            String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS256\"}".getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("fake-signature".getBytes(StandardCharsets.UTF_8));

            return header + "." + encodedPayload + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String createJwtTokenWithoutExp(String audience, String issuer) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("aud", audience);
            payload.put("iss", issuer);

            String payloadJson = objectMapper.writeValueAsString(payload);
            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS256\"}".getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("fake-signature".getBytes(StandardCharsets.UTF_8));

            return header + "." + encodedPayload + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ObjectNode createActivity() {
        ObjectNode activity = objectMapper.createObjectNode();
        activity.put("type", "message");
        activity.put("serviceUrl", "https://smba.trafficmanager.net/teams/");
        return activity;
    }
}
