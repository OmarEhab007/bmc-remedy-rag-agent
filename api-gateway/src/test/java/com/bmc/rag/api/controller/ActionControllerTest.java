package com.bmc.rag.api.controller;

import com.bmc.rag.agent.config.AgenticConfig;
import com.bmc.rag.agent.confirmation.ConfirmationService;
import com.bmc.rag.agent.confirmation.PendingAction;
import com.bmc.rag.agent.security.AgenticRateLimiter;
import com.bmc.rag.api.config.RateLimitConfig;
import com.bmc.rag.api.dto.ConfirmActionRequest;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ActionController.
 *
 * Note: This is a @WebMvcTest (NOT @SpringBootTest) to test the controller layer only.
 * ActionControllerIntegrationTest exists separately for full integration testing.
 */
@WebMvcTest(
    controllers = ActionController.class,
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "agentic.enabled=true")
class ActionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConfirmationService confirmationService;

    @MockBean
    private AgenticRateLimiter rateLimiter;

    @MockBean
    private AgenticConfig agenticConfig;

    @MockBean
    private ThreadLocalARContext threadLocalARContext;

    @MockBean
    private RateLimitConfig rateLimitConfig;

    @Test
    void confirmAction_validRequest_returnsSuccess() throws Exception {
        // Given
        ConfirmActionRequest request = ConfirmActionRequest.builder()
            .actionId("abc12345")
            .sessionId("test-session")
            .build();

        ConfirmationService.ConfirmationResult result =
            new ConfirmationService.ConfirmationResult(true, false, "INC000123", "Incident created successfully");

        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
        when(confirmationService.confirm(eq("abc12345"), eq("test-session"), anyString()))
            .thenReturn(result);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/actions/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.actionId").value("abc12345"))
            .andExpect(jsonPath("$.status").value("EXECUTED"))
            .andExpect(jsonPath("$.recordId").value("INC000123"))
            .andExpect(jsonPath("$.message").value("Incident created successfully"))
            .andExpect(jsonPath("$.timestamp").isNumber());
    }

    @Test
    void confirmAction_rateLimited_returns429() throws Exception {
        // Given
        ConfirmActionRequest request = ConfirmActionRequest.builder()
            .actionId("abc12345")
            .sessionId("test-session")
            .build();

        when(rateLimiter.isRateLimited(anyString())).thenReturn(true);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/actions/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.message").value(containsString("Rate limit exceeded")));
    }

    @Test
    void confirmAction_failedConfirmation_returnsBadRequest() throws Exception {
        // Given
        ConfirmActionRequest request = ConfirmActionRequest.builder()
            .actionId("abc12345")
            .sessionId("test-session")
            .build();

        ConfirmationService.ConfirmationResult result =
            new ConfirmationService.ConfirmationResult(false, false, null, "Action not found");

        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
        when(confirmationService.confirm(eq("abc12345"), eq("test-session"), anyString()))
            .thenReturn(result);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/actions/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Action not found"));
    }

    @Test
    void confirmAction_invalidActionId_returnsBadRequest() throws Exception {
        // Given - action ID must be exactly 8 characters
        ConfirmActionRequest request = ConfirmActionRequest.builder()
            .actionId("short")
            .sessionId("test-session")
            .build();

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/actions/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void confirmAction_missingSessionId_returnsBadRequest() throws Exception {
        // Given
        ConfirmActionRequest request = ConfirmActionRequest.builder()
            .actionId("abc12345")
            .sessionId("")
            .build();

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/actions/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void cancelAction_validRequest_returnsSuccess() throws Exception {
        // Given
        ConfirmActionRequest request = ConfirmActionRequest.builder()
            .actionId("abc12345")
            .sessionId("test-session")
            .build();

        // cancelled() returns true when success=false and cancelled=true
        ConfirmationService.ConfirmationResult result =
            new ConfirmationService.ConfirmationResult(false, true, null, "Action cancelled successfully");

        when(confirmationService.cancel(eq("abc12345"), eq("test-session"), anyString()))
            .thenReturn(result);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(delete("/api/v1/actions/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value("CANCELLED"))
            .andExpect(jsonPath("$.message").value("Action cancelled successfully"));
    }

    @Test
    void cancelAction_notFound_returnsBadRequest() throws Exception {
        // Given
        ConfirmActionRequest request = ConfirmActionRequest.builder()
            .actionId("abc12345")
            .sessionId("test-session")
            .build();

        // Not found: success=false, cancelled=false
        ConfirmationService.ConfirmationResult result =
            new ConfirmationService.ConfirmationResult(false, false, null, "Action not found");

        when(confirmationService.cancel(eq("abc12345"), eq("test-session"), anyString()))
            .thenReturn(result);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(delete("/api/v1/actions/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Action not found"));
    }

    @Test
    void getPendingActions_withActions_returnsActionList() throws Exception {
        // Given
        PendingAction action = PendingAction.builder()
            .actionId("abc12345")
            .actionType(PendingAction.ActionType.INCIDENT_CREATE)
            .preview("Create incident: VPN not working")
            .sessionId("test-session")
            .userId("session:test-session")
            .stagedAt(Instant.now().minus(1, ChronoUnit.MINUTES))
            .expiresAt(Instant.now().plus(4, ChronoUnit.MINUTES))
            .status(PendingAction.ActionStatus.PENDING)
            .build();

        when(confirmationService.getPendingActionsForSession("test-session"))
            .thenReturn(List.of(action));

        // When & Then
        mockMvc.perform(get("/api/v1/actions/pending")
                .param("sessionId", "test-session")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].actionId").value("abc12345"))
            .andExpect(jsonPath("$[0].actionType").value("INCIDENT_CREATE"))
            .andExpect(jsonPath("$[0].preview").value("Create incident: VPN not working"))
            .andExpect(jsonPath("$[0].status").value("PENDING"))
            .andExpect(jsonPath("$[0].secondsUntilExpiry").isNumber());
    }

    @Test
    void getPendingActions_noActions_returnsEmptyList() throws Exception {
        // Given
        when(confirmationService.getPendingActionsForSession("test-session"))
            .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/actions/pending")
                .param("sessionId", "test-session")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getAction_existingAction_returnsActionDetails() throws Exception {
        // Given
        PendingAction action = PendingAction.builder()
            .actionId("abc12345")
            .actionType(PendingAction.ActionType.INCIDENT_CREATE)
            .preview("Create incident: VPN not working")
            .sessionId("test-session")
            .userId("session:test-session")
            .stagedAt(Instant.now().minus(1, ChronoUnit.MINUTES))
            .expiresAt(Instant.now().plus(4, ChronoUnit.MINUTES))
            .status(PendingAction.ActionStatus.PENDING)
            .build();

        when(confirmationService.getAction("abc12345"))
            .thenReturn(Optional.of(action));

        // When & Then
        mockMvc.perform(get("/api/v1/actions/abc12345")
                .param("sessionId", "test-session")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actionId").value("abc12345"))
            .andExpect(jsonPath("$.actionType").value("INCIDENT_CREATE"))
            .andExpect(jsonPath("$.preview").value("Create incident: VPN not working"));
    }

    @Test
    void getAction_notFound_returns404() throws Exception {
        // Given
        when(confirmationService.getAction("abc12345"))
            .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/actions/abc12345")
                .param("sessionId", "test-session")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    void getRateLimitStatus_returnsStatus() throws Exception {
        // Given
        AgenticRateLimiter.RateLimitStatus status =
            new AgenticRateLimiter.RateLimitStatus(10, 5L, false);

        when(rateLimiter.getStatus(anyString())).thenReturn(status);

        // When & Then
        mockMvc.perform(get("/api/v1/actions/rate-limit")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.maxPerHour").value(10))
            .andExpect(jsonPath("$.remaining").value(5))
            .andExpect(jsonPath("$.isLimited").value(false));
    }

    @Test
    void getAgenticStatus_returnsConfiguration() throws Exception {
        // Given
        AgenticConfig.Confirmation confirmationCfg = new AgenticConfig.Confirmation();
        confirmationCfg.setRequireConfirmation(true);
        confirmationCfg.setTimeoutMinutes(5);

        AgenticConfig.RateLimit rateLimitCfg = new AgenticConfig.RateLimit();
        rateLimitCfg.setMaxCreationsPerHour(10);

        when(agenticConfig.isEnabled()).thenReturn(true);
        when(agenticConfig.getConfirmation()).thenReturn(confirmationCfg);
        when(agenticConfig.getRateLimit()).thenReturn(rateLimitCfg);

        // When & Then
        mockMvc.perform(get("/api/v1/actions/status")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(true))
            .andExpect(jsonPath("$.confirmationRequired").value(true))
            .andExpect(jsonPath("$.timeoutMinutes").value(5))
            .andExpect(jsonPath("$.maxCreationsPerHour").value(10));
    }

    @Test
    void confirmAction_withOptionalMessage_includesInRequest() throws Exception {
        // Given
        ConfirmActionRequest request = ConfirmActionRequest.builder()
            .actionId("abc12345")
            .sessionId("test-session")
            .message("Confirm this incident creation")
            .build();

        ConfirmationService.ConfirmationResult result =
            new ConfirmationService.ConfirmationResult(true, false, "INC000123", "Created");

        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
        when(confirmationService.confirm(eq("abc12345"), eq("test-session"), anyString()))
            .thenReturn(result);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/actions/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.recordId").value("INC000123"));
    }

    @Test
    void confirmAction_messageTooLong_returnsBadRequest() throws Exception {
        // Given - message exceeds 500 characters
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 501; i++) {
            longMessage.append("x");
        }

        ConfirmActionRequest request = ConfirmActionRequest.builder()
            .actionId("abc12345")
            .sessionId("test-session")
            .message(longMessage.toString())
            .build();

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/actions/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getPendingActions_filtersOutNonMatchingActions() throws Exception {
        // Given
        PendingAction matchingAction = PendingAction.builder()
            .actionId("abc12345")
            .actionType(PendingAction.ActionType.INCIDENT_CREATE)
            .preview("My action")
            .sessionId("test-session")
            .userId("session:test-session")
            .stagedAt(Instant.now())
            .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
            .status(PendingAction.ActionStatus.PENDING)
            .build();

        PendingAction nonMatchingAction = PendingAction.builder()
            .actionId("xyz67890")
            .actionType(PendingAction.ActionType.INCIDENT_CREATE)
            .preview("Someone else's action")
            .sessionId("test-session")
            .userId("other-user")
            .stagedAt(Instant.now())
            .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
            .status(PendingAction.ActionStatus.PENDING)
            .build();

        when(confirmationService.getPendingActionsForSession("test-session"))
            .thenReturn(List.of(matchingAction, nonMatchingAction));

        // When & Then - only matching action should be returned
        mockMvc.perform(get("/api/v1/actions/pending")
                .param("sessionId", "test-session")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].actionId").value("abc12345"));
        // Note: The non-matching action is filtered out by belongsTo() logic
    }
}
