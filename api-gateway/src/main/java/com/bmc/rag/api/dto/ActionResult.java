package com.bmc.rag.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result DTO for action confirmation/cancellation operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionResult {

    /**
     * Whether the operation was successful.
     */
    private boolean success;

    /**
     * The action ID that was processed.
     */
    private String actionId;

    /**
     * The resulting status of the action.
     */
    private String status;

    /**
     * The created record ID (if action was executed).
     */
    private String recordId;

    /**
     * Human-readable message about the result.
     */
    private String message;

    /**
     * Timestamp of the operation.
     */
    @Builder.Default
    private long timestamp = System.currentTimeMillis();

    /**
     * Create a success result.
     */
    public static ActionResult success(String actionId, String status, String recordId, String message) {
        return ActionResult.builder()
            .success(true)
            .actionId(actionId)
            .status(status)
            .recordId(recordId)
            .message(message)
            .build();
    }

    /**
     * Create a failure result.
     */
    public static ActionResult failure(String actionId, String message) {
        return ActionResult.builder()
            .success(false)
            .actionId(actionId)
            .status("FAILED")
            .message(message)
            .build();
    }

    /**
     * Create a cancellation result.
     * <p>
     * Note: {@code success} is set to {@code true} because cancellation represents
     * a successful operation -- the system successfully processed the cancellation
     * request. It does not indicate that the original action was executed.
     */
    public static ActionResult cancelled(String actionId, String message) {
        return ActionResult.builder()
            .success(true)
            .actionId(actionId)
            .status("CANCELLED")
            .message(message)
            .build();
    }
}
