package com.bmc.rag.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for submitting user feedback on AI responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {

    /**
     * ID of the message receiving feedback.
     */
    @NotBlank(message = "Message ID is required")
    private String messageId;

    /**
     * Session ID where the message was generated.
     */
    @NotBlank(message = "Session ID is required")
    private String sessionId;

    /**
     * Type of feedback: 'positive' or 'negative'.
     */
    @NotNull(message = "Feedback type is required")
    @Pattern(regexp = "^(positive|negative)$", message = "Feedback type must be 'positive' or 'negative'")
    private String feedbackType;

    /**
     * Optional detailed feedback text (for negative feedback).
     */
    private String feedbackText;

    /**
     * Optional user ID for tracking.
     */
    private String userId;

    /**
     * Timestamp when feedback was submitted.
     */
    private String timestamp;
}
