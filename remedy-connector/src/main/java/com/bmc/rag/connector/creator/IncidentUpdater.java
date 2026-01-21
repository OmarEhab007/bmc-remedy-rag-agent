package com.bmc.rag.connector.creator;

import com.bmc.arsys.api.*;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.connector.dto.CreationResult;
import com.bmc.rag.connector.dto.IncidentUpdateRequest;
import com.bmc.rag.connector.util.FieldIdConstants;
import com.bmc.rag.connector.util.FieldIdConstants.Incident;
import com.bmc.rag.connector.util.FieldIdConstants.IncidentWorkLog;
import com.bmc.rag.connector.util.QualifierBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for updating Incident records in BMC Remedy via AR API.
 * Supports field updates and work log additions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentUpdater {

    private final ThreadLocalARContext arContext;

    private static final String INCIDENT_FORM = FieldIdConstants.Incident.FORM_NAME;
    private static final String WORKLOG_FORM = FieldIdConstants.IncidentWorkLog.FORM_NAME;

    /**
     * Update an existing incident in BMC Remedy.
     *
     * @param request The incident update request with fields to update
     * @return CreationResult with the updated incident ID or error details
     */
    public CreationResult updateIncident(IncidentUpdateRequest request) {
        log.info("Updating incident: {}", request.getIncidentNumber());

        // Validate request
        if (request.getIncidentNumber() == null || request.getIncidentNumber().isBlank()) {
            return CreationResult.failure("Incident number is required for update");
        }
        if (!request.hasUpdates()) {
            return CreationResult.failure("No update fields specified");
        }
        if (!request.isValidImpact()) {
            return CreationResult.failure("Invalid impact value. Must be 1-4.");
        }
        if (!request.isValidUrgency()) {
            return CreationResult.failure("Invalid urgency value. Must be 1-4.");
        }
        if (!request.isValidStatus()) {
            return CreationResult.failure("Invalid status value. Must be 0-6.");
        }

        try {
            return arContext.executeWithRetry(ctx -> {
                // First, find the entry ID for the incident number
                String entryId = findIncidentEntryId(ctx, request.getIncidentNumber());
                if (entryId == null) {
                    return CreationResult.failure("Incident not found: " + request.getIncidentNumber());
                }

                // Build and apply the update
                Entry updateEntry = buildUpdateEntry(request);
                ctx.setEntry(INCIDENT_FORM, entryId, updateEntry, null, 0);
                log.info("Updated incident entry: {}", entryId);

                // Add work log if specified
                if (request.getWorkLog() != null && !request.getWorkLog().isBlank()) {
                    addWorkLog(ctx, request.getIncidentNumber(), request.getWorkLog(),
                              request.getWorkLogType() != null ? request.getWorkLogType() : 1);
                }

                return CreationResult.success(entryId, request.getIncidentNumber(), INCIDENT_FORM)
                    .withMessage("Incident updated successfully");
            });
        } catch (ThreadLocalARContext.ARConnectionException e) {
            log.error("Failed to update incident: {}", e.getMessage(), e);
            return CreationResult.failure(e.getMessage(), extractErrorCode(e));
        } catch (Exception e) {
            log.error("Unexpected error updating incident: {}", e.getMessage(), e);
            return CreationResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Add a work log entry to an incident.
     *
     * @param incidentNumber The incident number
     * @param workLogText The work log text
     * @param workLogType The work log type (default: 1 = Working Log)
     * @return CreationResult indicating success or failure
     */
    public CreationResult addWorkLogToIncident(String incidentNumber, String workLogText, int workLogType) {
        log.info("Adding work log to incident: {}", incidentNumber);

        if (incidentNumber == null || incidentNumber.isBlank()) {
            return CreationResult.failure("Incident number is required");
        }
        if (workLogText == null || workLogText.isBlank()) {
            return CreationResult.failure("Work log text is required");
        }

        try {
            return arContext.executeWithRetry(ctx -> {
                String workLogId = addWorkLog(ctx, incidentNumber, workLogText, workLogType);
                log.info("Added work log {} to incident {}", workLogId, incidentNumber);
                return CreationResult.success(workLogId, incidentNumber, WORKLOG_FORM)
                    .withMessage("Work log added successfully");
            });
        } catch (ThreadLocalARContext.ARConnectionException e) {
            log.error("Failed to add work log: {}", e.getMessage(), e);
            return CreationResult.failure(e.getMessage(), extractErrorCode(e));
        } catch (Exception e) {
            log.error("Unexpected error adding work log: {}", e.getMessage(), e);
            return CreationResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Find the entry ID for an incident number.
     */
    private String findIncidentEntryId(ARServerUser ctx, String incidentNumber) throws ARException {
        // Build qualifier to find by incident number
        String qualification = "'" + Incident.INCIDENT_NUMBER + "' = \"" + incidentNumber + "\"";

        QualifierInfo qualifier = QualifierBuilder.parseQualification(ctx, INCIDENT_FORM, qualification);

        List<Entry> entries = ctx.getListEntryObjects(
            INCIDENT_FORM,
            qualifier,
            0, // firstRetrieve
            1, // maxRetrieve
            null, // sortList
            new int[]{ FieldIdConstants.REQUEST_ID }, // fieldIds
            false, // useLocale
            null  // outputDoc
        );

        if (entries != null && !entries.isEmpty()) {
            Value entryIdValue = entries.get(0).get(FieldIdConstants.REQUEST_ID);
            if (entryIdValue != null) {
                return entryIdValue.toString();
            }
        }

        return null;
    }

    /**
     * Build an Entry object for the incident update.
     */
    private Entry buildUpdateEntry(IncidentUpdateRequest request) {
        Entry entry = new Entry();

        if (request.getSummary() != null) {
            entry.put(Incident.SUMMARY, new Value(request.getSummary()));
        }
        if (request.getDescription() != null) {
            entry.put(Incident.DESCRIPTION, new Value(request.getDescription()));
        }
        if (request.getImpact() != null) {
            entry.put(Incident.IMPACT, new Value(request.getImpact()));
        }
        if (request.getUrgency() != null) {
            entry.put(Incident.URGENCY, new Value(request.getUrgency()));
        }
        if (request.getStatus() != null) {
            entry.put(FieldIdConstants.STATUS, new Value(request.getStatus()));
        }
        if (request.getResolution() != null) {
            entry.put(Incident.RESOLUTION, new Value(request.getResolution()));
        }
        if (request.getAssignedGroup() != null) {
            entry.put(Incident.ASSIGNED_GROUP, new Value(request.getAssignedGroup()));
        }
        if (request.getCategoryTier1() != null) {
            entry.put(Incident.CATEGORY_TIER_1, new Value(request.getCategoryTier1()));
        }
        if (request.getCategoryTier2() != null) {
            entry.put(Incident.CATEGORY_TIER_2, new Value(request.getCategoryTier2()));
        }
        if (request.getCategoryTier3() != null) {
            entry.put(Incident.CATEGORY_TIER_3, new Value(request.getCategoryTier3()));
        }
        if (request.getResolutionCategoryTier1() != null) {
            entry.put(Incident.RESOLUTION_CATEGORY_TIER_1, new Value(request.getResolutionCategoryTier1()));
        }
        if (request.getResolutionCategoryTier2() != null) {
            entry.put(Incident.RESOLUTION_CATEGORY_TIER_2, new Value(request.getResolutionCategoryTier2()));
        }
        if (request.getResolutionCategoryTier3() != null) {
            entry.put(Incident.RESOLUTION_CATEGORY_TIER_3, new Value(request.getResolutionCategoryTier3()));
        }

        return entry;
    }

    /**
     * Add a work log entry to an incident.
     */
    private String addWorkLog(ARServerUser ctx, String incidentNumber, String workLogText, int workLogType)
            throws ARException {

        Entry workLogEntry = new Entry();
        workLogEntry.put(IncidentWorkLog.INCIDENT_NUMBER, new Value(incidentNumber));
        workLogEntry.put(IncidentWorkLog.DETAILED_DESCRIPTION, new Value(workLogText));
        workLogEntry.put(IncidentWorkLog.WORK_LOG_TYPE, new Value(workLogType));
        workLogEntry.put(IncidentWorkLog.VIEW_ACCESS, new Value(0)); // Internal (0) or Public (1)

        String workLogId = ctx.createEntry(WORKLOG_FORM, workLogEntry);
        log.debug("Created work log entry: {}", workLogId);

        return workLogId;
    }

    /**
     * Validate that an incident update can be performed.
     *
     * @param request The update request
     * @return List of validation errors, empty if valid
     */
    public List<String> validateRequest(IncidentUpdateRequest request) {
        List<String> errors = new ArrayList<>();

        if (request.getIncidentNumber() == null || request.getIncidentNumber().isBlank()) {
            errors.add("Incident number is required");
        }

        if (!request.hasUpdates()) {
            errors.add("At least one update field must be specified");
        }

        if (request.getSummary() != null && request.getSummary().length() > 255) {
            errors.add("Summary must not exceed 255 characters");
        }

        if (request.getDescription() != null && request.getDescription().length() > 32000) {
            errors.add("Description must not exceed 32000 characters");
        }

        if (!request.isValidImpact()) {
            errors.add("Impact must be between 1 and 4");
        }

        if (!request.isValidUrgency()) {
            errors.add("Urgency must be between 1 and 4");
        }

        if (!request.isValidStatus()) {
            errors.add("Status must be between 0 and 6");
        }

        // Validate resolution is provided when resolving
        if (request.getStatus() != null && request.getStatus() == 4 &&
            (request.getResolution() == null || request.getResolution().isBlank())) {
            errors.add("Resolution is required when setting status to Resolved");
        }

        return errors;
    }

    /**
     * Check if the Remedy connection is available.
     */
    public boolean isAvailable() {
        return arContext.isEnabled() && arContext.verifyConnection();
    }

    /**
     * Extract AR error code from exception message.
     */
    private String extractErrorCode(Exception e) {
        String message = e.getMessage();
        if (message != null && message.contains("ARERR")) {
            int start = message.indexOf("ARERR");
            int end = message.indexOf(" ", start);
            if (end > start) {
                return message.substring(start, end);
            }
        }
        return null;
    }
}
