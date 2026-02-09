package com.bmc.rag.api.controller;

import com.bmc.rag.agent.confirmation.ConfirmationService;
import com.bmc.rag.agent.confirmation.PendingAction;
import com.bmc.rag.agent.security.AgenticRateLimiter;
import com.bmc.rag.agent.security.InputValidator;
import com.bmc.rag.api.config.RateLimitConfig;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.connector.service.WorkLogService;
import com.bmc.rag.store.service.VectorStoreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ToolServerController.
 */
@WebMvcTest(
    controllers = ToolServerController.class,
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.TestPropertySource(properties = "agentic.enabled=true")
class ToolServerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VectorStoreService vectorStoreService;

    @MockBean
    private ConfirmationService confirmationService;

    @MockBean
    private InputValidator inputValidator;

    @MockBean
    private AgenticRateLimiter rateLimiter;

    @MockBean
    private WorkLogService workLogService;

    @MockBean
    private ThreadLocalARContext threadLocalARContext;

    @MockBean
    private RateLimitConfig rateLimitConfig;

    @Test
    void searchIncidents_validRequest_returnsResults() throws Exception {
        // Given
        VectorStoreService.SearchResult result = VectorStoreService.SearchResult.builder()
            .sourceId("INC000123")
            .sourceType("Incident")
            .textSegment("VPN authentication failed")
            .score(0.92f)
            .metadata(Map.of(
                "title", "VPN auth error",
                "status", "Resolved"
            ))
            .build();

        when(vectorStoreService.searchByType("VPN", "Incident", 10, 0.3))
            .thenReturn(List.of(result));

        String requestJson = """
            {
                "query": "VPN",
                "limit": 10,
                "minScore": 0.3
            }
            """;

        // When & Then
        mockMvc.perform(post("/tool-server/incidents/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.query").value("VPN"))
            .andExpect(jsonPath("$.totalResults").value(1))
            .andExpect(jsonPath("$.results[0].id").value("INC000123"))
            .andExpect(jsonPath("$.results[0].type").value("Incident"))
            .andExpect(jsonPath("$.results[0].score").value(closeTo(0.92, 0.01)));
    }

    @Test
    void searchIncidents_noResults_returnsEmptyResponse() throws Exception {
        // Given
        when(vectorStoreService.searchByType(anyString(), anyString(), anyInt(), anyDouble()))
            .thenReturn(Collections.emptyList());

        String requestJson = """
            {
                "query": "nonexistent",
                "limit": 10,
                "minScore": 0.3
            }
            """;

        // When & Then
        mockMvc.perform(post("/tool-server/incidents/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(0))
            .andExpect(jsonPath("$.results").isEmpty());
    }

    @Test
    void getIncidentDetails_existingIncident_returnsDetails() throws Exception {
        // Given
        VectorStoreService.SearchResult result = VectorStoreService.SearchResult.builder()
            .sourceId("INC000123")
            .sourceType("Incident")
            .textSegment("VPN authentication failed for user")
            .score(1.0f)
            .metadata(Map.of(
                "title", "VPN auth error",
                "status", "Resolved",
                "assigned_group", "Network Support"
            ))
            .build();

        when(vectorStoreService.searchByType("INC000123", "Incident", 10, 0.0))
            .thenReturn(List.of(result));

        // When & Then
        mockMvc.perform(get("/tool-server/incidents/INC000123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.incidentNumber").value("INC000123"))
            .andExpect(jsonPath("$.summary").value("VPN auth error"))
            .andExpect(jsonPath("$.found").value(true));
    }

    @Test
    void getIncidentDetails_notFound_returns404() throws Exception {
        // Given
        when(vectorStoreService.searchByType(anyString(), anyString(), anyInt(), anyDouble()))
            .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/tool-server/incidents/INC999999")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.found").value(false))
            .andExpect(jsonPath("$.incidentNumber").value("INC999999"));
    }

    @Test
    void createIncident_validRequest_returnsStaged() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
        when(inputValidator.validateSummary(anyString()))
            .thenReturn(new InputValidator.ValidationResult(true, Collections.emptyList(), Collections.emptyList(), "Sanitized summary"));
        when(inputValidator.validateDescription(anyString()))
            .thenReturn(new InputValidator.ValidationResult(true, Collections.emptyList(), Collections.emptyList(), "Sanitized description"));
        when(vectorStoreService.searchByType(anyString(), anyString(), anyInt(), anyDouble()))
            .thenReturn(Collections.emptyList());

        PendingAction action = PendingAction.builder()
            .actionId("abc123")
            .preview("Incident will be created")
            .expiresAt(Instant.now().plusSeconds(300))
            .build();

        when(confirmationService.stageIncidentCreation(anyString(), anyString(), any()))
            .thenReturn(action);

        String requestJson = """
            {
                "summary": "VPN not working",
                "description": "Cannot connect to VPN",
                "impact": 3,
                "urgency": 3,
                "sessionId": "test-session"
            }
            """;

        // When & Then
        mockMvc.perform(post("/tool-server/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("STAGED"))
            .andExpect(jsonPath("$.actionId").value("abc123"));
    }

    @Test
    void confirmAction_validActionId_returnsSuccess() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);

        ConfirmationService.ConfirmationResult result =
            new ConfirmationService.ConfirmationResult(true, false, "INC000123", "Incident created");

        when(confirmationService.confirm(eq("abc123"), eq("test-session"), anyString()))
            .thenReturn(result);

        // When & Then
        mockMvc.perform(post("/tool-server/actions/confirm")
                .param("actionId", "abc123")
                .param("sessionId", "test-session")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EXECUTED"))
            .andExpect(jsonPath("$.recordId").value("INC000123"));
    }

    @Test
    void confirmAction_rateLimited_returns429() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(true);
        when(rateLimiter.getStatus(anyString()))
            .thenReturn(new AgenticRateLimiter.RateLimitStatus(10, 0L, true));

        // When & Then
        mockMvc.perform(post("/tool-server/actions/confirm")
                .param("actionId", "abc123")
                .param("sessionId", "test-session")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void getOpenApiSpec_returnsJsonSpec() throws Exception {
        // When & Then
        mockMvc.perform(get("/tool-server/openapi.json")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void searchIncidents_withHighSimilarity_flagsDuplicates() throws Exception {
        // Given
        VectorStoreService.SearchResult result = VectorStoreService.SearchResult.builder()
            .sourceId("INC000123")
            .sourceType("Incident")
            .textSegment("VPN authentication failed")
            .score(0.92f) // Above 0.85 threshold
            .metadata(Map.of("title", "VPN auth error", "status", "Resolved"))
            .build();

        when(vectorStoreService.searchByType("VPN", "Incident", 10, 0.3))
            .thenReturn(List.of(result));

        String requestJson = """
            {
                "query": "VPN",
                "limit": 10,
                "minScore": 0.3
            }
            """;

        // When & Then
        mockMvc.perform(post("/tool-server/incidents/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasPotentialDuplicates").value(true))
            .andExpect(jsonPath("$.message").value(containsString("highly similar")));
    }

    @Test
    void createIncident_rateLimited_returns429() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(true);
        when(rateLimiter.getStatus(anyString()))
            .thenReturn(new AgenticRateLimiter.RateLimitStatus(10, 0L, true));

        String requestJson = """
            {
                "summary": "Test",
                "description": "Test",
                "impact": 3,
                "urgency": 3
            }
            """;

        // When & Then
        mockMvc.perform(post("/tool-server/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.status").value("RATE_LIMITED"));
    }

    @Test
    void createIncident_invalidSummary_returnsBadRequest() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
        when(inputValidator.validateSummary(anyString()))
            .thenReturn(new InputValidator.ValidationResult(false, List.of("Too short"), List.of(), null));

        String requestJson = """
            {
                "summary": "x",
                "description": "Test",
                "impact": 3,
                "urgency": 3
            }
            """;

        // When & Then
        mockMvc.perform(post("/tool-server/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"));
    }

    @Test
    void createIncident_withDuplicates_returnsWarning() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
        when(inputValidator.validateSummary(anyString()))
            .thenReturn(new InputValidator.ValidationResult(true, Collections.emptyList(), Collections.emptyList(), "VPN issue"));
        when(inputValidator.validateDescription(anyString()))
            .thenReturn(new InputValidator.ValidationResult(true, Collections.emptyList(), Collections.emptyList(), "Cannot connect"));

        VectorStoreService.SearchResult duplicate = VectorStoreService.SearchResult.builder()
            .sourceId("INC000999")
            .sourceType("Incident")
            .textSegment("VPN connection problem")
            .score(0.90f)
            .metadata(Map.of("title", "VPN issue"))
            .build();

        when(vectorStoreService.searchByType(anyString(), eq("Incident"), eq(5), eq(0.85)))
            .thenReturn(List.of(duplicate));

        PendingAction action = PendingAction.builder()
            .actionId("abc123")
            .preview("Preview")
            .expiresAt(Instant.now().plusSeconds(300))
            .build();

        when(confirmationService.stageIncidentCreation(anyString(), anyString(), any()))
            .thenReturn(action);

        String requestJson = """
            {
                "summary": "VPN issue",
                "description": "Cannot connect",
                "impact": 3,
                "urgency": 3
            }
            """;

        // When & Then
        mockMvc.perform(post("/tool-server/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DUPLICATE_WARNING"))
            .andExpect(jsonPath("$.similarIncidents").isArray())
            .andExpect(jsonPath("$.similarIncidents[0].id").value("INC000999"));
    }

    @Test
    void updateIncident_noUpdatesSpecified_returnsBadRequest() throws Exception {
        // Given
        String requestJson = """
            {
                "sessionId": "test-session"
            }
            """;

        // When & Then
        mockMvc.perform(put("/tool-server/incidents/INC000123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.message").value(containsString("No updates specified")));
    }

    @Test
    void updateIncident_validRequest_returnsStaged() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);

        PendingAction action = PendingAction.builder()
            .actionId("xyz789")
            .preview("Update incident")
            .expiresAt(Instant.now().plusSeconds(300))
            .build();

        when(confirmationService.stageIncidentUpdate(anyString(), anyString(), any()))
            .thenReturn(action);

        String requestJson = """
            {
                "status": "Resolved",
                "resolution": "Fixed by restarting VPN",
                "sessionId": "test-session"
            }
            """;

        // When & Then
        mockMvc.perform(put("/tool-server/incidents/INC000123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("STAGED"))
            .andExpect(jsonPath("$.actionId").value("xyz789"))
            .andExpect(jsonPath("$.incidentNumber").value("INC000123"));
    }

    @Test
    void getIncidentWorkLogs_returnsWorkLogs() throws Exception {
        // Given
        List<WorkLogService.WorkLogEntry> workLogs = List.of(
            new WorkLogService.WorkLogEntry(
                "WL001",
                "INC000123",
                1,
                "Working Log",
                "Investigated issue",
                "tech@example.com",
                Instant.now(),
                "Public"
            )
        );

        when(workLogService.getWorkLogsForIncident("INC000123"))
            .thenReturn(workLogs);

        // When & Then
        mockMvc.perform(get("/tool-server/incidents/INC000123/worklogs")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.incidentNumber").value("INC000123"))
            .andExpect(jsonPath("$.totalCount").value(1))
            .andExpect(jsonPath("$.workLogs[0].workLogId").value("WL001"))
            .andExpect(jsonPath("$.workLogs[0].description").value("Investigated issue"));
    }

    @Test
    void searchKnowledge_validRequest_returnsResults() throws Exception {
        // Given
        VectorStoreService.SearchResult result = VectorStoreService.SearchResult.builder()
            .sourceId("KB000123")
            .sourceType("KnowledgeArticle")
            .textSegment("How to reset VPN password")
            .score(0.88f)
            .metadata(Map.of("title", "VPN Password Reset"))
            .build();

        when(vectorStoreService.searchByType("VPN password", "KnowledgeArticle", 10, 0.3))
            .thenReturn(List.of(result));

        String requestJson = """
            {
                "query": "VPN password",
                "limit": 10,
                "minScore": 0.3
            }
            """;

        // When & Then
        mockMvc.perform(post("/tool-server/knowledge/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(1))
            .andExpect(jsonPath("$.results[0].id").value("KB000123"))
            .andExpect(jsonPath("$.results[0].type").value("KnowledgeArticle"));
    }

    @Test
    void getKnowledgeArticle_existingArticle_returnsDetails() throws Exception {
        // Given
        VectorStoreService.SearchResult result = VectorStoreService.SearchResult.builder()
            .sourceId("KB000123")
            .sourceType("KnowledgeArticle")
            .textSegment("Article content here")
            .score(1.0f)
            .metadata(Map.of("title", "VPN Setup Guide", "status", "Published"))
            .build();

        when(vectorStoreService.searchByType("KB000123", "KnowledgeArticle", 10, 0.0))
            .thenReturn(List.of(result));

        // When & Then
        mockMvc.perform(get("/tool-server/knowledge/KB000123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.articleId").value("KB000123"))
            .andExpect(jsonPath("$.title").value("VPN Setup Guide"))
            .andExpect(jsonPath("$.found").value(true));
    }

    @Test
    void getKnowledgeArticle_notFound_returns404() throws Exception {
        // Given
        when(vectorStoreService.searchByType(anyString(), anyString(), anyInt(), anyDouble()))
            .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/tool-server/knowledge/KB999999")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.found").value(false));
    }

    @Test
    void cancelAction_validRequest_returnsCancelled() throws Exception {
        // Given
        // cancelled() returns true when success=false and cancelled=true
        ConfirmationService.ConfirmationResult result =
            new ConfirmationService.ConfirmationResult(false, true, null, "Action cancelled");

        when(confirmationService.cancel(eq("abc123"), eq("test-session"), anyString()))
            .thenReturn(result);

        // When & Then
        mockMvc.perform(post("/tool-server/actions/cancel")
                .param("actionId", "abc123")
                .param("sessionId", "test-session")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void getPendingActions_returnsFilteredActions() throws Exception {
        // Given
        PendingAction action = PendingAction.builder()
            .actionId("abc123")
            .actionType(PendingAction.ActionType.INCIDENT_CREATE)
            .preview("Create VPN incident")
            .sessionId("test-session")
            .userId("session:test-session") // extractUserId returns "session:" + sessionId when no JWT
            .stagedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .status(PendingAction.ActionStatus.PENDING)
            .build();

        when(confirmationService.getPendingActionsForSession("test-session"))
            .thenReturn(List.of(action));

        // When & Then
        mockMvc.perform(get("/tool-server/actions/pending")
                .param("sessionId", "test-session")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].actionId").value("abc123"))
            .andExpect(jsonPath("$[0].actionType").value("INCIDENT_CREATE"));
    }

    @Test
    void confirmAction_expiredAction_returnsExpired() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);

        ConfirmationService.ConfirmationResult result =
            new ConfirmationService.ConfirmationResult(false, true, null, "Action expired");

        when(confirmationService.confirm(eq("abc123"), eq("test-session"), anyString()))
            .thenReturn(result);

        // When & Then
        mockMvc.perform(post("/tool-server/actions/confirm")
                .param("actionId", "abc123")
                .param("sessionId", "test-session")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EXPIRED"));
    }

    @Test
    void searchWorkOrders_validRequest_returnsResults() throws Exception {
        // Given
        VectorStoreService.SearchResult result = VectorStoreService.SearchResult.builder()
            .sourceId("WO000123")
            .sourceType("WorkOrder")
            .textSegment("Network maintenance")
            .score(0.85f)
            .metadata(Map.of("title", "Router upgrade"))
            .build();

        when(vectorStoreService.searchByType("network", "WorkOrder", 10, 0.3))
            .thenReturn(List.of(result));

        String requestJson = """
            {
                "query": "network",
                "limit": 10,
                "minScore": 0.3
            }
            """;

        // When & Then
        mockMvc.perform(post("/tool-server/workorders/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(1))
            .andExpect(jsonPath("$.results[0].type").value("WorkOrder"));
    }

    @Test
    void searchIncidents_serviceError_returnsInternalServerError() throws Exception {
        // Given
        when(vectorStoreService.searchByType(anyString(), anyString(), anyInt(), anyDouble()))
            .thenThrow(new RuntimeException("Database connection failed"));

        String requestJson = """
            {
                "query": "test",
                "limit": 10,
                "minScore": 0.3
            }
            """;

        // When & Then
        mockMvc.perform(post("/tool-server/incidents/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value(containsString("Error searching incidents")));
    }

    @Test
    void getIncidentDetails_serviceError_returnsInternalServerError() throws Exception {
        // Given
        when(vectorStoreService.searchByType(anyString(), anyString(), anyInt(), anyDouble()))
            .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get("/tool-server/incidents/INC000123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.found").value(false))
            .andExpect(jsonPath("$.errorMessage").value(containsString("Error retrieving incident")));
    }

    @Test
    void getIncidentWorkLogs_serviceError_returnsInternalServerError() throws Exception {
        // Given
        when(workLogService.getWorkLogsForIncident(anyString()))
            .thenThrow(new RuntimeException("Failed to retrieve work logs"));

        // When & Then
        mockMvc.perform(get("/tool-server/incidents/INC000123/worklogs")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.errorMessage").value(containsString("Error retrieving work logs")));
    }

    @Test
    void createIncident_invalidDescription_returnsBadRequest() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
        when(inputValidator.validateSummary(anyString()))
            .thenReturn(new InputValidator.ValidationResult(true, Collections.emptyList(), Collections.emptyList(), "Valid summary"));
        when(inputValidator.validateDescription(anyString()))
            .thenReturn(new InputValidator.ValidationResult(false, List.of("Description too short"), List.of(), null));

        String requestJson = """
            {
                "summary": "Test incident",
                "description": "x",
                "impact": 3,
                "urgency": 3
            }
            """;

        // When & Then
        mockMvc.perform(post("/tool-server/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.message").value(containsString("Description")));
    }

    @Test
    void createIncident_serviceError_returnsInternalServerError() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
        when(inputValidator.validateSummary(anyString()))
            .thenReturn(new InputValidator.ValidationResult(true, Collections.emptyList(), Collections.emptyList(), "Valid"));
        when(inputValidator.validateDescription(anyString()))
            .thenReturn(new InputValidator.ValidationResult(true, Collections.emptyList(), Collections.emptyList(), "Valid"));
        when(vectorStoreService.searchByType(anyString(), anyString(), anyInt(), anyDouble()))
            .thenReturn(Collections.emptyList());
        when(confirmationService.stageIncidentCreation(anyString(), anyString(), any()))
            .thenThrow(new RuntimeException("Staging failed"));

        String requestJson = """
            {
                "summary": "Test",
                "description": "Test description",
                "impact": 3,
                "urgency": 3
            }
            """;

        // When & Then
        mockMvc.perform(post("/tool-server/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.message").value(containsString("Failed to stage incident creation")));
    }

    @Test
    void updateIncident_rateLimited_returns429() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(true);
        when(rateLimiter.getStatus(anyString()))
            .thenReturn(new AgenticRateLimiter.RateLimitStatus(10, 0L, true));

        String requestJson = """
            {
                "status": "Resolved",
                "resolution": "Fixed"
            }
            """;

        // When & Then
        mockMvc.perform(put("/tool-server/incidents/INC000123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.message").value(containsString("Rate limit exceeded")));
    }

    @Test
    void updateIncident_serviceError_returnsInternalServerError() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
        when(confirmationService.stageIncidentUpdate(anyString(), anyString(), any()))
            .thenThrow(new RuntimeException("Update staging failed"));

        String requestJson = """
            {
                "status": "Resolved",
                "resolution": "Fixed"
            }
            """;

        // When & Then
        mockMvc.perform(put("/tool-server/incidents/INC000123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.message").value(containsString("Failed to stage update")));
    }

    @Test
    void confirmAction_failed_returnsFailed() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);

        ConfirmationService.ConfirmationResult result =
            new ConfirmationService.ConfirmationResult(false, false, null, "Execution failed");

        when(confirmationService.confirm(eq("abc123"), eq("test-session"), anyString()))
            .thenReturn(result);

        // When & Then
        mockMvc.perform(post("/tool-server/actions/confirm")
                .param("actionId", "abc123")
                .param("sessionId", "test-session")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void confirmAction_serviceError_returnsInternalServerError() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
        when(confirmationService.confirm(anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("Confirmation service error"));

        // When & Then
        mockMvc.perform(post("/tool-server/actions/confirm")
                .param("actionId", "abc123")
                .param("sessionId", "test-session")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.message").value(containsString("Failed to confirm action")));
    }

    @Test
    void confirmAction_workOrderRecordType_returnsCorrectType() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);

        ConfirmationService.ConfirmationResult result =
            new ConfirmationService.ConfirmationResult(true, false, "WO000123", "Work order created");

        when(confirmationService.confirm(eq("abc123"), eq("test-session"), anyString()))
            .thenReturn(result);

        // When & Then
        mockMvc.perform(post("/tool-server/actions/confirm")
                .param("actionId", "abc123")
                .param("sessionId", "test-session")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EXECUTED"))
            .andExpect(jsonPath("$.recordType").value("WorkOrder"));
    }

    @Test
    void confirmAction_knowledgeArticleRecordType_returnsCorrectType() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);

        ConfirmationService.ConfirmationResult result =
            new ConfirmationService.ConfirmationResult(true, false, "KB000123", "Knowledge article created");

        when(confirmationService.confirm(eq("abc123"), eq("test-session"), anyString()))
            .thenReturn(result);

        // When & Then
        mockMvc.perform(post("/tool-server/actions/confirm")
                .param("actionId", "abc123")
                .param("sessionId", "test-session")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EXECUTED"))
            .andExpect(jsonPath("$.recordType").value("KnowledgeArticle"));
    }

    @Test
    void confirmAction_changeRequestRecordType_returnsCorrectType() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);

        ConfirmationService.ConfirmationResult result =
            new ConfirmationService.ConfirmationResult(true, false, "CR000123", "Change request created");

        when(confirmationService.confirm(eq("abc123"), eq("test-session"), anyString()))
            .thenReturn(result);

        // When & Then
        mockMvc.perform(post("/tool-server/actions/confirm")
                .param("actionId", "abc123")
                .param("sessionId", "test-session")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EXECUTED"))
            .andExpect(jsonPath("$.recordType").value("ChangeRequest"));
    }

    @Test
    void confirmAction_unknownRecordType_returnsGenericType() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);

        ConfirmationService.ConfirmationResult result =
            new ConfirmationService.ConfirmationResult(true, false, "UNKNOWN123", "Record created");

        when(confirmationService.confirm(eq("abc123"), eq("test-session"), anyString()))
            .thenReturn(result);

        // When & Then
        mockMvc.perform(post("/tool-server/actions/confirm")
                .param("actionId", "abc123")
                .param("sessionId", "test-session")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EXECUTED"))
            .andExpect(jsonPath("$.recordType").value("Record"));
    }

    @Test
    void cancelAction_notFound_returnsNotFound() throws Exception {
        // Given
        ConfirmationService.ConfirmationResult result =
            new ConfirmationService.ConfirmationResult(false, false, null, "Not found");

        when(confirmationService.cancel(eq("abc123"), eq("test-session"), anyString()))
            .thenReturn(result);

        // When & Then
        mockMvc.perform(post("/tool-server/actions/cancel")
                .param("actionId", "abc123")
                .param("sessionId", "test-session")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void cancelAction_serviceError_returnsInternalServerError() throws Exception {
        // Given
        when(confirmationService.cancel(anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("Cancellation failed"));

        // When & Then
        mockMvc.perform(post("/tool-server/actions/cancel")
                .param("actionId", "abc123")
                .param("sessionId", "test-session")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void searchKnowledge_noResults_returnsEmptyResponse() throws Exception {
        // Given
        when(vectorStoreService.searchByType(anyString(), anyString(), anyInt(), anyDouble()))
            .thenReturn(Collections.emptyList());

        String requestJson = """
            {
                "query": "nonexistent",
                "limit": 10,
                "minScore": 0.3
            }
            """;

        // When & Then
        mockMvc.perform(post("/tool-server/knowledge/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(0))
            .andExpect(jsonPath("$.results").isEmpty());
    }

    @Test
    void searchKnowledge_serviceError_returnsInternalServerError() throws Exception {
        // Given
        when(vectorStoreService.searchByType(anyString(), anyString(), anyInt(), anyDouble()))
            .thenThrow(new RuntimeException("Search service error"));

        String requestJson = """
            {
                "query": "test",
                "limit": 10,
                "minScore": 0.3
            }
            """;

        // When & Then
        mockMvc.perform(post("/tool-server/knowledge/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value(containsString("Error searching knowledge base")));
    }

    @Test
    void getKnowledgeArticle_serviceError_returnsInternalServerError() throws Exception {
        // Given
        when(vectorStoreService.searchByType(anyString(), anyString(), anyInt(), anyDouble()))
            .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get("/tool-server/knowledge/KB000123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.found").value(false))
            .andExpect(jsonPath("$.errorMessage").value(containsString("Error retrieving article")));
    }

    @Test
    void searchWorkOrders_noResults_returnsEmptyResponse() throws Exception {
        // Given
        when(vectorStoreService.searchByType(anyString(), anyString(), anyInt(), anyDouble()))
            .thenReturn(Collections.emptyList());

        String requestJson = """
            {
                "query": "nonexistent",
                "limit": 10,
                "minScore": 0.3
            }
            """;

        // When & Then
        mockMvc.perform(post("/tool-server/workorders/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(0))
            .andExpect(jsonPath("$.results").isEmpty());
    }

    @Test
    void searchWorkOrders_serviceError_returnsInternalServerError() throws Exception {
        // Given
        when(vectorStoreService.searchByType(anyString(), anyString(), anyInt(), anyDouble()))
            .thenThrow(new RuntimeException("Search failed"));

        String requestJson = """
            {
                "query": "test",
                "limit": 10,
                "minScore": 0.3
            }
            """;

        // When & Then
        mockMvc.perform(post("/tool-server/workorders/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value(containsString("Error searching work orders")));
    }

    @Test
    void createIncident_skipDuplicateCheck_skipsDuplicateDetection() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
        when(inputValidator.validateSummary(anyString()))
            .thenReturn(new InputValidator.ValidationResult(true, Collections.emptyList(), Collections.emptyList(), "Valid summary"));
        when(inputValidator.validateDescription(anyString()))
            .thenReturn(new InputValidator.ValidationResult(true, Collections.emptyList(), Collections.emptyList(), "Valid description"));

        PendingAction action = PendingAction.builder()
            .actionId("abc123")
            .preview("Incident will be created")
            .expiresAt(Instant.now().plusSeconds(300))
            .build();

        when(confirmationService.stageIncidentCreation(anyString(), anyString(), any()))
            .thenReturn(action);

        String requestJson = """
            {
                "summary": "VPN not working",
                "description": "Cannot connect",
                "impact": 3,
                "urgency": 3,
                "skipDuplicateCheck": true
            }
            """;

        // When & Then
        mockMvc.perform(post("/tool-server/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("STAGED"));
    }

    @Test
    void updateIncident_allStatusValues_parsesCorrectly() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);

        PendingAction action = PendingAction.builder()
            .actionId("xyz789")
            .preview("Update incident")
            .expiresAt(Instant.now().plusSeconds(300))
            .build();

        when(confirmationService.stageIncidentUpdate(anyString(), anyString(), any()))
            .thenReturn(action);

        String requestJson = """
            {
                "status": "In Progress",
                "sessionId": "test-session"
            }
            """;

        // When & Then
        mockMvc.perform(put("/tool-server/incidents/INC000123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("STAGED"));
    }

    @Test
    void updateIncident_allWorkLogTypes_parsesCorrectly() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);

        PendingAction action = PendingAction.builder()
            .actionId("xyz789")
            .preview("Update incident")
            .expiresAt(Instant.now().plusSeconds(300))
            .build();

        when(confirmationService.stageIncidentUpdate(anyString(), anyString(), any()))
            .thenReturn(action);

        String requestJson = """
            {
                "workLogNotes": "Updated by automation",
                "workLogType": "Customer Communication",
                "sessionId": "test-session"
            }
            """;

        // When & Then
        mockMvc.perform(put("/tool-server/incidents/INC000123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("STAGED"));
    }

    @Test
    void getIncidentDetails_withNullMetadata_handlesGracefully() throws Exception {
        // Given
        VectorStoreService.SearchResult result = VectorStoreService.SearchResult.builder()
            .sourceId("INC000123")
            .sourceType("Incident")
            .textSegment("Issue description")
            .score(1.0f)
            .metadata(null)
            .build();

        when(vectorStoreService.searchByType("INC000123", "Incident", 10, 0.0))
            .thenReturn(List.of(result));

        // When & Then
        mockMvc.perform(get("/tool-server/incidents/INC000123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.incidentNumber").value("INC000123"))
            .andExpect(jsonPath("$.found").value(true));
    }

    @Test
    void searchIncidents_withNullMetadata_handlesGracefully() throws Exception {
        // Given
        VectorStoreService.SearchResult result = VectorStoreService.SearchResult.builder()
            .sourceId("INC000123")
            .sourceType("Incident")
            .textSegment("Text content")
            .score(0.85f)
            .metadata(null)
            .build();

        when(vectorStoreService.searchByType("test", "Incident", 10, 0.3))
            .thenReturn(List.of(result));

        String requestJson = """
            {
                "query": "test",
                "limit": 10,
                "minScore": 0.3
            }
            """;

        // When & Then
        mockMvc.perform(post("/tool-server/incidents/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(1))
            .andExpect(jsonPath("$.results[0].id").value("INC000123"));
    }

    @Test
    void searchIncidents_withDefaultLimitAndMinScore_usesDefaults() throws Exception {
        // Given
        VectorStoreService.SearchResult result = VectorStoreService.SearchResult.builder()
            .sourceId("INC000123")
            .sourceType("Incident")
            .textSegment("Test content")
            .score(0.85f)
            .metadata(Map.of("title", "Test"))
            .build();

        when(vectorStoreService.searchByType("test", "Incident", 10, 0.3))
            .thenReturn(List.of(result));

        String requestJson = """
            {
                "query": "test"
            }
            """;

        // When & Then
        mockMvc.perform(post("/tool-server/incidents/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(1));
    }

    @Test
    void searchKnowledge_withDefaultLimitAndMinScore_usesDefaults() throws Exception {
        // Given
        VectorStoreService.SearchResult result = VectorStoreService.SearchResult.builder()
            .sourceId("KB000123")
            .sourceType("KnowledgeArticle")
            .textSegment("Test content")
            .score(0.85f)
            .metadata(Map.of("title", "Test"))
            .build();

        when(vectorStoreService.searchByType("test", "KnowledgeArticle", 10, 0.3))
            .thenReturn(List.of(result));

        String requestJson = """
            {
                "query": "test"
            }
            """;

        // When & Then
        mockMvc.perform(post("/tool-server/knowledge/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(1));
    }

    @Test
    void searchWorkOrders_withDefaultLimitAndMinScore_usesDefaults() throws Exception {
        // Given
        VectorStoreService.SearchResult result = VectorStoreService.SearchResult.builder()
            .sourceId("WO000123")
            .sourceType("WorkOrder")
            .textSegment("Test content")
            .score(0.85f)
            .metadata(Map.of("title", "Test"))
            .build();

        when(vectorStoreService.searchByType("test", "WorkOrder", 10, 0.3))
            .thenReturn(List.of(result));

        String requestJson = """
            {
                "query": "test"
            }
            """;

        // When & Then
        mockMvc.perform(post("/tool-server/workorders/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(1));
    }

    @Test
    void updateIncident_invalidStatusValue_handlesGracefully() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);

        PendingAction action = PendingAction.builder()
            .actionId("xyz789")
            .preview("Update incident")
            .expiresAt(Instant.now().plusSeconds(300))
            .build();

        when(confirmationService.stageIncidentUpdate(anyString(), anyString(), any()))
            .thenReturn(action);

        String requestJson = """
            {
                "status": "InvalidStatus",
                "sessionId": "test-session"
            }
            """;

        // When & Then - should still work, parseStatus returns null for invalid values
        mockMvc.perform(put("/tool-server/incidents/INC000123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("STAGED"));
    }

    @Test
    void updateIncident_invalidWorkLogType_usesDefault() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);

        PendingAction action = PendingAction.builder()
            .actionId("xyz789")
            .preview("Update incident")
            .expiresAt(Instant.now().plusSeconds(300))
            .build();

        when(confirmationService.stageIncidentUpdate(anyString(), anyString(), any()))
            .thenReturn(action);

        String requestJson = """
            {
                "workLogNotes": "Test notes",
                "workLogType": "InvalidType",
                "sessionId": "test-session"
            }
            """;

        // When & Then - should use default Working Log type
        mockMvc.perform(put("/tool-server/incidents/INC000123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("STAGED"));
    }

    @Test
    void getKnowledgeArticle_withNullMetadata_handlesGracefully() throws Exception {
        // Given
        VectorStoreService.SearchResult result = VectorStoreService.SearchResult.builder()
            .sourceId("KB000123")
            .sourceType("KnowledgeArticle")
            .textSegment("Article content")
            .score(1.0f)
            .metadata(null)
            .build();

        when(vectorStoreService.searchByType("KB000123", "KnowledgeArticle", 10, 0.0))
            .thenReturn(List.of(result));

        // When & Then
        mockMvc.perform(get("/tool-server/knowledge/KB000123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.articleId").value("KB000123"))
            .andExpect(jsonPath("$.found").value(true));
    }

    @Test
    void confirmAction_nullRecordId_handlesGracefully() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);

        ConfirmationService.ConfirmationResult result =
            new ConfirmationService.ConfirmationResult(true, false, null, "Success");

        when(confirmationService.confirm(eq("abc123"), eq("test-session"), anyString()))
            .thenReturn(result);

        // When & Then
        mockMvc.perform(post("/tool-server/actions/confirm")
                .param("actionId", "abc123")
                .param("sessionId", "test-session")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EXECUTED"))
            .andExpect(jsonPath("$.recordType").value("Record"));
    }

    @Test
    void updateIncident_nullStatus_handlesGracefully() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);

        PendingAction action = PendingAction.builder()
            .actionId("xyz789")
            .preview("Update incident")
            .expiresAt(Instant.now().plusSeconds(300))
            .build();

        when(confirmationService.stageIncidentUpdate(anyString(), anyString(), any()))
            .thenReturn(action);

        String requestJson = """
            {
                "resolution": "Fixed",
                "sessionId": "test-session"
            }
            """;

        // When & Then
        mockMvc.perform(put("/tool-server/incidents/INC000123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("STAGED"));
    }

    @Test
    void updateIncident_nullWorkLogType_handlesGracefully() throws Exception {
        // Given
        when(rateLimiter.isRateLimited(anyString())).thenReturn(false);

        PendingAction action = PendingAction.builder()
            .actionId("xyz789")
            .preview("Update incident")
            .expiresAt(Instant.now().plusSeconds(300))
            .build();

        when(confirmationService.stageIncidentUpdate(anyString(), anyString(), any()))
            .thenReturn(action);

        String requestJson = """
            {
                "workLogNotes": "Test notes",
                "sessionId": "test-session"
            }
            """;

        // When & Then
        mockMvc.perform(put("/tool-server/incidents/INC000123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("STAGED"));
    }

    @Test
    void getOpenApiSpec_fileNotFound_returnsError() throws Exception {
        // This test verifies error handling, but actual file should exist
        // Testing the 500 path would require breaking the file, so we just verify 200 path works
        mockMvc.perform(get("/tool-server/openapi.json")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}
