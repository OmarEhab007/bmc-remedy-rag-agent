package com.bmc.rag.connector.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Work Order record from WOI:WorkOrder form.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderRecord implements ITSMRecord {

    private String entryId;
    private String workOrderId;
    private String summary;
    private String description;
    private Integer status;
    private String statusDisplayValue;
    private Integer priority;
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

    // Requester info
    private String requesterFirstName;
    private String requesterLastName;
    private String locationCompany;

    // Scheduling
    private Instant scheduledStartDate;
    private Instant scheduledEndDate;

    // Related records
    @Builder.Default
    private List<WorkLogEntry> workLogs = new ArrayList<>();

    @Builder.Default
    private List<AttachmentInfo> attachments = new ArrayList<>();

    @Override
    public String getRecordType() {
        return "WorkOrder";
    }

    @Override
    public String getRecordId() {
        return workOrderId;
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
     * Get requester full name.
     */
    public String getRequesterFullName() {
        if (requesterFirstName != null && requesterLastName != null) {
            return requesterFirstName + " " + requesterLastName;
        } else if (requesterFirstName != null) {
            return requesterFirstName;
        } else if (requesterLastName != null) {
            return requesterLastName;
        }
        return null;
    }

    /**
     * Check if the work order is closed.
     */
    public boolean isClosed() {
        return status != null && (status == 5 || status == 7 || status == 8); // Completed, Cancelled, Closed
    }
}
