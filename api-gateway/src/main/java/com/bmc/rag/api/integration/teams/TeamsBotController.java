package com.bmc.rag.api.integration.teams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Microsoft Teams Bot Framework webhook endpoint.
 * Receives activities from Teams and processes them through the bot handler.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "teams.bot", name = "enabled", havingValue = "true")
public class TeamsBotController {

    private final TeamsBotHandler botHandler;
    private final TeamsBotAuthenticator authenticator;
    private final ObjectMapper objectMapper;

    /**
     * Main webhook endpoint for Teams Bot Framework.
     * Receives activities from Azure Bot Service.
     *
     * @param authHeader Authorization header with Bearer token
     * @param activity The activity payload from Teams
     * @return Response to send back to Teams
     */
    @PostMapping("/messages")
    public ResponseEntity<JsonNode> receiveActivity(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody JsonNode activity) {

        log.debug("Received Teams activity: {}", activity.path("type").asText());

        // Validate the incoming request
        if (!authenticator.validateRequest(authHeader, activity)) {
            log.warn("Unauthorized Teams request rejected");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // Process the activity
            JsonNode response = botHandler.processMessage(activity);

            if (response != null) {
                return ResponseEntity.ok(response);
            } else {
                // No response needed (e.g., for some event types)
                return ResponseEntity.ok().build();
            }

        } catch (Exception e) {
            log.error("Error processing Teams activity: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint for Teams bot.
     *
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Teams bot is healthy");
    }
}
