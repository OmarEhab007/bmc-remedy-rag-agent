package com.bmc.rag.connector.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an Incident record from HPD:Help Desk form.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentRecord implements ITSMRecord {

    private String entryId;
    private String incidentNumber;
    private String summary;
    private String description;
    private String resolution;
    private Integer status;
    private String statusDisplayValue;
    private Integer urgency;
    private Integer impact;
    private Integer priority;
    private String assignedGroup;
    private String assignedTo;
    private String assignedSupportCompany;
    private String assignedSupportOrg;
    private String submitter;
    private Instant createDate;
    private Instant lastModifiedDate;
    private String lastModifiedBy;

    // Categorization
    private String categoryTier1;
    private String categoryTier2;
    private String categoryTier3;
    private String productTier1;
    private String productTier2;
    private String productTier3;
    private String resolutionCategoryTier1;
    private String resolutionCategoryTier2;
    private String resolutionCategoryTier3;

    // Customer info
    private String customerFirstName;
    private String customerLastName;
    private String customerCompany;

    // Reported source
    private String reportedSource;
    private String serviceType;

    // Related records
    @Builder.Default
    private List<WorkLogEntry> workLogs = new ArrayList<>();

    @Builder.Default
    private List<AttachmentInfo> attachments = new ArrayList<>();

    @Override
    public String getRecordType() {
        return "Incident";
    }

    @Override
    public String getRecordId() {
        return incidentNumber;
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
     * Get customer full name.
     */
    public String getCustomerFullName() {
        if (customerFirstName != null && customerLastName != null) {
            return customerFirstName + " " + customerLastName;
        } else if (customerFirstName != null) {
            return customerFirstName;
        } else if (customerLastName != null) {
            return customerLastName;
        }
        return null;
    }

    /**
     * Check if the incident has a resolution.
     */
    public boolean hasResolution() {
        return resolution != null && !resolution.trim().isEmpty();
    }

    /**
     * Check if the incident is closed.
     */
    public boolean isClosed() {
        return status != null && status >= 4; // Resolved, Closed, Cancelled
    }
}
