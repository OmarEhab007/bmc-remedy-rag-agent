package com.bmc.rag.api.dto.toolserver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for incident work log queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkLogResponse {

    /**
     * The incident number the work logs belong to.
     */
    private String incidentNumber;

    /**
     * Total count of work logs.
     */
    private int totalCount;

    /**
     * List of work log entries.
     */
    private List<WorkLogItem> workLogs;

    /**
     * Error message if the request failed.
     */
    private String errorMessage;

    /**
     * Whether the request was successful.
     */
    public boolean isSuccess() {
        return errorMessage == null;
    }
}
