package com.bmc.rag.connector.extractor;

import com.bmc.arsys.api.*;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.connector.config.RemedyConnectionConfig;
import com.bmc.rag.connector.model.WorkOrderRecord;
import com.bmc.rag.connector.util.FieldIdConstants;
import com.bmc.rag.connector.util.QualifierBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Extracts Work Order records from WOI:WorkOrder form.
 * Uses bulk retrieval with pagination to avoid timeouts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkOrderExtractor {

    private final ThreadLocalARContext arContext;
    private final RemedyConnectionConfig config;

    private static final String FORM_NAME = FieldIdConstants.WorkOrder.FORM_NAME;

    // Fields to retrieve
    private static final int[] FIELD_IDS = {
        FieldIdConstants.REQUEST_ID,
        FieldIdConstants.WorkOrder.WORK_ORDER_ID,
        FieldIdConstants.WorkOrder.SUMMARY,
        FieldIdConstants.WorkOrder.DESCRIPTION,
        FieldIdConstants.STATUS,
        FieldIdConstants.WorkOrder.PRIORITY,
        FieldIdConstants.WorkOrder.ASSIGNED_GROUP,
        FieldIdConstants.ASSIGNED_TO,
        FieldIdConstants.WorkOrder.ASSIGNED_SUPPORT_COMPANY,
        FieldIdConstants.SUBMITTER,
        FieldIdConstants.CREATE_DATE,
        FieldIdConstants.LAST_MODIFIED_DATE,
        FieldIdConstants.LAST_MODIFIED_BY,
        FieldIdConstants.WorkOrder.CATEGORY_TIER_1,
        FieldIdConstants.WorkOrder.CATEGORY_TIER_2,
        FieldIdConstants.WorkOrder.CATEGORY_TIER_3,
        FieldIdConstants.WorkOrder.REQUESTER_FIRST_NAME,
        FieldIdConstants.WorkOrder.REQUESTER_LAST_NAME,
        FieldIdConstants.WorkOrder.LOCATION_COMPANY,
        FieldIdConstants.WorkOrder.SCHEDULED_START_DATE,
        FieldIdConstants.WorkOrder.SCHEDULED_END_DATE
    };

    /**
     * Extract all work orders modified since the given timestamp.
     *
     * @param lastSyncTimestamp Unix epoch timestamp (seconds)
     * @return List of work order records
     */
    public List<WorkOrderRecord> extractModifiedSince(long lastSyncTimestamp) {
        String qualification = QualifierBuilder.incrementalSyncQualifier(lastSyncTimestamp);
        return extractWithQualification(qualification);
    }

    /**
     * Extract work orders matching a custom qualification.
     *
     * @param qualification Remedy qualification string (null for all records)
     * @return List of work order records
     */
    public List<WorkOrderRecord> extractWithQualification(String qualification) {
        return arContext.executeWithRetry(ctx -> {
            List<WorkOrderRecord> allWorkOrders = new ArrayList<>();
            int chunkSize = config.getChunkSize();
            int firstRetrieve = 0;
            boolean hasMore = true;



            OutputInteger numMatches = new OutputInteger();

            log.info("Starting work order extraction with qualification: {}",
                qualification != null ? qualification : "(all records)");

            // Parse qualification string to QualifierInfo (BMC AR API requires this)
            QualifierInfo qualifierInfo = QualifierBuilder.parseQualification(ctx, FORM_NAME, qualification);

            while (hasMore) {
                log.debug("Retrieving work orders: firstRetrieve={}, maxRetrieve={}",
                    firstRetrieve, chunkSize);

                List<Entry> entries = ctx.getListEntryObjects(
                    FORM_NAME,
                    qualifierInfo,
                    firstRetrieve,
                    chunkSize,
                    null,
                    FIELD_IDS,
                    false,
                    numMatches
                );

                if (entries == null || entries.isEmpty()) {
                    hasMore = false;
                } else {
                    log.debug("Retrieved {} entries", entries.size());

                    for (Entry entry : entries) {
                        try {
                            WorkOrderRecord record = mapEntryToWorkOrder(entry);
                            allWorkOrders.add(record);
                        } catch (Exception e) {
                            log.warn("Failed to map work order entry: {}", e.getMessage());
                        }
                    }

                    if (entries.size() < chunkSize) {
                        hasMore = false;
                    } else {
                        firstRetrieve += chunkSize;
                    }
                }
            }

            log.info("Extracted {} total work orders", allWorkOrders.size());
            return allWorkOrders;
        });
    }

    /**
     * Extract a single work order by ID.
     *
     * @param workOrderId The work order ID
     * @return The work order record, or empty optional if not found
     */
    public Optional<WorkOrderRecord> extractByWorkOrderId(String workOrderId) {
        String qualification = new QualifierBuilder()
            .equals(FieldIdConstants.WorkOrder.WORK_ORDER_ID, workOrderId)
            .build();

        List<WorkOrderRecord> results = extractWithQualification(qualification);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Extract work orders by assigned group.
     *
     * @param assignedGroup The assigned group name
     * @param lastSyncTimestamp Optional timestamp filter (0 for all)
     * @return List of work order records
     */
    public List<WorkOrderRecord> extractByAssignedGroup(String assignedGroup, long lastSyncTimestamp) {
        QualifierBuilder builder = new QualifierBuilder()
            .equals(FieldIdConstants.WorkOrder.ASSIGNED_GROUP, assignedGroup);

        if (lastSyncTimestamp > 0) {
            builder.dateAfter(FieldIdConstants.LAST_MODIFIED_DATE, lastSyncTimestamp);
        }

        return extractWithQualification(builder.build());
    }

    /**
     * Map a Remedy Entry to a WorkOrderRecord.
     */
    private WorkOrderRecord mapEntryToWorkOrder(Entry entry) {
        Map<Integer, Value> fieldValues = new HashMap<>();
        for (Map.Entry<Integer, Value> e : entry.entrySet()) {
            fieldValues.put(e.getKey(), e.getValue());
        }

        return WorkOrderRecord.builder()
            .entryId(entry.getEntryId())
            .workOrderId(getStringValue(fieldValues, FieldIdConstants.WorkOrder.WORK_ORDER_ID))
            .summary(getStringValue(fieldValues, FieldIdConstants.WorkOrder.SUMMARY))
            .description(getStringValue(fieldValues, FieldIdConstants.WorkOrder.DESCRIPTION))
            .status(getIntValue(fieldValues, FieldIdConstants.STATUS))
            .priority(getIntValue(fieldValues, FieldIdConstants.WorkOrder.PRIORITY))
            .assignedGroup(getStringValue(fieldValues, FieldIdConstants.WorkOrder.ASSIGNED_GROUP))
            .assignedTo(getStringValue(fieldValues, FieldIdConstants.ASSIGNED_TO))
            .assignedSupportCompany(getStringValue(fieldValues, FieldIdConstants.WorkOrder.ASSIGNED_SUPPORT_COMPANY))
            .submitter(getStringValue(fieldValues, FieldIdConstants.SUBMITTER))
            .createDate(getInstantValue(fieldValues, FieldIdConstants.CREATE_DATE))
            .lastModifiedDate(getInstantValue(fieldValues, FieldIdConstants.LAST_MODIFIED_DATE))
            .lastModifiedBy(getStringValue(fieldValues, FieldIdConstants.LAST_MODIFIED_BY))
            .categoryTier1(getStringValue(fieldValues, FieldIdConstants.WorkOrder.CATEGORY_TIER_1))
            .categoryTier2(getStringValue(fieldValues, FieldIdConstants.WorkOrder.CATEGORY_TIER_2))
            .categoryTier3(getStringValue(fieldValues, FieldIdConstants.WorkOrder.CATEGORY_TIER_3))
            .requesterFirstName(getStringValue(fieldValues, FieldIdConstants.WorkOrder.REQUESTER_FIRST_NAME))
            .requesterLastName(getStringValue(fieldValues, FieldIdConstants.WorkOrder.REQUESTER_LAST_NAME))
            .locationCompany(getStringValue(fieldValues, FieldIdConstants.WorkOrder.LOCATION_COMPANY))
            .scheduledStartDate(getInstantValue(fieldValues, FieldIdConstants.WorkOrder.SCHEDULED_START_DATE))
            .scheduledEndDate(getInstantValue(fieldValues, FieldIdConstants.WorkOrder.SCHEDULED_END_DATE))
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
