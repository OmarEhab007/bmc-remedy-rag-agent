package com.bmc.rag.api.dto.toolserver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for incident update operations via Tool Server.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateIncidentResponse {

    /**
     * Whether the operation was successful.
     */
    private Boolean success;

    /**
     * Response status: STAGED, UPDATED, FAILED
     */
    private String status;

    /**
     * Human-readable message about the result.
     */
    private String message;

    /**
     * The incident number that was updated.
     */
    private String incidentNumber;

    /**
     * The staged action ID (if status is STAGED).
     */
    private String actionId;

    /**
     * When the staged action expires (if status is STAGED).
     */
    private Instant expiresAt;

    /**
     * Preview of the update (if status is STAGED).
     */
    private String preview;

    /**
     * Error details if failed.
     */
    private String errorDetail;

    /**
     * Create a staged response.
     */
    public static UpdateIncidentResponse staged(
            String incidentNumber,
            String actionId,
            String preview,
            Instant expiresAt) {
        return UpdateIncidentResponse.builder()
            .success(true)
            .status("STAGED")
            .message("Update staged for " + incidentNumber + ". Please confirm to proceed.")
            .incidentNumber(incidentNumber)
            .actionId(actionId)
            .preview(preview)
            .expiresAt(expiresAt)
            .build();
    }

    /**
     * Create a success response.
     */
    public static UpdateIncidentResponse updated(String incidentNumber) {
        return UpdateIncidentResponse.builder()
            .success(true)
            .status("UPDATED")
            .message("Incident " + incidentNumber + " updated successfully.")
            .incidentNumber(incidentNumber)
            .build();
    }

    /**
     * Create a failure response.
     */
    public static UpdateIncidentResponse failed(String incidentNumber, String message, String detail) {
        return UpdateIncidentResponse.builder()
            .success(false)
            .status("FAILED")
            .message(message)
            .incidentNumber(incidentNumber)
            .errorDetail(detail)
            .build();
    }

    /**
     * Create a not-found response.
     */
    public static UpdateIncidentResponse notFound(String incidentNumber) {
        return UpdateIncidentResponse.builder()
            .success(false)
            .status("NOT_FOUND")
            .message("Incident " + incidentNumber + " not found.")
            .incidentNumber(incidentNumber)
            .build();
    }
}
