package com.bmc.rag.api.controller;

import com.bmc.rag.agent.metrics.RagMetricsService;
import com.bmc.rag.agent.metrics.RagMetricsService.MetricsSnapshot;
import com.bmc.rag.api.config.RateLimitConfig;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for MetricsController.
 */
@WebMvcTest(
    controllers = MetricsController.class,
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
class MetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RagMetricsService metricsService;

    @MockBean
    private ThreadLocalARContext threadLocalARContext;

    @MockBean
    private RateLimitConfig rateLimitConfig;

    @Test
    void getRagMetrics_returnsSnapshot() throws Exception {
        // Given
        MetricsSnapshot snapshot = MetricsSnapshot.builder()
            .totalRetrievals(100L)
            .activeRetrievals(5)
            .lastRetrievalCount(10)
            .retrievalP50Ms(50.0)
            .retrievalP95Ms(150.0)
            .build();

        when(metricsService.getSnapshot()).thenReturn(snapshot);

        // When & Then
        mockMvc.perform(get("/api/v1/metrics/rag"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRetrievals").value(100))
            .andExpect(jsonPath("$.activeRetrievals").value(5));
    }

    @Test
    void getRagMetricsSummary_returnsSummary() throws Exception {
        // Given
        MetricsSnapshot snapshot = MetricsSnapshot.builder()
            .totalRetrievals(100L)
            .retrievalP50Ms(50.0)
            .cacheHitRate(0.75)
            .build();

        when(metricsService.getSnapshot()).thenReturn(snapshot);

        // When & Then
        mockMvc.perform(get("/api/v1/metrics/rag/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.retrievals").exists())
            .andExpect(jsonPath("$.latency").exists())
            .andExpect(jsonPath("$.cache").exists());
    }

    @Test
    void getLatencyMetrics_returnsLatencyData() throws Exception {
        // Given
        MetricsSnapshot snapshot = MetricsSnapshot.builder()
            .retrievalP50Ms(50.0)
            .retrievalP95Ms(150.0)
            .retrievalP99Ms(200.0)
            .generationP50Ms(100.0)
            .generationP95Ms(300.0)
            .totalP50Ms(150.0)
            .totalP95Ms(450.0)
            .build();

        when(metricsService.getSnapshot()).thenReturn(snapshot);

        // When & Then
        mockMvc.perform(get("/api/v1/metrics/rag/latency"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.retrieval.p50").value(50.0))
            .andExpect(jsonPath("$.generation.p50").value(100.0))
            .andExpect(jsonPath("$.total.p50").value(150.0));
    }

    @Test
    void getQualityMetrics_returnsQualityData() throws Exception {
        // Given
        MetricsSnapshot snapshot = MetricsSnapshot.builder()
            .averageGroundednessScore(0.85)
            .hallucinationsDetected(5L)
            .totalCitations(200L)
            .totalRetrievals(100L)
            .build();

        when(metricsService.getSnapshot()).thenReturn(snapshot);

        // When & Then
        mockMvc.perform(get("/api/v1/metrics/rag/quality"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.groundednessScore").value(0.85))
            .andExpect(jsonPath("$.hallucinationsDetected").value(5));
    }

    @Test
    void getCacheMetrics_returnsCacheData() throws Exception {
        // Given
        MetricsSnapshot snapshot = MetricsSnapshot.builder()
            .cacheHitRate(0.75)
            .cacheHits(750L)
            .cacheMisses(250L)
            .build();

        when(metricsService.getSnapshot()).thenReturn(snapshot);

        // When & Then
        mockMvc.perform(get("/api/v1/metrics/rag/cache"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hitRate").value(0.75))
            .andExpect(jsonPath("$.hits").value(750))
            .andExpect(jsonPath("$.misses").value(250));
    }
}
