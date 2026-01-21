package com.bmc.rag.agent.damee;

import com.bmc.rag.store.service.VectorStoreService;
import com.bmc.rag.vectorization.chunking.TextChunk;
import com.bmc.rag.vectorization.chunking.TextChunk.ChunkType;
import com.bmc.rag.vectorization.embedding.LocalEmbeddingService;
import com.bmc.rag.vectorization.embedding.LocalEmbeddingService.EmbeddedChunk;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for ingesting Damee service catalog into the vector store.
 * Creates embeddings for each service to enable semantic search.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DameeIngestionService {

    private final DameeServiceCatalog serviceCatalog;
    private final VectorStoreService vectorStoreService;
    private final LocalEmbeddingService embeddingService;

    private static final String SOURCE_TYPE = "DAMEE_SERVICE";

    private final AtomicBoolean ingestionComplete = new AtomicBoolean(false);

    /**
     * Ingest all Damee services into the vector store on application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void ingestOnStartup() {
        if (ingestionComplete.get()) {
            log.debug("Damee services already ingested, skipping");
            return;
        }

        try {
            ingestAllServices();
            ingestionComplete.set(true);
        } catch (Exception e) {
            log.error("Failed to ingest Damee services: {}", e.getMessage(), e);
        }
    }

    /**
     * Ingest all services from the catalog into the vector store.
     */
    public void ingestAllServices() {
        log.info("Starting Damee service catalog ingestion...");

        List<DameeService> services = serviceCatalog.getAllServices();
        if (services.isEmpty()) {
            log.warn("No Damee services to ingest");
            return;
        }

        List<TextChunk> chunks = new ArrayList<>();

        for (DameeService service : services) {
            chunks.addAll(createChunksForService(service));
        }

        if (!chunks.isEmpty()) {
            log.info("Embedding {} chunks for {} Damee services", chunks.size(), services.size());

            // Embed all chunks
            List<EmbeddedChunk> embeddedChunks = embeddingService.embedChunks(chunks);

            log.info("Storing {} embedded chunks in vector store", embeddedChunks.size());
            vectorStoreService.storeBatch(embeddedChunks);

            log.info("Damee service catalog ingestion complete");
        }
    }

    /**
     * Create text chunks for a single service.
     */
    private List<TextChunk> createChunksForService(DameeService service) {
        List<TextChunk> chunks = new ArrayList<>();

        // Main service info chunk
        chunks.add(createServiceInfoChunk(service));

        // Workflow chunk (if exists)
        if (service.getWorkflow() != null && !service.getWorkflow().isEmpty()) {
            chunks.add(createWorkflowChunk(service));
        }

        // Keywords chunk for intent matching
        chunks.add(createKeywordsChunk(service));

        return chunks;
    }

    /**
     * Create the main service information chunk.
     */
    private TextChunk createServiceInfoChunk(DameeService service) {
        StringBuilder content = new StringBuilder();

        // English content
        content.append("Service: ").append(service.getNameEn()).append("\n");
        content.append("Service ID: ").append(service.getServiceId()).append("\n");

        if (service.getDescriptionEn() != null) {
            content.append("Description: ").append(service.getDescriptionEn()).append("\n");
        }

        if (service.getCategory() != null) {
            content.append("Category: ").append(service.getCategory()).append("\n");
        }

        if (service.getSubcategory() != null) {
            content.append("Subcategory: ").append(service.getSubcategory()).append("\n");
        }

        // Arabic content for bilingual search
        content.append("\n");
        if (service.getNameAr() != null) {
            content.append("الخدمة: ").append(service.getNameAr()).append("\n");
        }
        if (service.getDescriptionAr() != null) {
            content.append("الوصف: ").append(service.getDescriptionAr()).append("\n");
        }

        // URL for direct access
        if (service.getUrl() != null) {
            content.append("\nURL: ").append(service.getUrl()).append("\n");
        }

        // Approval requirements
        content.append("\nApproval Requirements:\n");
        content.append("- Manager Approval: ").append(service.isRequiresManagerApproval() ? "Yes" : "No").append("\n");
        content.append("- VIP Bypass Available: ").append(service.isVipBypass() ? "Yes" : "No").append("\n");

        Map<String, String> metadata = createBaseMetadata(service, ChunkType.DAMEE_SERVICE);

        return TextChunk.builder()
                .chunkId(TextChunk.generateChunkId(SOURCE_TYPE, service.getServiceId(), ChunkType.DAMEE_SERVICE, 0))
                .content(content.toString())
                .chunkType(ChunkType.DAMEE_SERVICE)
                .sourceType(SOURCE_TYPE)
                .sourceId(service.getServiceId())
                .entryId(service.getServiceId())
                .metadata(metadata)
                .sequenceNumber(0)
                .build();
    }

    /**
     * Create workflow chunk for a service.
     */
    private TextChunk createWorkflowChunk(DameeService service) {
        StringBuilder content = new StringBuilder();

        content.append("Workflow for: ").append(service.getNameEn()).append("\n");
        content.append("Service ID: ").append(service.getServiceId()).append("\n\n");
        content.append("Workflow Steps:\n");

        for (DameeService.WorkflowStep step : service.getWorkflow()) {
            content.append(step.getOrder()).append(". ").append(step.getDescription());
            if (step.getTeam() != null) {
                content.append(" (").append(step.getTeam()).append(")");
            }
            if (step.isRequiresApproval()) {
                content.append(" [Requires Approval]");
            }
            content.append("\n");
        }

        Map<String, String> metadata = createBaseMetadata(service, ChunkType.DAMEE_WORKFLOW);

        return TextChunk.builder()
                .chunkId(TextChunk.generateChunkId(SOURCE_TYPE, service.getServiceId(), ChunkType.DAMEE_WORKFLOW, 0))
                .content(content.toString())
                .chunkType(ChunkType.DAMEE_WORKFLOW)
                .sourceType(SOURCE_TYPE)
                .sourceId(service.getServiceId())
                .entryId(service.getServiceId())
                .metadata(metadata)
                .sequenceNumber(1)
                .build();
    }

    /**
     * Create keywords chunk for intent matching.
     */
    private TextChunk createKeywordsChunk(DameeService service) {
        StringBuilder content = new StringBuilder();

        content.append("Service Keywords for: ").append(service.getNameEn()).append("\n");
        content.append("Service ID: ").append(service.getServiceId()).append("\n\n");

        // Add all keywords
        content.append("Keywords: ");
        if (service.getKeywords() != null && !service.getKeywords().isEmpty()) {
            content.append(String.join(", ", service.getKeywords()));
        }
        content.append("\n");

        // Add common query patterns
        content.append("\nCommon requests:\n");
        content.append("- I need ").append(service.getNameEn().toLowerCase()).append("\n");
        content.append("- Request for ").append(service.getNameEn().toLowerCase()).append("\n");
        content.append("- How to get ").append(service.getNameEn().toLowerCase()).append("\n");

        // Arabic patterns
        if (service.getNameAr() != null) {
            content.append("- أريد ").append(service.getNameAr()).append("\n");
            content.append("- طلب ").append(service.getNameAr()).append("\n");
        }

        Map<String, String> metadata = createBaseMetadata(service, ChunkType.DAMEE_KEYWORDS);

        return TextChunk.builder()
                .chunkId(TextChunk.generateChunkId(SOURCE_TYPE, service.getServiceId(), ChunkType.DAMEE_KEYWORDS, 0))
                .content(content.toString())
                .chunkType(ChunkType.DAMEE_KEYWORDS)
                .sourceType(SOURCE_TYPE)
                .sourceId(service.getServiceId())
                .entryId(service.getServiceId())
                .metadata(metadata)
                .sequenceNumber(2)
                .build();
    }

    /**
     * Create base metadata for a chunk.
     */
    private Map<String, String> createBaseMetadata(DameeService service, ChunkType chunkType) {
        Map<String, String> metadata = new HashMap<>();

        metadata.put("source_type", SOURCE_TYPE);
        metadata.put("chunk_type", chunkType.name());
        metadata.put("service_id", service.getServiceId());
        metadata.put("service_name_en", service.getNameEn());

        if (service.getNameAr() != null) {
            metadata.put("service_name_ar", service.getNameAr());
        }

        if (service.getCategory() != null) {
            metadata.put("category", service.getCategory());
        }

        if (service.getSubcategory() != null) {
            metadata.put("subcategory", service.getSubcategory());
        }

        if (service.getUrl() != null) {
            metadata.put("url", service.getUrl());
        }

        metadata.put("requires_approval", String.valueOf(service.isRequiresManagerApproval()));
        metadata.put("vip_bypass", String.valueOf(service.isVipBypass()));

        return metadata;
    }

    /**
     * Re-ingest all services (for manual refresh).
     */
    public void refreshCatalog() {
        log.info("Refreshing Damee service catalog...");

        // Remove existing Damee service embeddings
        vectorStoreService.deleteBySourceType(SOURCE_TYPE);

        // Re-ingest
        ingestionComplete.set(false);
        ingestAllServices();
        ingestionComplete.set(true);
    }

    /**
     * Check if ingestion is complete.
     */
    public boolean isIngestionComplete() {
        return ingestionComplete.get();
    }

    /**
     * Get ingestion status.
     */
    public String getIngestionStatus() {
        if (ingestionComplete.get()) {
            return String.format("Ingestion complete. %d services indexed.",
                    serviceCatalog.getServiceCount());
        } else {
            return "Ingestion in progress or not started.";
        }
    }
}
