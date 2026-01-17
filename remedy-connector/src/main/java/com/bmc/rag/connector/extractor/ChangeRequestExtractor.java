package com.bmc.rag.connector.extractor;

import com.bmc.arsys.api.*;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.connector.config.RemedyConnectionConfig;
import com.bmc.rag.connector.model.ChangeRequestRecord;
import com.bmc.rag.connector.util.FieldIdConstants;
import com.bmc.rag.connector.util.QualifierBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Extracts Change Request records from CHG:Infrastructure Change form.
 * Uses bulk retrieval with pagination to avoid timeouts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChangeRequestExtractor {

    private final ThreadLocalARContext arContext;
    private final RemedyConnectionConfig config;

    private static final String FORM_NAME = FieldIdConstants.ChangeRequest.FORM_NAME;

    // Fields to retrieve
    private static final int[] FIELD_IDS = {
        FieldIdConstants.REQUEST_ID,
        FieldIdConstants.ChangeRequest.CHANGE_ID,
        FieldIdConstants.ChangeRequest.SUMMARY,
        FieldIdConstants.ChangeRequest.DESCRIPTION,
        FieldIdConstants.ChangeRequest.CHANGE_REASON,
        FieldIdConstants.ChangeRequest.IMPLEMENTATION_PLAN,
        FieldIdConstants.ChangeRequest.ROLLBACK_PLAN,
        FieldIdConstants.STATUS,
        FieldIdConstants.ChangeRequest.RISK_LEVEL,
        FieldIdConstants.ChangeRequest.IMPACT,
        FieldIdConstants.ChangeRequest.URGENCY,
        FieldIdConstants.ChangeRequest.CHANGE_TYPE,
        FieldIdConstants.ChangeRequest.CHANGE_CLASS,
        FieldIdConstants.ChangeRequest.ASSIGNED_GROUP,
        FieldIdConstants.ASSIGNED_TO,
        FieldIdConstants.ChangeRequest.ASSIGNED_SUPPORT_COMPANY,
        FieldIdConstants.SUBMITTER,
        FieldIdConstants.CREATE_DATE,
        FieldIdConstants.LAST_MODIFIED_DATE,
        FieldIdConstants.LAST_MODIFIED_BY,
        FieldIdConstants.ChangeRequest.CATEGORY_TIER_1,
        FieldIdConstants.ChangeRequest.CATEGORY_TIER_2,
        FieldIdConstants.ChangeRequest.CATEGORY_TIER_3,
        FieldIdConstants.ChangeRequest.SCHEDULED_START_DATE,
        FieldIdConstants.ChangeRequest.SCHEDULED_END_DATE,
        FieldIdConstants.ChangeRequest.ACTUAL_START_DATE,
        FieldIdConstants.ChangeRequest.ACTUAL_END_DATE
    };

    /**
     * Extract all change requests modified since the given timestamp.
     *
     * @param lastSyncTimestamp Unix epoch timestamp (seconds)
     * @return List of change request records
     */
    public List<ChangeRequestRecord> extractModifiedSince(long lastSyncTimestamp) {
        String qualification = QualifierBuilder.incrementalSyncQualifier(lastSyncTimestamp);
        return extractWithQualification(qualification);
    }

    /**
     * Extract change requests matching a custom qualification.
     *
     * @param qualification Remedy qualification string (null for all records)
     * @return List of change request records
     */
    public List<ChangeRequestRecord> extractWithQualification(String qualification) {
        return arContext.executeWithRetry(ctx -> {
            List<ChangeRequestRecord> allChanges = new ArrayList<>();
            int chunkSize = config.getChunkSize();
            int firstRetrieve = 0;
            boolean hasMore = true;



            OutputInteger numMatches = new OutputInteger();

            log.info("Starting change request extraction with qualification: {}",
                qualification != null ? qualification : "(all records)");

            // Parse qualification string to QualifierInfo (BMC AR API requires this)
            QualifierInfo qualifierInfo = QualifierBuilder.parseQualification(ctx, FORM_NAME, qualification);

            while (hasMore) {
                log.debug("Retrieving change requests: firstRetrieve={}, maxRetrieve={}",
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
                            ChangeRequestRecord record = mapEntryToChangeRequest(entry);
                            allChanges.add(record);
                        } catch (Exception e) {
                            log.warn("Failed to map change request entry: {}", e.getMessage());
                        }
                    }

                    if (entries.size() < chunkSize) {
                        hasMore = false;
                    } else {
                        firstRetrieve += chunkSize;
                    }
                }
            }

            log.info("Extracted {} total change requests", allChanges.size());
            return allChanges;
        });
    }

    /**
     * Extract a single change request by ID.
     *
     * @param changeId The change request ID
     * @return The change request record, or empty optional if not found
     */
    public Optional<ChangeRequestRecord> extractByChangeId(String changeId) {
        String qualification = new QualifierBuilder()
            .equals(FieldIdConstants.ChangeRequest.CHANGE_ID, changeId)
            .build();

        List<ChangeRequestRecord> results = extractWithQualification(qualification);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Extract change requests by assigned group.
     *
     * @param assignedGroup The assigned group name
     * @param lastSyncTimestamp Optional timestamp filter (0 for all)
     * @return List of change request records
     */
    public List<ChangeRequestRecord> extractByAssignedGroup(String assignedGroup, long lastSyncTimestamp) {
        QualifierBuilder builder = new QualifierBuilder()
            .equals(FieldIdConstants.ChangeRequest.ASSIGNED_GROUP, assignedGroup);

        if (lastSyncTimestamp > 0) {
            builder.dateAfter(FieldIdConstants.LAST_MODIFIED_DATE, lastSyncTimestamp);
        }

        return extractWithQualification(builder.build());
    }

    /**
     * Extract completed change requests for knowledge mining.
     *
     * @param lastSyncTimestamp Unix epoch timestamp
     * @return List of completed change request records
     */
    public List<ChangeRequestRecord> extractCompletedChanges(long lastSyncTimestamp) {
        String qualification = new QualifierBuilder()
            .dateAfter(FieldIdConstants.LAST_MODIFIED_DATE, lastSyncTimestamp)
            .raw(String.format("('%d' = %d OR '%d' = %d)",
                FieldIdConstants.STATUS, FieldIdConstants.StatusValues.CHG_COMPLETED,
                FieldIdConstants.STATUS, FieldIdConstants.StatusValues.CHG_CLOSED))
            .build();

        return extractWithQualification(qualification);
    }

    /**
     * Check which change IDs from the provided list still exist in Remedy.
     *
     * @param changeIds List of change IDs to check
     * @return Set of change IDs that still exist
     */
    public Set<String> checkExistence(List<String> changeIds) {
        if (changeIds == null || changeIds.isEmpty()) {
            return Collections.emptySet();
        }

        return arContext.executeWithRetry(ctx -> {
            Set<String> existingIds = new HashSet<>();

            // Build OR qualification for all change IDs
            StringBuilder orQualification = new StringBuilder();
            for (int i = 0; i < changeIds.size(); i++) {
                if (i > 0) orQualification.append(" OR ");
                orQualification.append("'")
                    .append(FieldIdConstants.ChangeRequest.CHANGE_ID)
                    .append("' = \"")
                    .append(changeIds.get(i).replace("\"", "\\\""))
                    .append("\"");
            }

            QualifierInfo qualifierInfo = QualifierBuilder.parseQualification(
                ctx, FORM_NAME, orQualification.toString());
            OutputInteger numMatches = new OutputInteger();

            // Only fetch the change ID field
            int[] fieldIds = { FieldIdConstants.ChangeRequest.CHANGE_ID };

            List<Entry> entries = ctx.getListEntryObjects(
                FORM_NAME,
                qualifierInfo,
                0,
                changeIds.size(),
                null,
                fieldIds,
                false,
                numMatches
            );

            if (entries != null) {
                for (Entry entry : entries) {
                    Value value = entry.get(FieldIdConstants.ChangeRequest.CHANGE_ID);
                    if (value != null && value.getValue() != null) {
                        existingIds.add(value.getValue().toString());
                    }
                }
            }

            return existingIds;
        });
    }

    /**
     * Map a Remedy Entry to a ChangeRequestRecord.
     */
    private ChangeRequestRecord mapEntryToChangeRequest(Entry entry) {
        Map<Integer, Value> fieldValues = new HashMap<>();
        for (Map.Entry<Integer, Value> e : entry.entrySet()) {
            fieldValues.put(e.getKey(), e.getValue());
        }

        return ChangeRequestRecord.builder()
            .entryId(entry.getEntryId())
            .changeId(getStringValue(fieldValues, FieldIdConstants.ChangeRequest.CHANGE_ID))
            .summary(getStringValue(fieldValues, FieldIdConstants.ChangeRequest.SUMMARY))
            .description(getStringValue(fieldValues, FieldIdConstants.ChangeRequest.DESCRIPTION))
            .changeReason(getStringValue(fieldValues, FieldIdConstants.ChangeRequest.CHANGE_REASON))
            .implementationPlan(getStringValue(fieldValues, FieldIdConstants.ChangeRequest.IMPLEMENTATION_PLAN))
            .rollbackPlan(getStringValue(fieldValues, FieldIdConstants.ChangeRequest.ROLLBACK_PLAN))
            .status(getIntValue(fieldValues, FieldIdConstants.STATUS))
            .riskLevel(getIntValue(fieldValues, FieldIdConstants.ChangeRequest.RISK_LEVEL))
            .impact(getIntValue(fieldValues, FieldIdConstants.ChangeRequest.IMPACT))
            .urgency(getIntValue(fieldValues, FieldIdConstants.ChangeRequest.URGENCY))
            .changeType(getStringValue(fieldValues, FieldIdConstants.ChangeRequest.CHANGE_TYPE))
            .changeClass(getStringValue(fieldValues, FieldIdConstants.ChangeRequest.CHANGE_CLASS))
            .assignedGroup(getStringValue(fieldValues, FieldIdConstants.ChangeRequest.ASSIGNED_GROUP))
            .assignedTo(getStringValue(fieldValues, FieldIdConstants.ASSIGNED_TO))
            .assignedSupportCompany(getStringValue(fieldValues, FieldIdConstants.ChangeRequest.ASSIGNED_SUPPORT_COMPANY))
            .submitter(getStringValue(fieldValues, FieldIdConstants.SUBMITTER))
            .createDate(getInstantValue(fieldValues, FieldIdConstants.CREATE_DATE))
            .lastModifiedDate(getInstantValue(fieldValues, FieldIdConstants.LAST_MODIFIED_DATE))
            .lastModifiedBy(getStringValue(fieldValues, FieldIdConstants.LAST_MODIFIED_BY))
            .categoryTier1(getStringValue(fieldValues, FieldIdConstants.ChangeRequest.CATEGORY_TIER_1))
            .categoryTier2(getStringValue(fieldValues, FieldIdConstants.ChangeRequest.CATEGORY_TIER_2))
            .categoryTier3(getStringValue(fieldValues, FieldIdConstants.ChangeRequest.CATEGORY_TIER_3))
            .scheduledStartDate(getInstantValue(fieldValues, FieldIdConstants.ChangeRequest.SCHEDULED_START_DATE))
            .scheduledEndDate(getInstantValue(fieldValues, FieldIdConstants.ChangeRequest.SCHEDULED_END_DATE))
            .actualStartDate(getInstantValue(fieldValues, FieldIdConstants.ChangeRequest.ACTUAL_START_DATE))
            .actualEndDate(getInstantValue(fieldValues, FieldIdConstants.ChangeRequest.ACTUAL_END_DATE))
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
