package com.bmc.rag.connector.creator;

import com.bmc.arsys.api.*;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.connector.dto.CreationResult;
import com.bmc.rag.connector.dto.IncidentCreationRequest;
import com.bmc.rag.connector.util.FieldIdConstants;
import com.bmc.rag.connector.util.FieldIdConstants.IncidentCreate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for creating Incident records in BMC Remedy via AR API.
 * Uses native Java RPC (not REST) for on-premise compatibility.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentCreator {

    private final ThreadLocalARContext arContext;

    private static final String INCIDENT_FORM = FieldIdConstants.Incident.FORM_NAME;

    /**
     * Create a new incident in BMC Remedy.
     *
     * @param request The incident creation request with validated fields
     * @return CreationResult with the created incident ID or error details
     */
    public CreationResult createIncident(IncidentCreationRequest request) {
        log.info("Creating new incident: {}", request.getSummary());

        // Validate request
        if (!request.isValidImpact()) {
            return CreationResult.failure("Invalid impact value. Must be 1-4.");
        }
        if (!request.isValidUrgency()) {
            return CreationResult.failure("Invalid urgency value. Must be 1-4.");
        }

        try {
            return arContext.executeWithRetry(ctx -> {
                Entry entry = buildIncidentEntry(request);

                // Create the entry using AR API
                String entryId = ctx.createEntry(INCIDENT_FORM, entry);
                log.info("Created incident entry with ID: {}", entryId);

                // Retrieve the incident number (display ID)
                String incidentNumber = retrieveIncidentNumber(ctx, entryId);
                log.info("Created incident: {}", incidentNumber);

                return CreationResult.success(entryId, incidentNumber, INCIDENT_FORM);
            });
        } catch (ThreadLocalARContext.ARConnectionException e) {
            log.error("Failed to create incident: {}", e.getMessage(), e);
            return CreationResult.failure(e.getMessage(), extractErrorCode(e));
        } catch (Exception e) {
            log.error("Unexpected error creating incident: {}", e.getMessage(), e);
            return CreationResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Build an Entry object for the incident creation.
     */
    private Entry buildIncidentEntry(IncidentCreationRequest request) {
        Entry entry = new Entry();

        // Required fields
        entry.put(IncidentCreate.SUMMARY, new Value(request.getSummary()));
        entry.put(IncidentCreate.DESCRIPTION, new Value(request.getDescription()));
        entry.put(IncidentCreate.IMPACT, new Value(request.getImpact()));
        entry.put(IncidentCreate.URGENCY, new Value(request.getUrgency()));
        entry.put(IncidentCreate.STATUS, new Value(FieldIdConstants.StatusValues.INCIDENT_NEW));
        entry.put(IncidentCreate.REPORTED_SOURCE, new Value(IncidentCreate.ReportedSource.SELF_SERVICE));

        // Optional fields
        if (request.getRequesterFirstName() != null) {
            entry.put(IncidentCreate.CUSTOMER_FIRST_NAME, new Value(request.getRequesterFirstName()));
        }
        if (request.getRequesterLastName() != null) {
            entry.put(IncidentCreate.CUSTOMER_LAST_NAME, new Value(request.getRequesterLastName()));
        }
        if (request.getRequesterCompany() != null) {
            entry.put(IncidentCreate.CUSTOMER_COMPANY, new Value(request.getRequesterCompany()));
        }
        if (request.getCategoryTier1() != null) {
            entry.put(IncidentCreate.CATEGORY_TIER_1, new Value(request.getCategoryTier1()));
        }
        if (request.getCategoryTier2() != null) {
            entry.put(IncidentCreate.CATEGORY_TIER_2, new Value(request.getCategoryTier2()));
        }
        if (request.getCategoryTier3() != null) {
            entry.put(IncidentCreate.CATEGORY_TIER_3, new Value(request.getCategoryTier3()));
        }
        if (request.getAssignedGroup() != null) {
            entry.put(IncidentCreate.ASSIGNED_GROUP, new Value(request.getAssignedGroup()));
        }
        if (request.getServiceType() != null) {
            entry.put(IncidentCreate.SERVICE_TYPE, new Value(request.getServiceType()));
        }
        if (request.getConfigurationItem() != null) {
            entry.put(IncidentCreate.CI_NAME, new Value(request.getConfigurationItem()));
        }
        if (request.getLocation() != null) {
            entry.put(IncidentCreate.LOCATION, new Value(request.getLocation()));
        }

        return entry;
    }

    /**
     * Retrieve the Incident Number for a created entry.
     */
    private String retrieveIncidentNumber(ARServerUser ctx, String entryId) throws ARException {
        // Get the incident number field
        int[] fieldIds = { FieldIdConstants.Incident.INCIDENT_NUMBER };
        Entry entry = ctx.getEntry(INCIDENT_FORM, entryId, fieldIds);

        if (entry != null) {
            Value incidentNumberValue = entry.get(FieldIdConstants.Incident.INCIDENT_NUMBER);
            if (incidentNumberValue != null) {
                return incidentNumberValue.toString();
            }
        }

        // Fallback to entry ID if incident number not found
        log.warn("Could not retrieve incident number for entry {}, using entry ID", entryId);
        return entryId;
    }

    /**
     * Validate that an incident can be created with the given request.
     * This is a dry-run validation without actually creating the record.
     *
     * @param request The incident creation request
     * @return List of validation errors, empty if valid
     */
    public List<String> validateRequest(IncidentCreationRequest request) {
        java.util.List<String> errors = new java.util.ArrayList<>();

        if (request.getSummary() == null || request.getSummary().isBlank()) {
            errors.add("Summary is required");
        } else if (request.getSummary().length() > 255) {
            errors.add("Summary must not exceed 255 characters");
        }

        if (request.getDescription() == null || request.getDescription().isBlank()) {
            errors.add("Description is required");
        } else if (request.getDescription().length() > 32000) {
            errors.add("Description must not exceed 32000 characters");
        }

        if (!request.isValidImpact()) {
            errors.add("Impact must be between 1 and 4");
        }

        if (!request.isValidUrgency()) {
            errors.add("Urgency must be between 1 and 4");
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
