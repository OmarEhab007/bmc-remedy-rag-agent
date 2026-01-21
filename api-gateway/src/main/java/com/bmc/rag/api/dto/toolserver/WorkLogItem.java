package com.bmc.rag.api.dto.toolserver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Work log entry item for API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkLogItem {

    /**
     * Work log entry ID.
     */
    private String workLogId;

    /**
     * Type of work log (e.g., Working Log, Customer Communication).
     */
    private String type;

    /**
     * Detailed description/notes.
     */
    private String description;

    /**
     * Person who submitted the work log.
     */
    private String submitter;

    /**
     * When the work log was submitted (ISO 8601 format).
     */
    private String submitDate;

    /**
     * View access level (Internal or Public).
     */
    private String viewAccess;

    /**
     * Get a preview of the description (first 200 chars).
     */
    public String getPreview() {
        if (description == null) return "";
        return description.length() > 200
            ? description.substring(0, 200) + "..."
            : description;
    }
}
