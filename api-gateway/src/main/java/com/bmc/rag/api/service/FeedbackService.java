package com.bmc.rag.api.service;

import com.bmc.rag.api.dto.FeedbackRequest;
import com.bmc.rag.api.dto.FeedbackResponse;
import com.bmc.rag.api.dto.FeedbackStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing user feedback on AI responses.
 * Stores feedback in PostgreSQL for analysis and model improvement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Save feedback to the database.
     *
     * @param request Feedback request
     * @return Feedback response with confirmation
     */
    public FeedbackResponse saveFeedback(FeedbackRequest request) {
        String id = UUID.randomUUID().toString();
        String createdAt = Instant.now().toString();

        try {
            jdbcTemplate.update(
                """
                INSERT INTO feedback (id, message_id, session_id, feedback_type, feedback_text, user_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                request.getMessageId(),
                request.getSessionId(),
                request.getFeedbackType(),
                request.getFeedbackText(),
                request.getUserId(),
                createdAt
            );

            log.info("Feedback saved: {} for message {}", request.getFeedbackType(), request.getMessageId());

            return FeedbackResponse.builder()
                .id(id)
                .messageId(request.getMessageId())
                .status("received")
                .createdAt(createdAt)
                .build();

        } catch (Exception e) {
            log.error("Failed to save feedback: {}", e.getMessage(), e);
            // Return response anyway to not block the user
            return FeedbackResponse.builder()
                .id(id)
                .messageId(request.getMessageId())
                .status("received")
                .createdAt(createdAt)
                .build();
        }
    }

    /**
     * Get feedback statistics for a session.
     *
     * @param sessionId Session ID
     * @return Feedback statistics
     */
    public FeedbackStats getStatsForSession(String sessionId) {
        try {
            Long positive = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM feedback WHERE session_id = ? AND feedback_type = 'positive'",
                Long.class,
                sessionId
            );

            Long negative = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM feedback WHERE session_id = ? AND feedback_type = 'negative'",
                Long.class,
                sessionId
            );

            return FeedbackStats.builder()
                .sessionId(sessionId)
                .positive(positive != null ? positive : 0)
                .negative(negative != null ? negative : 0)
                .total((positive != null ? positive : 0) + (negative != null ? negative : 0))
                .build();

        } catch (Exception e) {
            log.error("Failed to get feedback stats: {}", e.getMessage(), e);
            return FeedbackStats.builder()
                .sessionId(sessionId)
                .positive(0)
                .negative(0)
                .total(0)
                .build();
        }
    }

    /**
     * Get all feedback entries (paginated).
     *
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return List of feedback entries
     */
    public List<Map<String, Object>> getAllFeedback(int page, int size) {
        int offset = page * size;

        try {
            return jdbcTemplate.queryForList(
                """
                SELECT id, message_id, session_id, feedback_type, feedback_text, user_id, created_at
                FROM feedback
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """,
                size,
                offset
            );
        } catch (Exception e) {
            log.error("Failed to get all feedback: {}", e.getMessage(), e);
            return List.of();
        }
    }
}
