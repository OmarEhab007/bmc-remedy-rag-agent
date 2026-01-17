package com.bmc.rag.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for feedback statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackStats {

    /**
     * Number of positive feedback entries.
     */
    private long positive;

    /**
     * Number of negative feedback entries.
     */
    private long negative;

    /**
     * Total number of feedback entries.
     */
    private long total;

    /**
     * Session ID these stats are for (if applicable).
     */
    private String sessionId;
}
