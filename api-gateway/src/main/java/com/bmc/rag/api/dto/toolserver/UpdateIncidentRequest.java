package com.bmc.rag.api.dto.toolserver;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating incidents via Tool Server.
 * All fields are optional - only specified fields are updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateIncidentRequest {

    /**
     * New status for the incident.
     * Allowed values: New, Assigned, In Progress, Pending, Resolved, Closed, Cancelled
     */
    @Size(max = 30, message = "Status must not exceed 30 characters")
    private String status;

    /**
     * Status reason (required for some status transitions).
     */
    @Size(max = 255, message = "Status reason must not exceed 255 characters")
    private String statusReason;

    /**
     * Resolution notes (required when resolving).
     */
    @Size(max = 32000, message = "Resolution must not exceed 32000 characters")
    private String resolution;

    /**
     * Resolution category tier 1.
     */
    @Size(max = 120, message = "Resolution category must not exceed 120 characters")
    private String resolutionCategory;

    /**
     * Resolution category tier 2.
     */
    @Size(max = 120, message = "Resolution sub-category must not exceed 120 characters")
    private String resolutionSubCategory;

    /**
     * Resolution category tier 3.
     */
    @Size(max = 120, message = "Resolution item must not exceed 120 characters")
    private String resolutionItem;

    /**
     * Work log notes to add.
     */
    @Size(max = 32000, message = "Work log notes must not exceed 32000 characters")
    private String workLogNotes;

    /**
     * Work log type.
     * Common values: Customer Communication, Customer Inbound, Customer Outbound,
     * General Information, Resolution Communications, Working Log
     */
    @Size(max = 50, message = "Work log type must not exceed 50 characters")
    private String workLogType;

    /**
     * New impact level (1-4).
     */
    @Min(value = 1, message = "Impact must be between 1 and 4")
    @Max(value = 4, message = "Impact must be between 1 and 4")
    private Integer impact;

    /**
     * New urgency level (1-4).
     */
    @Min(value = 1, message = "Urgency must be between 1 and 4")
    @Max(value = 4, message = "Urgency must be between 1 and 4")
    private Integer urgency;

    /**
     * New assigned group.
     */
    @Size(max = 120, message = "Assigned group must not exceed 120 characters")
    private String assignedGroup;

    /**
     * New assigned individual.
     */
    @Size(max = 120, message = "Assigned to must not exceed 120 characters")
    private String assignedTo;

    /**
     * Whether to stage for confirmation or update directly.
     */
    @Builder.Default
    private Boolean requireConfirmation = true;

    /**
     * Session ID for tracking the staged action.
     */
    private String sessionId;

    /**
     * Check if any update fields are specified.
     */
    public boolean hasUpdates() {
        return status != null ||
               resolution != null ||
               workLogNotes != null ||
               impact != null ||
               urgency != null ||
               assignedGroup != null ||
               assignedTo != null;
    }

    /**
     * Get a summary of what will be updated.
     */
    public String getUpdateSummary() {
        StringBuilder sb = new StringBuilder();
        if (status != null) sb.append("Status → ").append(status).append("; ");
        if (resolution != null) sb.append("Resolution added; ");
        if (workLogNotes != null) sb.append("Work log added; ");
        if (impact != null) sb.append("Impact → ").append(impact).append("; ");
        if (urgency != null) sb.append("Urgency → ").append(urgency).append("; ");
        if (assignedGroup != null) sb.append("Assigned to ").append(assignedGroup).append("; ");
        if (assignedTo != null) sb.append("Assigned to ").append(assignedTo).append("; ");
        return sb.length() > 0 ? sb.substring(0, sb.length() - 2) : "No updates specified";
    }
}
