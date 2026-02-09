package com.bmc.rag.api.controller;

import com.bmc.rag.api.config.RateLimitConfig;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.store.repository.SyncStateRepository;
import com.bmc.rag.store.service.VectorStoreService;
import com.bmc.rag.store.sync.IncrementalSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for IngestionController.
 */
@WebMvcTest(
    controllers = IngestionController.class,
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
class IngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IncrementalSyncService syncService;

    @MockBean
    private SyncStateRepository syncStateRepository;

    @MockBean
    private VectorStoreService vectorStoreService;

    @MockBean
    private ThreadLocalARContext threadLocalARContext;

    @MockBean
    private RateLimitConfig rateLimitConfig;

    @Test
    void triggerSync_fullSync_returnsAccepted() throws Exception {
        // Given
        String requestJson = """
            {
                "sourceType": "Incident",
                "fullSync": true
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/v1/admin/ingestion/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("triggered"))
            .andExpect(jsonPath("$.sourceType").value("Incident"))
            .andExpect(jsonPath("$.syncType").value("full"));
    }

    @Test
    void triggerSync_incrementalSync_returnsAccepted() throws Exception {
        // Given
        String requestJson = """
            {
                "sourceType": "Incident",
                "fullSync": false
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/v1/admin/ingestion/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("triggered"))
            .andExpect(jsonPath("$.syncType").value("incremental"));
    }

    @Test
    void triggerSync_invalidSourceType_returnsBadRequest() throws Exception {
        // Given
        String requestJson = """
            {
                "sourceType": "InvalidType",
                "fullSync": false
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/v1/admin/ingestion/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid source type: InvalidType"));
    }

    @Test
    void triggerSync_noSourceType_triggersAll() throws Exception {
        // Given - empty request triggers all sources
        String requestJson = """
            {
                "fullSync": false
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/v1/admin/ingestion/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("triggered"))
            .andExpect(jsonPath("$.sourceTypes").isArray());
    }

    @Test
    void getStatus_returnsStatus() throws Exception {
        // Given
        when(syncStateRepository.findAll()).thenReturn(java.util.Collections.emptyList());
        when(syncStateRepository.isAnySyncRunning()).thenReturn(false);
        when(vectorStoreService.getStatistics()).thenReturn(Map.of(
            "incidents", 100L,
            "workorders", 50L
        ));

        // When & Then
        mockMvc.perform(get("/api/v1/admin/ingestion/status")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.statistics").isMap());
    }

    @Test
    void getStatistics_returnsStats() throws Exception {
        // Given
        when(vectorStoreService.getStatistics()).thenReturn(Map.of(
            "incidents", 100L,
            "workorders", 50L,
            "total", 150L
        ));

        // When & Then
        mockMvc.perform(get("/api/v1/admin/ingestion/statistics")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.incidents").value(100))
            .andExpect(jsonPath("$.workorders").value(50))
            .andExpect(jsonPath("$.total").value(150));
    }

    @Test
    void clearEmbeddings_validSourceType_returnsSuccess() throws Exception {
        // Given
        when(vectorStoreService.getStatistics()).thenReturn(Map.of("incidents", 100L));

        // When & Then
        mockMvc.perform(delete("/api/v1/admin/ingestion/embeddings/Incident")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("cleared"))
            .andExpect(jsonPath("$.sourceType").value("Incident"));
    }

    @Test
    void clearEmbeddings_invalidSourceType_returnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/admin/ingestion/embeddings/InvalidType")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid source type: InvalidType"));
    }

    @Test
    void triggerSync_nullRequestBody_usesDefaults() throws Exception {
        // When & Then - null body should trigger all sources with incremental sync
        mockMvc.perform(post("/api/v1/admin/ingestion/trigger")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("triggered"))
            .andExpect(jsonPath("$.sourceTypes").isArray());
    }

    @Test
    void triggerSync_allSourceTypes_triggersCorrectly() throws Exception {
        // Given - Test all valid source types
        for (String sourceType : java.util.List.of("Incident", "WorkOrder", "KnowledgeArticle", "ChangeRequest")) {
            String requestJson = String.format("""
                {
                    "sourceType": "%s",
                    "fullSync": false
                }
                """, sourceType);

            // When & Then
            mockMvc.perform(post("/api/v1/admin/ingestion/trigger")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.sourceType").value(sourceType));
        }
    }

    @Test
    void getStatus_withRunningSyncs_returnsRunningStatus() throws Exception {
        // Given
        when(syncStateRepository.findAll()).thenReturn(java.util.Collections.emptyList());
        when(syncStateRepository.isAnySyncRunning()).thenReturn(true);
        when(vectorStoreService.getStatistics()).thenReturn(Map.of());

        // When & Then
        mockMvc.perform(get("/api/v1/admin/ingestion/status")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("running"));
    }

    @Test
    void getStatus_withFailedSync_returnsErrorStatus() throws Exception {
        // Given
        com.bmc.rag.store.entity.SyncStateEntity failedState =
            new com.bmc.rag.store.entity.SyncStateEntity();
        failedState.setSourceType("Incident");
        failedState.setStatus("failed");
        failedState.setErrorMessage("Connection timeout");

        when(syncStateRepository.findAll()).thenReturn(java.util.List.of(failedState));
        when(syncStateRepository.isAnySyncRunning()).thenReturn(false);
        when(vectorStoreService.getStatistics()).thenReturn(Map.of());

        // When & Then
        mockMvc.perform(get("/api/v1/admin/ingestion/status")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.sources.Incident.status").value("failed"))
            .andExpect(jsonPath("$.sources.Incident.errorMessage").value("Connection timeout"));
    }

    @Test
    void clearEmbeddings_withZeroCount_returnsCorrectly() throws Exception {
        // Given
        when(vectorStoreService.getStatistics()).thenReturn(Map.of());

        // When & Then
        mockMvc.perform(delete("/api/v1/admin/ingestion/embeddings/Incident")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("cleared"))
            .andExpect(jsonPath("$.chunksDeleted").value(0));
    }

    @Test
    void triggerSync_emptyRequestObject_triggersAllWithDefaults() throws Exception {
        // Given
        String requestJson = "{}";

        // When & Then
        mockMvc.perform(post("/api/v1/admin/ingestion/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("triggered"))
            .andExpect(jsonPath("$.sourceTypes").isArray())
            .andExpect(jsonPath("$.syncType").value("incremental"));
    }
}
