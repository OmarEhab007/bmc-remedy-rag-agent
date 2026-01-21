package com.bmc.rag.api.dto.toolserver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for incident creation via Tool Server.
 * Supports both staged (confirmation required) and direct creation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateIncidentResponse {

    /**
     * Whether the operation was successful.
     */
    private Boolean success;

    /**
     * Response status: STAGED, CREATED, FAILED, DUPLICATE_WARNING
     */
    private String status;

    /**
     * Human-readable message about the result.
     */
    private String message;

    /**
     * The created incident number (if status is CREATED).
     */
    private String incidentNumber;

    /**
     * The staged action ID (if status is STAGED).
     * Used to confirm or cancel the action.
     */
    private String actionId;

    /**
     * When the staged action expires (if status is STAGED).
     */
    private Instant expiresAt;

    /**
     * Preview of what will be created (if status is STAGED).
     */
    private String preview;

    /**
     * Similar incidents found during duplicate check.
     */
    private List<SearchResultItem> similarIncidents;

    /**
     * Whether potential duplicates were found.
     */
    @Builder.Default
    private Boolean hasDuplicates = false;

    /**
     * Error details if failed.
     */
    private String errorDetail;

    /**
     * Create a staged response (confirmation required).
     */
    public static CreateIncidentResponse staged(String actionId, String preview, Instant expiresAt) {
        return CreateIncidentResponse.builder()
            .success(true)
            .status("STAGED")
            .message("Incident creation staged. Please confirm to proceed.")
            .actionId(actionId)
            .preview(preview)
            .expiresAt(expiresAt)
            .build();
    }

    /**
     * Create a staged response with duplicate warning.
     */
    public static CreateIncidentResponse stagedWithDuplicates(
            String actionId,
            String preview,
            Instant expiresAt,
            List<SearchResultItem> similarIncidents) {
        return CreateIncidentResponse.builder()
            .success(true)
            .status("DUPLICATE_WARNING")
            .message("Potential duplicates found. Review similar incidents before confirming.")
            .actionId(actionId)
            .preview(preview)
            .expiresAt(expiresAt)
            .similarIncidents(similarIncidents)
            .hasDuplicates(true)
            .build();
    }

    /**
     * Create a success response (incident created).
     */
    public static CreateIncidentResponse created(String incidentNumber) {
        return CreateIncidentResponse.builder()
            .success(true)
            .status("CREATED")
            .message("Incident " + incidentNumber + " created successfully.")
            .incidentNumber(incidentNumber)
            .build();
    }

    /**
     * Create a failure response.
     */
    public static CreateIncidentResponse failed(String message, String detail) {
        return CreateIncidentResponse.builder()
            .success(false)
            .status("FAILED")
            .message(message)
            .errorDetail(detail)
            .build();
    }

    /**
     * Create a validation error response.
     */
    public static CreateIncidentResponse validationError(String message) {
        return CreateIncidentResponse.builder()
            .success(false)
            .status("VALIDATION_ERROR")
            .message(message)
            .build();
    }

    /**
     * Create a rate limit exceeded response.
     */
    public static CreateIncidentResponse rateLimited(int maxPerHour) {
        return CreateIncidentResponse.builder()
            .success(false)
            .status("RATE_LIMITED")
            .message("Rate limit exceeded. Maximum " + maxPerHour + " incident creations per hour.")
            .build();
    }
}
