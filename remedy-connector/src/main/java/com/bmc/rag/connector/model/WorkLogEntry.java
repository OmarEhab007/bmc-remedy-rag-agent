package com.bmc.rag.connector.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Work Log entry from HPD:WorkLog, WOI:WorkInfo, or CHG:WorkLog forms.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkLogEntry {

    public enum WorkLogSource {
        INCIDENT("HPD:WorkLog"),
        WORK_ORDER("WOI:WorkInfo"),
        CHANGE_REQUEST("CHG:WorkLog");

        private final String formName;

        WorkLogSource(String formName) {
            this.formName = formName;
        }

        public String getFormName() {
            return formName;
        }
    }

    private String entryId;
    private String workLogId;
    private String parentId;  // Incident Number, Work Order ID, or Change ID
    private WorkLogSource source;
    private Integer workLogType;
    private String workLogTypeDisplayValue;
    private String detailedDescription;
    private String submitter;
    private Instant submitDate;
    private Integer viewAccess;
    private Instant createDate;
    private Instant lastModifiedDate;

    // Related attachments
    @Builder.Default
    private List<AttachmentInfo> attachments = new ArrayList<>();

    /**
     * Get a formatted string representation of the work log.
     */
    public String getFormattedContent() {
        StringBuilder sb = new StringBuilder();

        if (submitDate != null) {
            sb.append("[").append(submitDate).append("] ");
        }

        if (submitter != null) {
            sb.append("By: ").append(submitter).append("\n");
        }

        if (workLogTypeDisplayValue != null) {
            sb.append("Type: ").append(workLogTypeDisplayValue).append("\n");
        }

        if (detailedDescription != null) {
            sb.append(detailedDescription);
        }

        return sb.toString();
    }

    /**
     * Check if this work log is public (visible to customers).
     */
    public boolean isPublic() {
        // Typically, viewAccess = 0 means Public, 1 means Internal
        return viewAccess == null || viewAccess == 0;
    }

    /**
     * Check if this work log has meaningful content.
     */
    public boolean hasContent() {
        return detailedDescription != null && !detailedDescription.trim().isEmpty();
    }

    /**
     * Get content length for chunking decisions.
     */
    public int getContentLength() {
        return detailedDescription != null ? detailedDescription.length() : 0;
    }
}
