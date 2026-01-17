package com.bmc.rag.connector.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Change Request from CHG:Infrastructure Change form.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRequestRecord implements ITSMRecord {

    private String entryId;
    private String changeId;
    private String summary;
    private String description;
    private String changeReason;
    private String implementationPlan;
    private String rollbackPlan;
    private Integer status;
    private String statusDisplayValue;
    private Integer riskLevel;
    private Integer impact;
    private Integer urgency;
    private String changeType;
    private String changeClass;
    private String assignedGroup;
    private String assignedTo;
    private String assignedSupportCompany;
    private String submitter;
    private Instant createDate;
    private Instant lastModifiedDate;
    private String lastModifiedBy;

    // Categorization
    private String categoryTier1;
    private String categoryTier2;
    private String categoryTier3;

    // Scheduling
    private Instant scheduledStartDate;
    private Instant scheduledEndDate;
    private Instant actualStartDate;
    private Instant actualEndDate;

    // Related records
    @Builder.Default
    private List<WorkLogEntry> workLogs = new ArrayList<>();

    @Builder.Default
    private List<AttachmentInfo> attachments = new ArrayList<>();

    @Override
    public String getRecordType() {
        return "ChangeRequest";
    }

    @Override
    public String getRecordId() {
        return changeId;
    }

    @Override
    public String getTitle() {
        return summary;
    }

    @Override
    public String getContent() {
        return description;
    }

    /**
     * Get full categorization as a path string.
     */
    public String getCategoryPath() {
        StringBuilder sb = new StringBuilder();
        if (categoryTier1 != null) {
            sb.append(categoryTier1);
            if (categoryTier2 != null) {
                sb.append(" > ").append(categoryTier2);
                if (categoryTier3 != null) {
                    sb.append(" > ").append(categoryTier3);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Check if the change request has an implementation plan.
     */
    public boolean hasImplementationPlan() {
        return implementationPlan != null && !implementationPlan.trim().isEmpty();
    }

    /**
     * Check if the change request has a rollback plan.
     */
    public boolean hasRollbackPlan() {
        return rollbackPlan != null && !rollbackPlan.trim().isEmpty();
    }

    /**
     * Check if the change is closed/completed.
     */
    public boolean isClosed() {
        return status != null && (status >= 10); // Completed, Closed, Cancelled
    }

    /**
     * Check if the change is high risk.
     */
    public boolean isHighRisk() {
        return riskLevel != null && riskLevel >= 3; // Typically 3+ is high risk
    }

    /**
     * Get the combined content for vectorization.
     * Includes summary, description, implementation plan, and rollback plan.
     */
    public String getCombinedContent() {
        StringBuilder sb = new StringBuilder();

        if (summary != null) {
            sb.append("Summary: ").append(summary).append("\n\n");
        }

        if (description != null) {
            sb.append("Description: ").append(description).append("\n\n");
        }

        if (changeReason != null) {
            sb.append("Reason for Change: ").append(changeReason).append("\n\n");
        }

        if (implementationPlan != null) {
            sb.append("Implementation Plan: ").append(implementationPlan).append("\n\n");
        }

        if (rollbackPlan != null) {
            sb.append("Rollback Plan: ").append(rollbackPlan);
        }

        return sb.toString().trim();
    }
}
