package com.bmc.rag.connector.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Request DTO for creating new Work Orders via BMC Remedy AR API.
 * Contains validated fields required for WOI:WorkOrder form submission.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderCreationRequest {

    /**
     * Work order summary - required field.
     * Max 255 characters per Remedy field constraint.
     */
    @NotBlank(message = "Summary is required")
    @Size(max = 255, message = "Summary must not exceed 255 characters")
    private String summary;

    /**
     * Detailed work order description - required field.
     * Max 32000 characters per Remedy field constraint.
     */
    @NotBlank(message = "Description is required")
    @Size(max = 32000, message = "Description must not exceed 32000 characters")
    private String description;

    /**
     * Work order type - required field.
     * Values: 0=General, 1=Project Work, 2=Break/Fix, 3=Move/Add/Change, 4=Release Activity
     */
    @NotNull(message = "Work order type is required")
    private Integer workOrderType;

    /**
     * Priority level - required field.
     * Values: 0=Critical, 1=High, 2=Medium, 3=Low
     */
    @NotNull(message = "Priority is required")
    private Integer priority;

    // Optional fields

    /**
     * Requester's first name.
     */
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String requesterFirstName;

    /**
     * Requester's last name.
     */
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String requesterLastName;

    /**
     * Location company.
     */
    @Size(max = 120, message = "Company must not exceed 120 characters")
    private String locationCompany;

    /**
     * Category tier 1.
     */
    @Size(max = 120, message = "Category tier 1 must not exceed 120 characters")
    private String categoryTier1;

    /**
     * Category tier 2.
     */
    @Size(max = 120, message = "Category tier 2 must not exceed 120 characters")
    private String categoryTier2;

    /**
     * Category tier 3.
     */
    @Size(max = 120, message = "Category tier 3 must not exceed 120 characters")
    private String categoryTier3;

    /**
     * Assignment group.
     */
    @Size(max = 120, message = "Assigned group must not exceed 120 characters")
    private String assignedGroup;

    /**
     * Scheduled start date.
     */
    private Instant scheduledStartDate;

    /**
     * Scheduled end date.
     */
    private Instant scheduledEndDate;

    /**
     * User ID of the person creating the work order (for audit purposes).
     */
    private String createdBy;

    /**
     * Session ID for tracking (for confirmation workflow).
     */
    private String sessionId;

    /**
     * Validate work order type value is within allowed range.
     */
    public boolean isValidWorkOrderType() {
        return workOrderType != null && workOrderType >= 0 && workOrderType <= 4;
    }

    /**
     * Validate priority value is within allowed range.
     */
    public boolean isValidPriority() {
        return priority != null && priority >= 0 && priority <= 3;
    }

    /**
     * Get human-readable work order type label.
     */
    public String getWorkOrderTypeLabel() {
        if (workOrderType == null) return "Unknown";
        return switch (workOrderType) {
            case 0 -> "General";
            case 1 -> "Project Work";
            case 2 -> "Break/Fix";
            case 3 -> "Move/Add/Change";
            case 4 -> "Release Activity";
            default -> "Unknown";
        };
    }

    /**
     * Get human-readable priority label.
     */
    public String getPriorityLabel() {
        if (priority == null) return "Unknown";
        return switch (priority) {
            case 0 -> "Critical";
            case 1 -> "High";
            case 2 -> "Medium";
            case 3 -> "Low";
            default -> "Unknown";
        };
    }

    /**
     * Create a preview string for confirmation display.
     */
    public String toPreviewString() {
        StringBuilder sb = new StringBuilder();
        sb.append("**New Work Order Preview**\n\n");
        sb.append("**Summary:** ").append(summary).append("\n\n");
        sb.append("**Description:** ").append(
            description.length() > 200 ? description.substring(0, 200) + "..." : description
        ).append("\n\n");
        sb.append("**Type:** ").append(getWorkOrderTypeLabel()).append("\n");
        sb.append("**Priority:** ").append(getPriorityLabel()).append("\n");

        if (requesterFirstName != null || requesterLastName != null) {
            sb.append("**Requester:** ");
            if (requesterFirstName != null) sb.append(requesterFirstName);
            if (requesterFirstName != null && requesterLastName != null) sb.append(" ");
            if (requesterLastName != null) sb.append(requesterLastName);
            sb.append("\n");
        }

        if (categoryTier1 != null) {
            sb.append("**Category:** ").append(categoryTier1);
            if (categoryTier2 != null) sb.append(" > ").append(categoryTier2);
            if (categoryTier3 != null) sb.append(" > ").append(categoryTier3);
            sb.append("\n");
        }

        if (assignedGroup != null) {
            sb.append("**Assigned Group:** ").append(assignedGroup).append("\n");
        }

        if (scheduledStartDate != null) {
            sb.append("**Scheduled Start:** ").append(scheduledStartDate).append("\n");
        }

        if (scheduledEndDate != null) {
            sb.append("**Scheduled End:** ").append(scheduledEndDate).append("\n");
        }

        return sb.toString();
    }
}
