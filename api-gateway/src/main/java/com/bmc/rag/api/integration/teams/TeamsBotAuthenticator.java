package com.bmc.rag.api.integration.teams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Authenticator for Microsoft Teams Bot Framework requests.
 * Validates JWT tokens from Azure Bot Service.
 *
 * Note: In production, this should use a proper JWT library with JWKS validation.
 * For on-premise deployments, you may need to configure trusted issuers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "teams.bot", name = "enabled", havingValue = "true")
public class TeamsBotAuthenticator {

    private static final String BOT_FRAMEWORK_ISSUER = "https://api.botframework.com";
    private static final String AZURE_AD_ISSUER_PREFIX = "https://sts.windows.net/";

    private final TeamsBotConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Validate an incoming request from Teams/Bot Framework.
     *
     * @param authHeader Authorization header value
     * @param activity The activity payload
     * @return true if valid, false otherwise
     */
    public boolean validateRequest(String authHeader, JsonNode activity) {
        // In development/testing, allow requests without auth when disabled
        if (!config.isEnabled()) {
            log.debug("Teams bot authentication disabled, allowing request");
            return true;
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header");
            return false;
        }

        String token = authHeader.substring(7);

        try {
            // Decode and validate the JWT payload (basic validation)
            JsonNode payload = decodeJwtPayload(token);

            if (payload == null) {
                log.warn("Failed to decode JWT payload");
                return false;
            }

            // Verify the audience matches our app ID
            String audience = payload.path("aud").asText();
            if (!config.getAppId().equals(audience)) {
                log.warn("Token audience mismatch. Expected: {}, Got: {}", config.getAppId(), audience);
                return false;
            }

            // Verify the issuer
            String issuer = payload.path("iss").asText();
            if (!isValidIssuer(issuer)) {
                log.warn("Invalid token issuer: {}", issuer);
                return false;
            }

            // Verify token is not expired
            long exp = payload.path("exp").asLong(0);
            long now = System.currentTimeMillis() / 1000;
            if (exp > 0 && exp < now) {
                log.warn("Token has expired");
                return false;
            }

            // Verify service URL if present
            String serviceUrl = activity.path("serviceUrl").asText();
            if (serviceUrl != null && !serviceUrl.isEmpty()) {
                log.debug("Request from service URL: {}", serviceUrl);
            }

            log.debug("Token validated successfully for audience: {}", audience);
            return true;

        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Decode the JWT payload without signature verification.
     * Note: In production, use proper JWKS-based signature verification.
     */
    private JsonNode decodeJwtPayload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.warn("Invalid JWT format - expected 3 parts, got {}", parts.length);
                return null;
            }

            // Decode the payload (middle part)
            String payloadBase64 = parts[1];
            // Handle URL-safe Base64
            byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadBase64);
            String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);

            return objectMapper.readTree(payloadJson);
        } catch (Exception e) {
            log.error("Failed to decode JWT: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if the issuer is valid for Bot Framework tokens.
     */
    private boolean isValidIssuer(String issuer) {
        if (issuer == null || issuer.isEmpty()) {
            return false;
        }

        // Accept Bot Framework issuer
        if (BOT_FRAMEWORK_ISSUER.equals(issuer)) {
            return true;
        }

        // Accept Azure AD issuers
        if (issuer.startsWith(AZURE_AD_ISSUER_PREFIX)) {
            // If tenant ID is configured, verify it matches
            if (config.getTenantId() != null && !config.getTenantId().isEmpty()) {
                String expectedIssuer = AZURE_AD_ISSUER_PREFIX + config.getTenantId() + "/";
                return expectedIssuer.equals(issuer);
            }
            return true; // Accept any Azure AD tenant if not configured
        }

        // Also accept login.microsoftonline.com issuers
        if (issuer.startsWith("https://login.microsoftonline.com/")) {
            return true;
        }

        return false;
    }
}
