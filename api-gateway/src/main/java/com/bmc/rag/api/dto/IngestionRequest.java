package com.bmc.rag.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ingestion request DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionRequest {

    /**
     * Source type to sync (Incident, WorkOrder, KnowledgeArticle, ChangeRequest).
     * If null, syncs all source types.
     */
    private String sourceType;

    /**
     * Whether to force a full sync (deletes existing data).
     */
    @Builder.Default
    private boolean fullSync = false;
}
