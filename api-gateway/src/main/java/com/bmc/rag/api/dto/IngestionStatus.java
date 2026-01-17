package com.bmc.rag.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Ingestion status DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionStatus {

    /**
     * Overall status.
     */
    private String status;

    /**
     * Status per source type.
     */
    private Map<String, SourceStatus> sources;

    /**
     * Total chunks in vector store.
     */
    private Map<String, Long> statistics;

    /**
     * Status for a specific source type.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceStatus {
        private String sourceType;
        private String status;
        private long lastSyncTimestamp;
        private Instant lastSyncAt;
        private int recordsSynced;
        private String errorMessage;
    }
}
