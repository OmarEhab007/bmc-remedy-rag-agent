package com.bmc.rag.connector.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating existing Incidents via BMC Remedy AR API.
 * All fields are optional - only specified fields will be updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentUpdateRequest {

    /**
     * The incident number to update (e.g., INC000000001).
     */
    private String incidentNumber;

    /**
     * Updated summary (max 255 characters).
     */
    @Size(max = 255, message = "Summary must not exceed 255 characters")
    private String summary;

    /**
     * Updated description (max 32000 characters).
     */
    @Size(max = 32000, message = "Description must not exceed 32000 characters")
    private String description;

    /**
     * Updated impact level (1-4).
     */
    private Integer impact;

    /**
     * Updated urgency level (1-4).
     */
    private Integer urgency;

    /**
     * Updated status (0=New, 1=Assigned, 2=In Progress, 3=Pending, 4=Resolved, 5=Closed).
     */
    private Integer status;

    /**
     * Resolution notes (required when resolving).
     */
    @Size(max = 32000, message = "Resolution must not exceed 32000 characters")
    private String resolution;

    /**
     * Work log entry to add.
     */
    @Size(max = 32000, message = "Work log must not exceed 32000 characters")
    private String workLog;

    /**
     * Work log type (0=General Information, 1=Working Log, 2=Email System, etc.).
     */
    private Integer workLogType;

    /**
     * Updated assigned group.
     */
    @Size(max = 120, message = "Assigned group must not exceed 120 characters")
    private String assignedGroup;

    /**
     * Updated category tier 1.
     */
    @Size(max = 120, message = "Category must not exceed 120 characters")
    private String categoryTier1;

    /**
     * Updated category tier 2.
     */
    @Size(max = 120, message = "Category must not exceed 120 characters")
    private String categoryTier2;

    /**
     * Updated category tier 3.
     */
    @Size(max = 120, message = "Category must not exceed 120 characters")
    private String categoryTier3;

    /**
     * Resolution category tier 1 (for resolved incidents).
     */
    @Size(max = 120, message = "Resolution category must not exceed 120 characters")
    private String resolutionCategoryTier1;

    /**
     * Resolution category tier 2 (for resolved incidents).
     */
    @Size(max = 120, message = "Resolution category must not exceed 120 characters")
    private String resolutionCategoryTier2;

    /**
     * Resolution category tier 3 (for resolved incidents).
     */
    @Size(max = 120, message = "Resolution category must not exceed 120 characters")
    private String resolutionCategoryTier3;

    /**
     * User ID of the person performing the update (for audit).
     */
    private String updatedBy;

    /**
     * Session ID for tracking.
     */
    private String sessionId;

    /**
     * Validate impact value is within allowed range (if set).
     */
    public boolean isValidImpact() {
        return impact == null || (impact >= 1 && impact <= 4);
    }

    /**
     * Validate urgency value is within allowed range (if set).
     */
    public boolean isValidUrgency() {
        return urgency == null || (urgency >= 1 && urgency <= 4);
    }

    /**
     * Validate status value is within allowed range (if set).
     */
    public boolean isValidStatus() {
        return status == null || (status >= 0 && status <= 6);
    }

    /**
     * Check if any update fields are set.
     */
    public boolean hasUpdates() {
        return summary != null || description != null || impact != null ||
               urgency != null || status != null || resolution != null ||
               workLog != null || assignedGroup != null || categoryTier1 != null;
    }

    /**
     * Get human-readable status label.
     */
    public String getStatusLabel() {
        if (status == null) return null;
        return switch (status) {
            case 0 -> "New";
            case 1 -> "Assigned";
            case 2 -> "In Progress";
            case 3 -> "Pending";
            case 4 -> "Resolved";
            case 5 -> "Closed";
            case 6 -> "Cancelled";
            default -> "Unknown";
        };
    }

    /**
     * Get human-readable impact label.
     */
    public String getImpactLabel() {
        if (impact == null) return null;
        return switch (impact) {
            case 1 -> "Extensive/Widespread";
            case 2 -> "Significant/Large";
            case 3 -> "Moderate/Limited";
            case 4 -> "Minor/Localized";
            default -> "Unknown";
        };
    }

    /**
     * Get human-readable urgency label.
     */
    public String getUrgencyLabel() {
        if (urgency == null) return null;
        return switch (urgency) {
            case 1 -> "Critical";
            case 2 -> "High";
            case 3 -> "Medium";
            case 4 -> "Low";
            default -> "Unknown";
        };
    }

    /**
     * Create a preview string for confirmation display.
     */
    public String toPreviewString() {
        StringBuilder sb = new StringBuilder();
        sb.append("**Incident Update Preview**\n\n");
        sb.append("**Incident:** ").append(incidentNumber).append("\n\n");

        if (summary != null) {
            sb.append("**Summary:** ").append(summary).append("\n");
        }
        if (description != null) {
            sb.append("**Description:** ").append(
                description.length() > 200 ? description.substring(0, 200) + "..." : description
            ).append("\n");
        }
        if (impact != null) {
            sb.append("**Impact:** ").append(getImpactLabel()).append("\n");
        }
        if (urgency != null) {
            sb.append("**Urgency:** ").append(getUrgencyLabel()).append("\n");
        }
        if (status != null) {
            sb.append("**Status:** ").append(getStatusLabel()).append("\n");
        }
        if (resolution != null) {
            sb.append("**Resolution:** ").append(
                resolution.length() > 200 ? resolution.substring(0, 200) + "..." : resolution
            ).append("\n");
        }
        if (workLog != null) {
            sb.append("**Work Log:** ").append(
                workLog.length() > 200 ? workLog.substring(0, 200) + "..." : workLog
            ).append("\n");
        }
        if (assignedGroup != null) {
            sb.append("**Assigned Group:** ").append(assignedGroup).append("\n");
        }
        if (categoryTier1 != null) {
            sb.append("**Category:** ").append(categoryTier1);
            if (categoryTier2 != null) sb.append(" > ").append(categoryTier2);
            if (categoryTier3 != null) sb.append(" > ").append(categoryTier3);
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Get a concise summary of the update for display.
     */
    public String getUpdateSummary() {
        StringBuilder sb = new StringBuilder();
        int fieldCount = 0;

        if (summary != null) { sb.append("summary"); fieldCount++; }
        if (description != null) {
            if (fieldCount > 0) sb.append(", ");
            sb.append("description");
            fieldCount++;
        }
        if (status != null) {
            if (fieldCount > 0) sb.append(", ");
            sb.append("status → ").append(getStatusLabel());
            fieldCount++;
        }
        if (resolution != null) {
            if (fieldCount > 0) sb.append(", ");
            sb.append("resolution");
            fieldCount++;
        }
        if (workLog != null) {
            if (fieldCount > 0) sb.append(", ");
            sb.append("work log");
            fieldCount++;
        }
        if (assignedGroup != null) {
            if (fieldCount > 0) sb.append(", ");
            sb.append("assignment");
            fieldCount++;
        }

        return sb.length() > 0 ? "Updating: " + sb : "No changes";
    }
}
