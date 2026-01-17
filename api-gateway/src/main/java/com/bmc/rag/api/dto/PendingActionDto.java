package com.bmc.rag.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for pending action information (for API responses).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingActionDto {

    /**
     * The action ID for confirmation.
     */
    private String actionId;

    /**
     * The type of action.
     */
    private String actionType;

    /**
     * Human-readable preview of the action.
     */
    private String preview;

    /**
     * When the action was staged.
     */
    private Instant stagedAt;

    /**
     * When the action will expire.
     */
    private Instant expiresAt;

    /**
     * Current status of the action.
     */
    private String status;

    /**
     * Seconds until the action expires.
     */
    private long secondsUntilExpiry;
}
