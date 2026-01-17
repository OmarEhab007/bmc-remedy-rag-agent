package com.bmc.rag.connector.extractor;

import com.bmc.arsys.api.*;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.connector.config.RemedyConnectionConfig;
import com.bmc.rag.connector.model.WorkLogEntry;
import com.bmc.rag.connector.util.FieldIdConstants;
import com.bmc.rag.connector.util.QualifierBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Extracts Work Log entries from HPD:WorkLog, WOI:WorkInfo, and CHG:WorkLog forms.
 * Work logs are linked to parent records (Incidents, Work Orders, Change Requests).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkLogExtractor {

    private final ThreadLocalARContext arContext;
    private final RemedyConnectionConfig config;

    /**
     * Extract work logs for an incident.
     *
     * @param incidentNumber The incident number
     * @return List of work log entries
     */
    public List<WorkLogEntry> extractIncidentWorkLogs(String incidentNumber) {
        return extractWorkLogs(
            FieldIdConstants.IncidentWorkLog.FORM_NAME,
            FieldIdConstants.IncidentWorkLog.INCIDENT_NUMBER,
            incidentNumber,
            WorkLogEntry.WorkLogSource.INCIDENT
        );
    }

    /**
     * Extract work logs for a work order.
     *
     * @param workOrderId The work order ID
     * @return List of work log entries
     */
    public List<WorkLogEntry> extractWorkOrderWorkLogs(String workOrderId) {
        return extractWorkLogs(
            FieldIdConstants.WorkOrderInfo.FORM_NAME,
            FieldIdConstants.WorkOrderInfo.WORK_ORDER_ID,
            workOrderId,
            WorkLogEntry.WorkLogSource.WORK_ORDER
        );
    }

    /**
     * Extract work logs for a change request.
     *
     * @param changeId The change request ID
     * @return List of work log entries
     */
    public List<WorkLogEntry> extractChangeWorkLogs(String changeId) {
        return extractWorkLogs(
            FieldIdConstants.ChangeWorkLog.FORM_NAME,
            FieldIdConstants.ChangeWorkLog.CHANGE_ID,
            changeId,
            WorkLogEntry.WorkLogSource.CHANGE_REQUEST
        );
    }

    /**
     * Batch extract work logs for multiple incidents.
     *
     * @param incidentNumbers List of incident numbers
     * @return Map of incident number to work logs
     */
    public Map<String, List<WorkLogEntry>> batchExtractIncidentWorkLogs(List<String> incidentNumbers) {
        if (incidentNumbers == null || incidentNumbers.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<WorkLogEntry>> result = new HashMap<>();
        for (String incidentNumber : incidentNumbers) {
            result.put(incidentNumber, new ArrayList<>());
        }

        // Build OR qualification for batch retrieval
        StringBuilder orCondition = new StringBuilder();
        for (int i = 0; i < incidentNumbers.size(); i++) {
            if (i > 0) {
                orCondition.append(" OR ");
            }
            orCondition.append(String.format("'%d' = \"%s\"",
                FieldIdConstants.IncidentWorkLog.INCIDENT_NUMBER,
                incidentNumbers.get(i)));
        }

        List<WorkLogEntry> allWorkLogs = extractWorkLogsWithQualification(
            FieldIdConstants.IncidentWorkLog.FORM_NAME,
            orCondition.toString(),
            WorkLogEntry.WorkLogSource.INCIDENT,
            FieldIdConstants.IncidentWorkLog.INCIDENT_NUMBER
        );

        // Group by incident number
        for (WorkLogEntry workLog : allWorkLogs) {
            List<WorkLogEntry> logs = result.get(workLog.getParentId());
            if (logs != null) {
                logs.add(workLog);
            }
        }

        return result;
    }

    /**
     * Batch extract work logs for multiple work orders.
     *
     * @param workOrderIds List of work order IDs
     * @return Map of work order ID to work logs
     */
    public Map<String, List<WorkLogEntry>> batchExtractWorkOrderWorkLogs(List<String> workOrderIds) {
        if (workOrderIds == null || workOrderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<WorkLogEntry>> result = new HashMap<>();
        for (String workOrderId : workOrderIds) {
            result.put(workOrderId, new ArrayList<>());
        }

        StringBuilder orCondition = new StringBuilder();
        for (int i = 0; i < workOrderIds.size(); i++) {
            if (i > 0) {
                orCondition.append(" OR ");
            }
            orCondition.append(String.format("'%d' = \"%s\"",
                FieldIdConstants.WorkOrderInfo.WORK_ORDER_ID,
                workOrderIds.get(i)));
        }

        List<WorkLogEntry> allWorkLogs = extractWorkLogsWithQualification(
            FieldIdConstants.WorkOrderInfo.FORM_NAME,
            orCondition.toString(),
            WorkLogEntry.WorkLogSource.WORK_ORDER,
            FieldIdConstants.WorkOrderInfo.WORK_ORDER_ID
        );

        for (WorkLogEntry workLog : allWorkLogs) {
            List<WorkLogEntry> logs = result.get(workLog.getParentId());
            if (logs != null) {
                logs.add(workLog);
            }
        }

        return result;
    }

    /**
     * Batch extract work logs for multiple change requests.
     *
     * @param changeIds List of change request IDs
     * @return Map of change ID to work logs
     */
    public Map<String, List<WorkLogEntry>> batchExtractChangeWorkLogs(List<String> changeIds) {
        if (changeIds == null || changeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<WorkLogEntry>> result = new HashMap<>();
        for (String changeId : changeIds) {
            result.put(changeId, new ArrayList<>());
        }

        StringBuilder orCondition = new StringBuilder();
        for (int i = 0; i < changeIds.size(); i++) {
            if (i > 0) {
                orCondition.append(" OR ");
            }
            orCondition.append(String.format("'%d' = \"%s\"",
                FieldIdConstants.ChangeWorkLog.CHANGE_ID,
                changeIds.get(i)));
        }

        List<WorkLogEntry> allWorkLogs = extractWorkLogsWithQualification(
            FieldIdConstants.ChangeWorkLog.FORM_NAME,
            orCondition.toString(),
            WorkLogEntry.WorkLogSource.CHANGE_REQUEST,
            FieldIdConstants.ChangeWorkLog.CHANGE_ID
        );

        for (WorkLogEntry workLog : allWorkLogs) {
            List<WorkLogEntry> logs = result.get(workLog.getParentId());
            if (logs != null) {
                logs.add(workLog);
            }
        }

        return result;
    }

    /**
     * Internal method to extract work logs from any work log form.
     */
    private List<WorkLogEntry> extractWorkLogs(
            String formName,
            int parentFieldId,
            String parentId,
            WorkLogEntry.WorkLogSource source) {

        String qualification = QualifierBuilder.byParentId(parentFieldId, parentId);
        return extractWorkLogsWithQualification(formName, qualification, source, parentFieldId);
    }

    /**
     * Extract work logs with a custom qualification.
     */
    private List<WorkLogEntry> extractWorkLogsWithQualification(
            String formName,
            String qualification,
            WorkLogEntry.WorkLogSource source,
            int parentFieldId) {

        return arContext.executeWithRetry(ctx -> {
            List<WorkLogEntry> workLogs = new ArrayList<>();
            int chunkSize = config.getChunkSize();
            int firstRetrieve = 0;
            boolean hasMore = true;

            // Determine field IDs based on form
            int[] fieldIds = getFieldIdsForForm(formName, parentFieldId);
            OutputInteger numMatches = new OutputInteger();

            log.debug("Extracting work logs from {} with qualification: {}", formName, qualification);

            // Parse qualification string to QualifierInfo
            QualifierInfo qualifierInfo = QualifierBuilder.parseQualification(ctx, formName, qualification);

            while (hasMore) {
                List<Entry> entries = ctx.getListEntryObjects(
                    formName,
                    qualifierInfo,
                    firstRetrieve,
                    chunkSize,
                    null,
                    fieldIds,
                    false,
                    numMatches
                );

                if (entries == null || entries.isEmpty()) {
                    hasMore = false;
                } else {
                    for (Entry entry : entries) {
                        try {
                            WorkLogEntry workLog = mapEntryToWorkLog(entry, source, parentFieldId);
                            workLogs.add(workLog);
                        } catch (Exception e) {
                            log.warn("Failed to map work log entry: {}", e.getMessage());
                        }
                    }

                    if (entries.size() < chunkSize) {
                        hasMore = false;
                    } else {
                        firstRetrieve += chunkSize;
                    }
                }
            }

            // Sort by submit date (newest first)
            workLogs.sort((a, b) -> {
                if (a.getSubmitDate() == null) return 1;
                if (b.getSubmitDate() == null) return -1;
                return b.getSubmitDate().compareTo(a.getSubmitDate());
            });

            log.debug("Extracted {} work logs from {}", workLogs.size(), formName);
            return workLogs;
        });
    }

    private int[] getFieldIdsForForm(String formName, int parentFieldId) {
        List<Integer> fieldIdList = new ArrayList<>();
        fieldIdList.add(FieldIdConstants.REQUEST_ID);
        fieldIdList.add(parentFieldId);

        // Common work log fields
        if (formName.equals(FieldIdConstants.IncidentWorkLog.FORM_NAME)) {
            fieldIdList.add(FieldIdConstants.IncidentWorkLog.WORK_LOG_TYPE);
            fieldIdList.add(FieldIdConstants.IncidentWorkLog.DETAILED_DESCRIPTION);
            fieldIdList.add(FieldIdConstants.IncidentWorkLog.SUBMITTER);
            fieldIdList.add(FieldIdConstants.IncidentWorkLog.SUBMIT_DATE);
            fieldIdList.add(FieldIdConstants.IncidentWorkLog.VIEW_ACCESS);
        } else if (formName.equals(FieldIdConstants.WorkOrderInfo.FORM_NAME)) {
            fieldIdList.add(FieldIdConstants.WorkOrderInfo.WORK_INFO_TYPE);
            fieldIdList.add(FieldIdConstants.WorkOrderInfo.DETAILED_DESCRIPTION);
            fieldIdList.add(FieldIdConstants.WorkOrderInfo.SUBMITTER);
            fieldIdList.add(FieldIdConstants.WorkOrderInfo.SUBMIT_DATE);
        } else if (formName.equals(FieldIdConstants.ChangeWorkLog.FORM_NAME)) {
            fieldIdList.add(FieldIdConstants.ChangeWorkLog.WORK_LOG_TYPE);
            fieldIdList.add(FieldIdConstants.ChangeWorkLog.DETAILED_DESCRIPTION);
            fieldIdList.add(FieldIdConstants.ChangeWorkLog.SUBMITTER);
            fieldIdList.add(FieldIdConstants.ChangeWorkLog.SUBMIT_DATE);
        }

        fieldIdList.add(FieldIdConstants.CREATE_DATE);
        fieldIdList.add(FieldIdConstants.LAST_MODIFIED_DATE);

        // Convert List<Integer> to int[]
        int[] result = new int[fieldIdList.size()];
        for (int i = 0; i < fieldIdList.size(); i++) {
            result[i] = fieldIdList.get(i);
        }
        return result;
    }

    private WorkLogEntry mapEntryToWorkLog(Entry entry, WorkLogEntry.WorkLogSource source, int parentFieldId) {
        Map<Integer, Value> fieldValues = new HashMap<>();
        for (Map.Entry<Integer, Value> e : entry.entrySet()) {
            fieldValues.put(e.getKey(), e.getValue());
        }

        // Get appropriate field IDs based on source
        int descriptionFieldId;
        int typeFieldId;
        int submitterFieldId;
        int submitDateFieldId;

        switch (source) {
            case INCIDENT:
                descriptionFieldId = FieldIdConstants.IncidentWorkLog.DETAILED_DESCRIPTION;
                typeFieldId = FieldIdConstants.IncidentWorkLog.WORK_LOG_TYPE;
                submitterFieldId = FieldIdConstants.IncidentWorkLog.SUBMITTER;
                submitDateFieldId = FieldIdConstants.IncidentWorkLog.SUBMIT_DATE;
                break;
            case WORK_ORDER:
                descriptionFieldId = FieldIdConstants.WorkOrderInfo.DETAILED_DESCRIPTION;
                typeFieldId = FieldIdConstants.WorkOrderInfo.WORK_INFO_TYPE;
                submitterFieldId = FieldIdConstants.WorkOrderInfo.SUBMITTER;
                submitDateFieldId = FieldIdConstants.WorkOrderInfo.SUBMIT_DATE;
                break;
            case CHANGE_REQUEST:
            default:
                descriptionFieldId = FieldIdConstants.ChangeWorkLog.DETAILED_DESCRIPTION;
                typeFieldId = FieldIdConstants.ChangeWorkLog.WORK_LOG_TYPE;
                submitterFieldId = FieldIdConstants.ChangeWorkLog.SUBMITTER;
                submitDateFieldId = FieldIdConstants.ChangeWorkLog.SUBMIT_DATE;
                break;
        }

        return WorkLogEntry.builder()
            .entryId(entry.getEntryId())
            .workLogId(getStringValue(fieldValues, FieldIdConstants.REQUEST_ID))
            .parentId(getStringValue(fieldValues, parentFieldId))
            .source(source)
            .workLogType(getIntValue(fieldValues, typeFieldId))
            .detailedDescription(getStringValue(fieldValues, descriptionFieldId))
            .submitter(getStringValue(fieldValues, submitterFieldId))
            .submitDate(getInstantValue(fieldValues, submitDateFieldId))
            .viewAccess(source == WorkLogEntry.WorkLogSource.INCIDENT ?
                getIntValue(fieldValues, FieldIdConstants.IncidentWorkLog.VIEW_ACCESS) : null)
            .createDate(getInstantValue(fieldValues, FieldIdConstants.CREATE_DATE))
            .lastModifiedDate(getInstantValue(fieldValues, FieldIdConstants.LAST_MODIFIED_DATE))
            .build();
    }

    private String getStringValue(Map<Integer, Value> fields, int fieldId) {
        Value value = fields.get(fieldId);
        if (value == null || value.getValue() == null) {
            return null;
        }
        return value.getValue().toString();
    }

    private Integer getIntValue(Map<Integer, Value> fields, int fieldId) {
        Value value = fields.get(fieldId);
        if (value == null || value.getValue() == null) {
            return null;
        }
        try {
            if (value.getValue() instanceof Integer) {
                return (Integer) value.getValue();
            }
            return Integer.parseInt(value.getValue().toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Instant getInstantValue(Map<Integer, Value> fields, int fieldId) {
        Value value = fields.get(fieldId);
        if (value == null || value.getValue() == null) {
            return null;
        }
        try {
            if (value.getValue() instanceof Timestamp) {
                return Instant.ofEpochSecond(((Timestamp) value.getValue()).getValue());
            }
            long epochSeconds = Long.parseLong(value.getValue().toString());
            return Instant.ofEpochSecond(epochSeconds);
        } catch (Exception e) {
            return null;
        }
    }
}
