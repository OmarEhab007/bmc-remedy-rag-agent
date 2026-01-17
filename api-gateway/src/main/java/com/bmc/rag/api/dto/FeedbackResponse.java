package com.bmc.rag.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for feedback submission confirmation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {

    /**
     * Unique ID of the stored feedback entry.
     */
    private String id;

    /**
     * ID of the message that received feedback.
     */
    private String messageId;

    /**
     * Status of the feedback: 'received' or 'processed'.
     */
    private String status;

    /**
     * Timestamp when feedback was recorded.
     */
    private String createdAt;
}
