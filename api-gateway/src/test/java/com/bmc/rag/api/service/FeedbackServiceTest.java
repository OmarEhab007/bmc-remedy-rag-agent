package com.bmc.rag.api.service;

import com.bmc.rag.api.dto.FeedbackRequest;
import com.bmc.rag.api.dto.FeedbackResponse;
import com.bmc.rag.api.dto.FeedbackStats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FeedbackService.
 */
@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private FeedbackService feedbackService;

    @Test
    void saveFeedback_validRequest_returnsResponse() {
        // Given
        FeedbackRequest request = FeedbackRequest.builder()
            .messageId("msg-123")
            .sessionId("sess-123")
            .feedbackType("positive")
            .feedbackText("Great!")
            .userId("user-1")
            .build();

        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(1);

        // When
        FeedbackResponse response = feedbackService.saveFeedback(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessageId()).isEqualTo("msg-123");
        assertThat(response.getStatus()).isEqualTo("received");
        verify(jdbcTemplate).update(anyString(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void saveFeedback_databaseError_stillReturnsResponse() {
        // Given
        FeedbackRequest request = FeedbackRequest.builder()
            .messageId("msg-123")
            .sessionId("sess-123")
            .feedbackType("positive")
            .build();

        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any()))
            .thenThrow(new RuntimeException("DB error"));

        // When
        FeedbackResponse response = feedbackService.saveFeedback(request);

        // Then - should not throw, returns response anyway
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("received");
    }

    @Test
    void getStatsForSession_returnsStatistics() {
        // Given
        when(jdbcTemplate.queryForObject(contains("feedback_type = 'positive'"), eq(Long.class), anyString()))
            .thenReturn(10L);
        when(jdbcTemplate.queryForObject(contains("feedback_type = 'negative'"), eq(Long.class), anyString()))
            .thenReturn(2L);

        // When
        FeedbackStats stats = feedbackService.getStatsForSession("sess-123");

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.getSessionId()).isEqualTo("sess-123");
        assertThat(stats.getPositive()).isEqualTo(10);
        assertThat(stats.getNegative()).isEqualTo(2);
        assertThat(stats.getTotal()).isEqualTo(12);
    }

    @Test
    void getStatsForSession_databaseError_returnsZeroStats() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString()))
            .thenThrow(new RuntimeException("DB error"));

        // When
        FeedbackStats stats = feedbackService.getStatsForSession("sess-123");

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.getTotal()).isEqualTo(0);
    }

    @Test
    void getAllFeedback_returnsPaginatedResults() {
        // Given
        when(jdbcTemplate.queryForList(anyString(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());

        // When
        List<Map<String, Object>> results = feedbackService.getAllFeedback(0, 50);

        // Then
        assertThat(results).isNotNull().isEmpty();
        verify(jdbcTemplate).queryForList(anyString(), eq(50), eq(0));
    }
}
