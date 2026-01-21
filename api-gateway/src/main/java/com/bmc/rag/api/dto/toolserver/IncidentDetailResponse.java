package com.bmc.rag.api.dto.toolserver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Full incident details response for Tool Server.
 * Contains all relevant incident information for AI tool consumption.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentDetailResponse {

    /**
     * Incident number (e.g., INC000000001).
     */
    private String incidentNumber;

    /**
     * Brief summary of the incident.
     */
    private String summary;

    /**
     * Full description of the issue.
     */
    private String description;

    /**
     * Resolution notes (if resolved).
     */
    private String resolution;

    /**
     * Current status.
     */
    private String status;

    /**
     * Status code (numeric).
     */
    private Integer statusCode;

    /**
     * Impact level (1-4).
     */
    private Integer impact;

    /**
     * Impact label (e.g., "Moderate/Limited").
     */
    private String impactLabel;

    /**
     * Urgency level (1-4).
     */
    private Integer urgency;

    /**
     * Urgency label (e.g., "Medium").
     */
    private String urgencyLabel;

    /**
     * Priority calculated from impact and urgency.
     */
    private Integer priority;

    /**
     * Priority label (e.g., "Medium").
     */
    private String priorityLabel;

    /**
     * Assigned support group.
     */
    private String assignedGroup;

    /**
     * Assigned individual.
     */
    private String assignedTo;

    /**
     * Person who submitted the incident.
     */
    private String submitter;

    /**
     * Customer first name.
     */
    private String customerFirstName;

    /**
     * Customer last name.
     */
    private String customerLastName;

    /**
     * Customer company.
     */
    private String customerCompany;

    /**
     * Category path (e.g., "Hardware > Laptop > Screen").
     */
    private String categoryPath;

    /**
     * Resolution category path.
     */
    private String resolutionCategoryPath;

    /**
     * When the incident was created.
     */
    private Instant createDate;

    /**
     * Last modification timestamp.
     */
    private Instant lastModifiedDate;

    /**
     * Who last modified the incident.
     */
    private String lastModifiedBy;

    /**
     * Work log entries.
     */
    @Builder.Default
    private List<WorkLogItem> workLogs = new ArrayList<>();

    /**
     * Attachment information.
     */
    @Builder.Default
    private List<AttachmentItem> attachments = new ArrayList<>();

    /**
     * Whether the record was found.
     */
    @Builder.Default
    private Boolean found = true;

    /**
     * Error message if not found.
     */
    private String errorMessage;

    /**
     * Work log entry.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkLogItem {
        private String id;
        private String type;
        private String summary;
        private String notes;
        private String submitter;
        private Instant submitDate;
    }

    /**
     * Attachment information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentItem {
        private String name;
        private Long sizeBytes;
        private String contentType;
    }

    /**
     * Create a not-found response.
     */
    public static IncidentDetailResponse notFound(String incidentNumber) {
        return IncidentDetailResponse.builder()
            .incidentNumber(incidentNumber)
            .found(false)
            .errorMessage("Incident " + incidentNumber + " not found")
            .build();
    }

    /**
     * Get human-readable impact label.
     */
    public String getImpactLabel() {
        if (impactLabel != null) return impactLabel;
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
        if (urgencyLabel != null) return urgencyLabel;
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
     * Get human-readable priority label.
     */
    public String getPriorityLabel() {
        if (priorityLabel != null) return priorityLabel;
        if (priority == null) return "Unknown";
        return switch (priority) {
            case 1 -> "Critical";
            case 2 -> "High";
            case 3 -> "Medium";
            case 4 -> "Low";
            default -> "Unknown";
        };
    }
}
