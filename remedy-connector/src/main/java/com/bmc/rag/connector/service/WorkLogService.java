package com.bmc.rag.connector.service;

import com.bmc.arsys.api.*;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.connector.util.FieldIdConstants;
import com.bmc.rag.connector.util.FieldIdConstants.IncidentWorkLog;
import com.bmc.rag.connector.util.QualifierBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service for retrieving work log entries from BMC Remedy.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkLogService {

    private final ThreadLocalARContext arContext;

    private static final String WORKLOG_FORM = FieldIdConstants.IncidentWorkLog.FORM_NAME;
    private static final int MAX_WORKLOGS = 100;

    /**
     * Get work logs for an incident.
     *
     * @param incidentNumber The incident number (e.g., INC000000001)
     * @return List of work log entries, sorted by date descending
     */
    public List<WorkLogEntry> getWorkLogsForIncident(String incidentNumber) {
        if (incidentNumber == null || incidentNumber.isBlank()) {
            return Collections.emptyList();
        }

        log.info("Retrieving work logs for incident: {}", incidentNumber);

        try {
            return arContext.executeWithRetry(ctx -> {
                String qualification = String.format("'%d' = \"%s\"",
                    IncidentWorkLog.INCIDENT_NUMBER, escapeValue(incidentNumber));

                QualifierInfo qualifier = QualifierBuilder.parseQualification(ctx, WORKLOG_FORM, qualification);

                // Sort by submit date descending
                List<SortInfo> sortList = List.of(new SortInfo(IncidentWorkLog.SUBMIT_DATE, 2)); // 2 = descending

                int[] fieldIds = {
                    IncidentWorkLog.WORK_LOG_ID,
                    IncidentWorkLog.INCIDENT_NUMBER,
                    IncidentWorkLog.WORK_LOG_TYPE,
                    IncidentWorkLog.DETAILED_DESCRIPTION,
                    IncidentWorkLog.SUBMITTER,
                    IncidentWorkLog.SUBMIT_DATE,
                    IncidentWorkLog.VIEW_ACCESS
                };

                List<Entry> entries = ctx.getListEntryObjects(
                    WORKLOG_FORM,
                    qualifier,
                    0,
                    MAX_WORKLOGS,
                    sortList,
                    fieldIds,
                    false,
                    null
                );

                if (entries == null || entries.isEmpty()) {
                    log.debug("No work logs found for incident {}", incidentNumber);
                    return Collections.<WorkLogEntry>emptyList();
                }

                List<WorkLogEntry> workLogs = new ArrayList<>();
                for (Entry entry : entries) {
                    WorkLogEntry workLog = parseWorkLogEntry(entry, incidentNumber);
                    if (workLog != null) {
                        workLogs.add(workLog);
                    }
                }

                log.info("Found {} work logs for incident {}", workLogs.size(), incidentNumber);
                return workLogs;
            });
        } catch (Exception e) {
            log.error("Failed to retrieve work logs for {}: {}", incidentNumber, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Parse an entry into a WorkLogEntry record.
     */
    private WorkLogEntry parseWorkLogEntry(Entry entry, String incidentNumber) {
        try {
            String workLogId = getStringValue(entry, IncidentWorkLog.WORK_LOG_ID);
            int workLogType = getIntValue(entry, IncidentWorkLog.WORK_LOG_TYPE);
            String description = getStringValue(entry, IncidentWorkLog.DETAILED_DESCRIPTION);
            String submitter = getStringValue(entry, IncidentWorkLog.SUBMITTER);
            long submitDateEpoch = getLongValue(entry, IncidentWorkLog.SUBMIT_DATE);
            int viewAccess = getIntValue(entry, IncidentWorkLog.VIEW_ACCESS);

            Instant submitDate = submitDateEpoch > 0 ? Instant.ofEpochSecond(submitDateEpoch) : null;

            return new WorkLogEntry(
                workLogId,
                incidentNumber,
                workLogType,
                getWorkLogTypeName(workLogType),
                description,
                submitter,
                submitDate,
                viewAccess == 0 ? "Internal" : "Public"
            );
        } catch (Exception e) {
            log.warn("Failed to parse work log entry: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get string value from entry.
     */
    private String getStringValue(Entry entry, int fieldId) {
        Value value = entry.get(fieldId);
        return value != null && value.getValue() != null ? value.toString() : null;
    }

    /**
     * Get integer value from entry.
     */
    private int getIntValue(Entry entry, int fieldId) {
        Value value = entry.get(fieldId);
        if (value != null && value.getValue() != null) {
            Object val = value.getValue();
            if (val instanceof Number) {
                return ((Number) val).intValue();
            }
        }
        return 0;
    }

    /**
     * Get long value from entry.
     */
    private long getLongValue(Entry entry, int fieldId) {
        Value value = entry.get(fieldId);
        if (value != null && value.getValue() != null) {
            Object val = value.getValue();
            if (val instanceof Number) {
                return ((Number) val).longValue();
            }
        }
        return 0L;
    }

    /**
     * Escape special characters in values for Remedy qualifications.
     */
    private String escapeValue(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Get human-readable work log type name.
     */
    private String getWorkLogTypeName(int typeId) {
        return switch (typeId) {
            case 0 -> "General Information";
            case 1 -> "Working Log";
            case 2 -> "Email System";
            case 3 -> "Customer Communication";
            case 4 -> "Customer Inbound";
            case 5 -> "Customer Outbound";
            case 6 -> "Resolution Communications";
            case 7 -> "Paging System";
            case 8 -> "System Assignment";
            case 9 -> "Status Update";
            case 10 -> "Pending Change";
            case 11 -> "Closure";
            default -> "Other";
        };
    }

    /**
     * Work log entry record.
     */
    public record WorkLogEntry(
        String workLogId,
        String incidentNumber,
        int workLogType,
        String workLogTypeName,
        String description,
        String submitter,
        Instant submitDate,
        String viewAccess
    ) {
        public String getFormattedDate() {
            if (submitDate == null) return "";
            return submitDate.toString();
        }

        public String getPreview(int maxLength) {
            if (description == null) return "";
            return description.length() > maxLength
                ? description.substring(0, maxLength) + "..."
                : description;
        }
    }
}
