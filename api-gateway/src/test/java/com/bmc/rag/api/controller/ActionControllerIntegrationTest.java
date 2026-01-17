package com.bmc.rag.api.controller;

import com.bmc.rag.agent.confirmation.ConfirmationService;
import com.bmc.rag.agent.confirmation.PendingAction;
import com.bmc.rag.connector.dto.IncidentCreationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the agentic confirmation workflow.
 * Tests the end-to-end flow of staging and confirming actions.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "agentic.enabled=true",
        "spring.datasource.password=ragpassword"
    }
)
@ActiveProfiles("dev")
class ActionControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired(required = false)
    private ConfirmationService confirmationService;

    @Test
    void testAgenticStatusEndpoint() {
        String url = "http://localhost:" + port + "/api/v1/actions/status";

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("enabled"));
    }

    @Test
    void testPendingActionsEndpoint() {
        String url = "http://localhost:" + port + "/api/v1/actions/pending?sessionId=test-session";

        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testRateLimitEndpoint() {
        String url = "http://localhost:" + port + "/api/v1/actions/rate-limit?sessionId=test-session";

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10, ((Number) response.getBody().get("maxPerHour")).intValue());
        assertEquals(10, ((Number) response.getBody().get("remaining")).intValue());
        assertEquals(false, response.getBody().get("isLimited"));
    }

    @Test
    void testConfirmationWorkflow() {
        // Skip if confirmation service not available (agentic disabled)
        if (confirmationService == null) {
            System.out.println("ConfirmationService not available, skipping test");
            return;
        }

        String sessionId = "test-integration-session";
        String userId = "test-user";

        // Stage an incident creation
        IncidentCreationRequest request = IncidentCreationRequest.builder()
            .summary("Test incident from integration test")
            .description("This is a test incident created by the integration test")
            .impact(3)
            .urgency(3)
            .createdBy(userId)
            .sessionId(sessionId)
            .build();

        PendingAction action = confirmationService.stageIncidentCreation(sessionId, userId, request);

        assertNotNull(action);
        assertNotNull(action.getActionId());
        assertEquals(PendingAction.ActionType.INCIDENT_CREATE, action.getActionType());
        assertEquals(PendingAction.ActionStatus.PENDING, action.getStatus());

        System.out.println("Staged action ID: " + action.getActionId());
        System.out.println("Confirmation prompt: " + action.getConfirmationPrompt());

        // Verify pending action is in the confirmation service
        var pendingActions = confirmationService.getPendingActionsForSession(sessionId);
        assertFalse(pendingActions.isEmpty(), "Should have pending actions in session");

        // Verify via API (note: API requires session ownership check)
        String pendingUrl = "http://localhost:" + port + "/api/v1/actions/pending?sessionId=" + sessionId;
        ResponseEntity<List> pendingResponse = restTemplate.getForEntity(pendingUrl, List.class);
        assertEquals(HttpStatus.OK, pendingResponse.getStatusCode());

        // Cancel the action (don't actually create in Remedy during tests)
        var cancelResult = confirmationService.cancel(action.getActionId(), sessionId, userId);
        assertTrue(cancelResult.cancelled());

        System.out.println("Action cancelled: " + cancelResult.message());
    }
}
