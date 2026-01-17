package com.bmc.rag.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Session summary DTO for listing chat sessions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionSummary {

    /**
     * Session ID.
     */
    private String sessionId;

    /**
     * Session title (derived from first user message).
     */
    private String title;

    /**
     * Number of messages in the session.
     */
    private int messageCount;

    /**
     * Timestamp of the last message (Unix epoch millis).
     */
    private long lastUpdated;
}
