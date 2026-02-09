package com.bmc.rag.api.controller;

import com.bmc.rag.api.config.RateLimitConfig;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.store.service.EmbeddingRefreshService;
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

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AdminController.
 */
@WebMvcTest(
    controllers = AdminController.class,
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmbeddingRefreshService embeddingRefreshService;

    @MockBean
    private VectorStoreService vectorStoreService;

    @MockBean
    private ThreadLocalARContext threadLocalARContext;

    @MockBean
    private RateLimitConfig rateLimitConfig;

    @Test
    void reembedAll_successful_returnsCount() throws Exception {
        // Given
        when(embeddingRefreshService.refreshAllEmbeddings()).thenReturn(150);

        // When & Then
        mockMvc.perform(post("/api/admin/reembed")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("completed"))
            .andExpect(jsonPath("$.count").value(150))
            .andExpect(jsonPath("$.durationMs").exists());
    }

    @Test
    void reembedBySourceType_successful_returnsCount() throws Exception {
        // Given
        when(embeddingRefreshService.refreshEmbeddingsBySourceType("Incident")).thenReturn(50);

        // When & Then
        mockMvc.perform(post("/api/admin/reembed/Incident")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("completed"))
            .andExpect(jsonPath("$.sourceType").value("Incident"))
            .andExpect(jsonPath("$.count").value(50));
    }

    @Test
    void getStats_returnsStatistics() throws Exception {
        // Given
        when(vectorStoreService.getStatistics()).thenReturn(Map.of(
            "incidents", 100L,
            "workorders", 50L,
            "total", 150L
        ));

        // When & Then
        mockMvc.perform(get("/api/admin/stats")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.incidents").value(100))
            .andExpect(jsonPath("$.workorders").value(50))
            .andExpect(jsonPath("$.total").value(150));
    }
}
