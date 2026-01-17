package com.bmc.rag.api.controller;

import com.bmc.rag.api.dto.FeedbackRequest;
import com.bmc.rag.api.dto.FeedbackResponse;
import com.bmc.rag.api.dto.FeedbackStats;
import com.bmc.rag.api.service.FeedbackService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API controller for user feedback on AI responses.
 * Captures positive/negative feedback and detailed comments for model improvement.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * Submit feedback for a specific message.
     *
     * @param request Feedback request containing message ID and feedback type
     * @return Feedback response with confirmation
     */
    @PostMapping
    @RateLimiter(name = "feedback")
    public ResponseEntity<FeedbackResponse> submitFeedback(@Valid @RequestBody FeedbackRequest request) {
        log.info("Feedback received for message {} in session {}: {}",
            request.getMessageId(),
            request.getSessionId(),
            request.getFeedbackType());

        FeedbackResponse response = feedbackService.saveFeedback(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get feedback statistics for a session.
     *
     * @param sessionId Session ID to get stats for
     * @return Feedback statistics
     */
    @GetMapping("/stats/{sessionId}")
    public ResponseEntity<FeedbackStats> getFeedbackStats(@PathVariable String sessionId) {
        log.info("Getting feedback stats for session: {}", sessionId);

        FeedbackStats stats = feedbackService.getStatsForSession(sessionId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get all feedback (admin endpoint).
     *
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Paginated list of feedback entries
     */
    @GetMapping
    public ResponseEntity<?> getAllFeedback(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.info("Getting all feedback, page {} size {}", page, size);

        return ResponseEntity.ok(feedbackService.getAllFeedback(page, size));
    }
}
