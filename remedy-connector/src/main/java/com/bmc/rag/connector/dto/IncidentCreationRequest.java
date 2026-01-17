package com.bmc.rag.connector.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating new Incidents via BMC Remedy AR API.
 * Contains validated fields required for HPD:Help Desk form submission.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentCreationRequest {

    /**
     * Incident summary - required field.
     * Max 255 characters per Remedy field constraint.
     */
    @NotBlank(message = "Summary is required")
    @Size(max = 255, message = "Summary must not exceed 255 characters")
    private String summary;

    /**
     * Detailed incident description - required field.
     * Max 32000 characters per Remedy field constraint.
     */
    @NotBlank(message = "Description is required")
    @Size(max = 32000, message = "Description must not exceed 32000 characters")
    private String description;

    /**
     * Impact level - required field.
     * Values: 1=Extensive/Widespread, 2=Significant/Large, 3=Moderate/Limited, 4=Minor/Localized
     */
    @NotNull(message = "Impact is required")
    private Integer impact;

    /**
     * Urgency level - required field.
     * Values: 1=Critical, 2=High, 3=Medium, 4=Low
     */
    @NotNull(message = "Urgency is required")
    private Integer urgency;

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
     * Requester's company.
     */
    @Size(max = 120, message = "Company must not exceed 120 characters")
    private String requesterCompany;

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
     * Initial assignment group.
     */
    @Size(max = 120, message = "Assigned group must not exceed 120 characters")
    private String assignedGroup;

    /**
     * Service type.
     */
    @Size(max = 50, message = "Service type must not exceed 50 characters")
    private String serviceType;

    /**
     * User ID of the person creating the incident (for audit purposes).
     */
    private String createdBy;

    /**
     * Session ID for tracking (for confirmation workflow).
     */
    private String sessionId;

    /**
     * Validate impact value is within allowed range.
     */
    public boolean isValidImpact() {
        return impact != null && impact >= 1 && impact <= 4;
    }

    /**
     * Validate urgency value is within allowed range.
     */
    public boolean isValidUrgency() {
        return urgency != null && urgency >= 1 && urgency <= 4;
    }

    /**
     * Get human-readable impact label.
     */
    public String getImpactLabel() {
        if (impact == null) return "Unknown";
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
        if (urgency == null) return "Unknown";
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
        sb.append("**New Incident Preview**\n\n");
        sb.append("**Summary:** ").append(summary).append("\n\n");
        sb.append("**Description:** ").append(
            description.length() > 200 ? description.substring(0, 200) + "..." : description
        ).append("\n\n");
        sb.append("**Impact:** ").append(getImpactLabel()).append("\n");
        sb.append("**Urgency:** ").append(getUrgencyLabel()).append("\n");

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

        return sb.toString();
    }
}
