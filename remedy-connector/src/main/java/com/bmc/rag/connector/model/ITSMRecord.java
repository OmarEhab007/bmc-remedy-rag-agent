package com.bmc.rag.connector.model;

import java.time.Instant;

/**
 * Common interface for all ITSM records (Incident, Work Order, Change Request, Knowledge Article).
 */
public interface ITSMRecord {

    /**
     * Get the unique entry ID from Remedy.
     */
    String getEntryId();

    /**
     * Get the record type name.
     */
    String getRecordType();

    /**
     * Get the business record ID (e.g., INC000000000001, WO0000000000001).
     */
    String getRecordId();

    /**
     * Get the record title/summary.
     */
    String getTitle();

    /**
     * Get the main content/description.
     */
    String getContent();

    /**
     * Get the status value.
     */
    Integer getStatus();

    /**
     * Get the assigned group.
     */
    String getAssignedGroup();

    /**
     * Get the last modified date.
     */
    Instant getLastModifiedDate();

    /**
     * Get the create date.
     */
    Instant getCreateDate();
}
