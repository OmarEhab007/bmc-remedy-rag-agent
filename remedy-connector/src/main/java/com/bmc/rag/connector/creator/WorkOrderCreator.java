package com.bmc.rag.connector.creator;

import com.bmc.arsys.api.*;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.connector.dto.CreationResult;
import com.bmc.rag.connector.dto.WorkOrderCreationRequest;
import com.bmc.rag.connector.util.FieldIdConstants;
import com.bmc.rag.connector.util.FieldIdConstants.WorkOrderCreate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for creating Work Order records in BMC Remedy via AR API.
 * Uses native Java RPC (not REST) for on-premise compatibility.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderCreator {

    private final ThreadLocalARContext arContext;

    private static final String WORK_ORDER_FORM = FieldIdConstants.WorkOrder.FORM_NAME;

    /**
     * Create a new work order in BMC Remedy.
     *
     * @param request The work order creation request with validated fields
     * @return CreationResult with the created work order ID or error details
     */
    public CreationResult createWorkOrder(WorkOrderCreationRequest request) {
        log.info("Creating new work order: {}", request.getSummary());

        // Validate request
        if (!request.isValidWorkOrderType()) {
            return CreationResult.failure("Invalid work order type. Must be 0-4.");
        }
        if (!request.isValidPriority()) {
            return CreationResult.failure("Invalid priority value. Must be 0-3.");
        }

        try {
            return arContext.executeWithRetry(ctx -> {
                Entry entry = buildWorkOrderEntry(request);

                // Create the entry using AR API
                String entryId = ctx.createEntry(WORK_ORDER_FORM, entry);
                log.info("Created work order entry with ID: {}", entryId);

                // Retrieve the work order ID (display ID)
                String workOrderId = retrieveWorkOrderId(ctx, entryId);
                log.info("Created work order: {}", workOrderId);

                return CreationResult.success(entryId, workOrderId, WORK_ORDER_FORM);
            });
        } catch (ThreadLocalARContext.ARConnectionException e) {
            log.error("Failed to create work order: {}", e.getMessage(), e);
            return CreationResult.failure(e.getMessage(), extractErrorCode(e));
        } catch (Exception e) {
            log.error("Unexpected error creating work order: {}", e.getMessage(), e);
            return CreationResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Build an Entry object for the work order creation.
     */
    private Entry buildWorkOrderEntry(WorkOrderCreationRequest request) {
        Entry entry = new Entry();

        // Required fields
        entry.put(WorkOrderCreate.SUMMARY, new Value(request.getSummary()));
        entry.put(WorkOrderCreate.DESCRIPTION, new Value(request.getDescription()));
        entry.put(WorkOrderCreate.WORK_ORDER_TYPE, new Value(request.getWorkOrderType()));
        entry.put(WorkOrderCreate.PRIORITY, new Value(request.getPriority()));
        entry.put(WorkOrderCreate.STATUS, new Value(FieldIdConstants.StatusValues.WO_NEW));

        // Optional fields
        if (request.getRequesterFirstName() != null) {
            entry.put(WorkOrderCreate.REQUESTER_FIRST_NAME, new Value(request.getRequesterFirstName()));
        }
        if (request.getRequesterLastName() != null) {
            entry.put(WorkOrderCreate.REQUESTER_LAST_NAME, new Value(request.getRequesterLastName()));
        }
        if (request.getLocationCompany() != null) {
            entry.put(WorkOrderCreate.LOCATION_COMPANY, new Value(request.getLocationCompany()));
        }
        if (request.getCategoryTier1() != null) {
            entry.put(WorkOrderCreate.CATEGORY_TIER_1, new Value(request.getCategoryTier1()));
        }
        if (request.getCategoryTier2() != null) {
            entry.put(WorkOrderCreate.CATEGORY_TIER_2, new Value(request.getCategoryTier2()));
        }
        if (request.getCategoryTier3() != null) {
            entry.put(WorkOrderCreate.CATEGORY_TIER_3, new Value(request.getCategoryTier3()));
        }
        if (request.getAssignedGroup() != null) {
            entry.put(WorkOrderCreate.ASSIGNED_GROUP, new Value(request.getAssignedGroup()));
        }
        if (request.getScheduledStartDate() != null) {
            // Convert Instant to Unix timestamp for Remedy
            long epochSeconds = request.getScheduledStartDate().getEpochSecond();
            entry.put(WorkOrderCreate.SCHEDULED_START_DATE, new Value((int) epochSeconds));
        }
        if (request.getScheduledEndDate() != null) {
            long epochSeconds = request.getScheduledEndDate().getEpochSecond();
            entry.put(WorkOrderCreate.SCHEDULED_END_DATE, new Value((int) epochSeconds));
        }

        return entry;
    }

    /**
     * Retrieve the Work Order ID for a created entry.
     */
    private String retrieveWorkOrderId(ARServerUser ctx, String entryId) throws ARException {
        // Get the work order ID field
        int[] fieldIds = { FieldIdConstants.WorkOrder.WORK_ORDER_ID };
        Entry entry = ctx.getEntry(WORK_ORDER_FORM, entryId, fieldIds);

        if (entry != null) {
            Value workOrderIdValue = entry.get(FieldIdConstants.WorkOrder.WORK_ORDER_ID);
            if (workOrderIdValue != null) {
                return workOrderIdValue.toString();
            }
        }

        // Fallback to entry ID if work order ID not found
        log.warn("Could not retrieve work order ID for entry {}, using entry ID", entryId);
        return entryId;
    }

    /**
     * Validate that a work order can be created with the given request.
     * This is a dry-run validation without actually creating the record.
     *
     * @param request The work order creation request
     * @return List of validation errors, empty if valid
     */
    public List<String> validateRequest(WorkOrderCreationRequest request) {
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

        if (!request.isValidWorkOrderType()) {
            errors.add("Work order type must be between 0 and 4");
        }

        if (!request.isValidPriority()) {
            errors.add("Priority must be between 0 and 3");
        }

        // Validate scheduled dates
        if (request.getScheduledStartDate() != null && request.getScheduledEndDate() != null) {
            if (request.getScheduledEndDate().isBefore(request.getScheduledStartDate())) {
                errors.add("Scheduled end date cannot be before start date");
            }
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
