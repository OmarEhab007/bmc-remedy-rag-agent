package com.bmc.rag.connector.extractor;

import com.bmc.arsys.api.*;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.connector.config.RemedyConnectionConfig;
import com.bmc.rag.connector.model.IncidentRecord;
import com.bmc.rag.connector.util.FieldIdConstants;
import com.bmc.rag.connector.util.QualifierBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Extracts Incident records from HPD:Help Desk form.
 * Uses bulk retrieval with pagination to avoid timeouts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentExtractor {

    private final ThreadLocalARContext arContext;
    private final RemedyConnectionConfig config;

    private static final String FORM_NAME = FieldIdConstants.Incident.FORM_NAME;

    // Fields to retrieve
    private static final int[] FIELD_IDS = {
        FieldIdConstants.REQUEST_ID,
        FieldIdConstants.Incident.INCIDENT_NUMBER,
        FieldIdConstants.Incident.SUMMARY,
        FieldIdConstants.Incident.DESCRIPTION,
        FieldIdConstants.Incident.RESOLUTION,
        FieldIdConstants.STATUS,
        FieldIdConstants.Incident.URGENCY,
        FieldIdConstants.Incident.IMPACT,
        FieldIdConstants.Incident.PRIORITY,
        FieldIdConstants.Incident.ASSIGNED_GROUP,
        FieldIdConstants.ASSIGNED_TO,
        FieldIdConstants.Incident.ASSIGNED_SUPPORT_COMPANY,
        FieldIdConstants.Incident.ASSIGNED_SUPPORT_ORG,
        FieldIdConstants.SUBMITTER,
        FieldIdConstants.CREATE_DATE,
        FieldIdConstants.LAST_MODIFIED_DATE,
        FieldIdConstants.LAST_MODIFIED_BY,
        FieldIdConstants.Incident.CATEGORY_TIER_1,
        FieldIdConstants.Incident.CATEGORY_TIER_2,
        FieldIdConstants.Incident.CATEGORY_TIER_3,
        FieldIdConstants.Incident.PRODUCT_TIER_1,
        FieldIdConstants.Incident.PRODUCT_TIER_2,
        FieldIdConstants.Incident.PRODUCT_TIER_3,
        FieldIdConstants.Incident.RESOLUTION_CATEGORY_TIER_1,
        FieldIdConstants.Incident.RESOLUTION_CATEGORY_TIER_2,
        FieldIdConstants.Incident.RESOLUTION_CATEGORY_TIER_3,
        FieldIdConstants.Incident.CUSTOMER_FIRST_NAME,
        FieldIdConstants.Incident.CUSTOMER_LAST_NAME,
        FieldIdConstants.Incident.CUSTOMER_COMPANY,
        FieldIdConstants.Incident.REPORTED_SOURCE,
        FieldIdConstants.Incident.SERVICE_TYPE
    };

    /**
     * Extract all incidents modified since the given timestamp.
     * Uses pagination to handle large result sets.
     *
     * @param lastSyncTimestamp Unix epoch timestamp (seconds)
     * @return List of incident records
     */
    public List<IncidentRecord> extractModifiedSince(long lastSyncTimestamp) {
        String qualification = QualifierBuilder.incrementalSyncQualifier(lastSyncTimestamp);
        return extractWithQualification(qualification);
    }

    /**
     * Extract incidents matching a custom qualification.
     *
     * @param qualification Remedy qualification string (null for all records)
     * @return List of incident records
     */
    public List<IncidentRecord> extractWithQualification(String qualification) {
        return arContext.executeWithRetry(ctx -> {
            List<IncidentRecord> allIncidents = new ArrayList<>();
            int chunkSize = config.getChunkSize();
            int firstRetrieve = 0;
            boolean hasMore = true;


            OutputInteger numMatches = new OutputInteger();

            log.info("Starting incident extraction with qualification: {}",
                qualification != null ? qualification : "(all records)");

            // Parse qualification string to QualifierInfo (BMC AR API requires this)
            QualifierInfo qualifierInfo = QualifierBuilder.parseQualification(ctx, FORM_NAME, qualification);

            while (hasMore) {
                log.debug("Retrieving incidents: firstRetrieve={}, maxRetrieve={}",
                    firstRetrieve, chunkSize);

                // Use getListEntryObjects for bulk retrieval (avoids N+1 pattern)
                List<Entry> entries = ctx.getListEntryObjects(
                    FORM_NAME,
                    qualifierInfo,
                    firstRetrieve,
                    chunkSize,
                    null,  // Sort list (null = default)
                    FIELD_IDS,
                    false, // useLocale
                    numMatches
                );

                if (entries == null || entries.isEmpty()) {
                    hasMore = false;
                    log.debug("No more entries found");
                } else {
                    log.debug("Retrieved {} entries", entries.size());

                    for (Entry entry : entries) {
                        try {
                            IncidentRecord record = mapEntryToIncident(entry);
                            allIncidents.add(record);
                        } catch (Exception e) {
                            log.warn("Failed to map incident entry: {}", e.getMessage());
                        }
                    }

                    // Check if there are more records
                    if (entries.size() < chunkSize) {
                        hasMore = false;
                    } else {
                        firstRetrieve += chunkSize;
                    }
                }
            }

            log.info("Extracted {} total incidents", allIncidents.size());
            return allIncidents;
        });
    }

    /**
     * Extract a single incident by incident number.
     *
     * @param incidentNumber The incident number (e.g., INC000000000001)
     * @return The incident record, or empty optional if not found
     */
    public Optional<IncidentRecord> extractByIncidentNumber(String incidentNumber) {
        String qualification = new QualifierBuilder()
            .equals(FieldIdConstants.Incident.INCIDENT_NUMBER, incidentNumber)
            .build();

        List<IncidentRecord> results = extractWithQualification(qualification);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Extract incidents by assigned group.
     *
     * @param assignedGroup The assigned group name
     * @param lastSyncTimestamp Optional timestamp filter (0 for all)
     * @return List of incident records
     */
    public List<IncidentRecord> extractByAssignedGroup(String assignedGroup, long lastSyncTimestamp) {
        QualifierBuilder builder = new QualifierBuilder()
            .equals(FieldIdConstants.Incident.ASSIGNED_GROUP, assignedGroup);

        if (lastSyncTimestamp > 0) {
            builder.dateAfter(FieldIdConstants.LAST_MODIFIED_DATE, lastSyncTimestamp);
        }

        return extractWithQualification(builder.build());
    }

    /**
     * Extract resolved/closed incidents for knowledge mining.
     *
     * @param lastSyncTimestamp Unix epoch timestamp
     * @return List of resolved incident records
     */
    public List<IncidentRecord> extractResolvedIncidents(long lastSyncTimestamp) {
        String qualification = new QualifierBuilder()
            .dateAfter(FieldIdConstants.LAST_MODIFIED_DATE, lastSyncTimestamp)
            .raw(String.format("('%d' = %d OR '%d' = %d)",
                FieldIdConstants.STATUS, FieldIdConstants.StatusValues.INCIDENT_RESOLVED,
                FieldIdConstants.STATUS, FieldIdConstants.StatusValues.INCIDENT_CLOSED))
            .build();

        return extractWithQualification(qualification);
    }

    /**
     * Get the count of incidents matching a qualification.
     *
     * @param qualification Remedy qualification string
     * @return Count of matching records
     */
    public int getIncidentCount(String qualification) {
        return arContext.executeWithRetry(ctx -> {
            QualifierInfo qualifierInfo = QualifierBuilder.parseQualification(ctx, FORM_NAME, qualification);
            OutputInteger numMatches = new OutputInteger();

            // Use getListEntryObjects with maxRetrieve=0 to get just the count
            List<Entry> entries = ctx.getListEntryObjects(
                FORM_NAME,
                qualifierInfo,
                0,
                0,  // maxRetrieve = 0 for count only
                null,  // sortList
                new int[0],  // fieldIds
                false,
                numMatches
            );

            return numMatches.intValue();
        });
    }

    /**
     * Map a Remedy Entry to an IncidentRecord.
     */
    private IncidentRecord mapEntryToIncident(Entry entry) {
        Map<Integer, Value> fieldValues = new HashMap<>();
        for (Map.Entry<Integer, Value> e : entry.entrySet()) {
            fieldValues.put(e.getKey(), e.getValue());
        }

        return IncidentRecord.builder()
            .entryId(entry.getEntryId())
            .incidentNumber(getStringValue(fieldValues, FieldIdConstants.Incident.INCIDENT_NUMBER))
            .summary(getStringValue(fieldValues, FieldIdConstants.Incident.SUMMARY))
            .description(getStringValue(fieldValues, FieldIdConstants.Incident.DESCRIPTION))
            .resolution(getStringValue(fieldValues, FieldIdConstants.Incident.RESOLUTION))
            .status(getIntValue(fieldValues, FieldIdConstants.STATUS))
            .urgency(getIntValue(fieldValues, FieldIdConstants.Incident.URGENCY))
            .impact(getIntValue(fieldValues, FieldIdConstants.Incident.IMPACT))
            .priority(getIntValue(fieldValues, FieldIdConstants.Incident.PRIORITY))
            .assignedGroup(getStringValue(fieldValues, FieldIdConstants.Incident.ASSIGNED_GROUP))
            .assignedTo(getStringValue(fieldValues, FieldIdConstants.ASSIGNED_TO))
            .assignedSupportCompany(getStringValue(fieldValues, FieldIdConstants.Incident.ASSIGNED_SUPPORT_COMPANY))
            .assignedSupportOrg(getStringValue(fieldValues, FieldIdConstants.Incident.ASSIGNED_SUPPORT_ORG))
            .submitter(getStringValue(fieldValues, FieldIdConstants.SUBMITTER))
            .createDate(getInstantValue(fieldValues, FieldIdConstants.CREATE_DATE))
            .lastModifiedDate(getInstantValue(fieldValues, FieldIdConstants.LAST_MODIFIED_DATE))
            .lastModifiedBy(getStringValue(fieldValues, FieldIdConstants.LAST_MODIFIED_BY))
            .categoryTier1(getStringValue(fieldValues, FieldIdConstants.Incident.CATEGORY_TIER_1))
            .categoryTier2(getStringValue(fieldValues, FieldIdConstants.Incident.CATEGORY_TIER_2))
            .categoryTier3(getStringValue(fieldValues, FieldIdConstants.Incident.CATEGORY_TIER_3))
            .productTier1(getStringValue(fieldValues, FieldIdConstants.Incident.PRODUCT_TIER_1))
            .productTier2(getStringValue(fieldValues, FieldIdConstants.Incident.PRODUCT_TIER_2))
            .productTier3(getStringValue(fieldValues, FieldIdConstants.Incident.PRODUCT_TIER_3))
            .resolutionCategoryTier1(getStringValue(fieldValues, FieldIdConstants.Incident.RESOLUTION_CATEGORY_TIER_1))
            .resolutionCategoryTier2(getStringValue(fieldValues, FieldIdConstants.Incident.RESOLUTION_CATEGORY_TIER_2))
            .resolutionCategoryTier3(getStringValue(fieldValues, FieldIdConstants.Incident.RESOLUTION_CATEGORY_TIER_3))
            .customerFirstName(getStringValue(fieldValues, FieldIdConstants.Incident.CUSTOMER_FIRST_NAME))
            .customerLastName(getStringValue(fieldValues, FieldIdConstants.Incident.CUSTOMER_LAST_NAME))
            .customerCompany(getStringValue(fieldValues, FieldIdConstants.Incident.CUSTOMER_COMPANY))
            .reportedSource(getStringValue(fieldValues, FieldIdConstants.Incident.REPORTED_SOURCE))
            .serviceType(getStringValue(fieldValues, FieldIdConstants.Incident.SERVICE_TYPE))
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
            // Remedy stores dates as Unix epoch (seconds)
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
