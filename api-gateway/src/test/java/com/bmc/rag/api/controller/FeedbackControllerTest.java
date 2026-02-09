package com.bmc.rag.api.controller;

import com.bmc.rag.api.config.RateLimitConfig;
import com.bmc.rag.api.dto.FeedbackResponse;
import com.bmc.rag.api.dto.FeedbackStats;
import com.bmc.rag.api.service.FeedbackService;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for FeedbackController.
 */
@WebMvcTest(
    controllers = FeedbackController.class,
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FeedbackService feedbackService;

    @MockBean
    private ThreadLocalARContext threadLocalARContext;

    @MockBean
    private RateLimitConfig rateLimitConfig;

    @Test
    void submitFeedback_validRequest_returnsSuccess() throws Exception {
        // Given
        FeedbackResponse response = FeedbackResponse.builder()
            .id(UUID.randomUUID().toString())
            .messageId("msg-123")
            .status("received")
            .createdAt(Instant.now().toString())
            .build();

        when(feedbackService.saveFeedback(any())).thenReturn(response);

        String requestJson = """
            {
                "messageId": "msg-123",
                "sessionId": "sess-123",
                "feedbackType": "positive",
                "feedbackText": "Great response!"
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/v1/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messageId").value("msg-123"))
            .andExpect(jsonPath("$.status").value("received"));
    }

    @Test
    void getFeedbackStats_returnsStatistics() throws Exception {
        // Given
        FeedbackStats stats = FeedbackStats.builder()
            .sessionId("sess-123")
            .positive(10L)
            .negative(2L)
            .total(12L)
            .build();

        when(feedbackService.getStatsForSession("sess-123")).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/v1/feedback/stats/sess-123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value("sess-123"))
            .andExpect(jsonPath("$.positive").value(10))
            .andExpect(jsonPath("$.negative").value(2))
            .andExpect(jsonPath("$.total").value(12));
    }

    @Test
    void getAllFeedback_returnsPaginatedResults() throws Exception {
        // Given
        when(feedbackService.getAllFeedback(0, 50)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/feedback")
                .param("page", "0")
                .param("size", "50")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}
