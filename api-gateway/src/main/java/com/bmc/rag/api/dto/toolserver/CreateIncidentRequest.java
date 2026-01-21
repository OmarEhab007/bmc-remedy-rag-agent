package com.bmc.rag.api.dto.toolserver;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating incidents via Tool Server.
 * Designed for consumption by Open WebUI tools.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateIncidentRequest {

    /**
     * Brief summary of the incident (max 255 characters).
     */
    @NotBlank(message = "Summary is required")
    @Size(max = 255, message = "Summary must not exceed 255 characters")
    private String summary;

    /**
     * Detailed description of the issue.
     */
    @NotBlank(message = "Description is required")
    @Size(max = 32000, message = "Description must not exceed 32000 characters")
    private String description;

    /**
     * Impact level.
     * 1=Extensive/Widespread, 2=Significant/Large, 3=Moderate/Limited, 4=Minor/Localized
     */
    @NotNull(message = "Impact is required")
    @Min(value = 1, message = "Impact must be between 1 and 4")
    @Max(value = 4, message = "Impact must be between 1 and 4")
    private Integer impact;

    /**
     * Urgency level.
     * 1=Critical, 2=High, 3=Medium, 4=Low
     */
    @NotNull(message = "Urgency is required")
    @Min(value = 1, message = "Urgency must be between 1 and 4")
    @Max(value = 4, message = "Urgency must be between 1 and 4")
    private Integer urgency;

    /**
     * Requester's first name (optional).
     */
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String requesterFirstName;

    /**
     * Requester's last name (optional).
     */
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String requesterLastName;

    /**
     * Requester's company (optional).
     */
    @Size(max = 120, message = "Company must not exceed 120 characters")
    private String requesterCompany;

    /**
     * Category tier 1 (optional).
     */
    @Size(max = 120, message = "Category must not exceed 120 characters")
    private String category;

    /**
     * Sub-category tier 2 (optional).
     */
    @Size(max = 120, message = "Sub-category must not exceed 120 characters")
    private String subCategory;

    /**
     * Sub-sub-category tier 3 (optional).
     */
    @Size(max = 120, message = "Item must not exceed 120 characters")
    private String item;

    /**
     * Initial assignment group (optional).
     */
    @Size(max = 120, message = "Assigned group must not exceed 120 characters")
    private String assignedGroup;

    /**
     * Service type (optional).
     */
    @Size(max = 50, message = "Service type must not exceed 50 characters")
    private String serviceType;

    /**
     * Configuration Item (CI) name from CMDB (optional).
     * Links the incident to a specific asset or service.
     */
    @Size(max = 255, message = "Configuration item must not exceed 255 characters")
    private String configurationItem;

    /**
     * Location where the issue is occurring (optional).
     */
    @Size(max = 120, message = "Location must not exceed 120 characters")
    private String location;

    /**
     * Whether to skip duplicate checking (default: false).
     */
    @Builder.Default
    private Boolean skipDuplicateCheck = false;

    /**
     * Whether to stage for confirmation or create directly (default: true = staged).
     * If true, creates a staged action requiring confirmation.
     * If false, creates the incident immediately (requires elevated permissions).
     */
    @Builder.Default
    private Boolean requireConfirmation = true;

    /**
     * Session ID for tracking the staged action.
     */
    private String sessionId;

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
}
