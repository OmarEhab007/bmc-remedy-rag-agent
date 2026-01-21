package com.bmc.rag.api.dto.toolserver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for action confirmation/cancellation via Tool Server.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmActionResponse {

    /**
     * Whether the operation was successful.
     */
    private Boolean success;

    /**
     * Response status: EXECUTED, CANCELLED, FAILED, EXPIRED, NOT_FOUND
     */
    private String status;

    /**
     * Human-readable message about the result.
     */
    private String message;

    /**
     * The action ID that was processed.
     */
    private String actionId;

    /**
     * The created record ID (if action was executed successfully).
     */
    private String recordId;

    /**
     * The type of record created (Incident, WorkOrder, etc.).
     */
    private String recordType;

    /**
     * Error details if failed.
     */
    private String errorDetail;

    /**
     * Create an executed response.
     */
    public static ConfirmActionResponse executed(String actionId, String recordId, String recordType) {
        return ConfirmActionResponse.builder()
            .success(true)
            .status("EXECUTED")
            .message(recordType + " " + recordId + " created successfully.")
            .actionId(actionId)
            .recordId(recordId)
            .recordType(recordType)
            .build();
    }

    /**
     * Create a cancelled response.
     */
    public static ConfirmActionResponse cancelled(String actionId) {
        return ConfirmActionResponse.builder()
            .success(true)
            .status("CANCELLED")
            .message("Action cancelled successfully.")
            .actionId(actionId)
            .build();
    }

    /**
     * Create an expired response.
     */
    public static ConfirmActionResponse expired(String actionId) {
        return ConfirmActionResponse.builder()
            .success(false)
            .status("EXPIRED")
            .message("Action has expired. Please create a new request.")
            .actionId(actionId)
            .build();
    }

    /**
     * Create a not-found response.
     */
    public static ConfirmActionResponse notFound(String actionId) {
        return ConfirmActionResponse.builder()
            .success(false)
            .status("NOT_FOUND")
            .message("Action not found or has already been processed.")
            .actionId(actionId)
            .build();
    }

    /**
     * Create a failed response.
     */
    public static ConfirmActionResponse failed(String actionId, String message, String detail) {
        return ConfirmActionResponse.builder()
            .success(false)
            .status("FAILED")
            .message(message)
            .actionId(actionId)
            .errorDetail(detail)
            .build();
    }
}
