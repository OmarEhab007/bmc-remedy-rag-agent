package com.bmc.rag.api.controller;

import com.bmc.rag.api.dto.IngestionRequest;
import com.bmc.rag.api.dto.IngestionStatus;
import com.bmc.rag.store.entity.SyncStateEntity;
import com.bmc.rag.store.repository.SyncStateRepository;
import com.bmc.rag.store.service.VectorStoreService;
import com.bmc.rag.store.sync.IncrementalSyncService;
import com.bmc.rag.store.sync.IncrementalSyncService.SyncResult;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * REST API controller for ingestion/sync operations.
 * Admin endpoints for managing data synchronization.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/ingestion")
public class IngestionController {

    private final IncrementalSyncService syncService;
    private final SyncStateRepository syncStateRepository;
    private final VectorStoreService vectorStoreService;

    // Dedicated executor for sync operations to avoid ForkJoinPool exhaustion
    private final ExecutorService syncExecutor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "sync-executor");
        t.setDaemon(true);
        return t;
    });

    // Track running sync operations
    private final Map<String, CompletableFuture<SyncResult>> runningSyncs = new ConcurrentHashMap<>();

    public IngestionController(IncrementalSyncService syncService,
                               SyncStateRepository syncStateRepository,
                               VectorStoreService vectorStoreService) {
        this.syncService = syncService;
        this.syncStateRepository = syncStateRepository;
        this.vectorStoreService = vectorStoreService;
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down sync executor...");
        syncExecutor.shutdown();
        try {
            if (!syncExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                syncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            syncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Trigger a sync operation.
     *
     * @param request Ingestion request specifying what to sync
     * @return Status of the triggered sync
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerSync(@RequestBody(required = false) IngestionRequest request) {
        log.info("Sync trigger request received: {}", request);

        if (request == null) {
            request = new IngestionRequest();
        }

        String sourceType = request.getSourceType();
        boolean fullSync = request.isFullSync();

        Map<String, Object> response = new HashMap<>();

        if (sourceType != null) {
            // Sync specific source type
            if (!isValidSourceType(sourceType)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid source type: " + sourceType,
                    "validTypes", List.of("Incident", "WorkOrder", "KnowledgeArticle", "ChangeRequest")
                ));
            }

            log.info("Triggering {} sync for {}", fullSync ? "FULL" : "incremental", sourceType);

            // Run sync asynchronously with proper error handling and cleanup
            final String syncSourceType = sourceType;
            CompletableFuture<SyncResult> future = CompletableFuture.supplyAsync(() -> {
                if (fullSync) {
                    return syncService.forceFullSync(syncSourceType);
                } else {
                    return triggerSourceSync(syncSourceType);
                }
            }, syncExecutor).whenComplete((result, error) -> {
                // Always clean up the running sync entry
                runningSyncs.remove(syncSourceType);
                if (error != null) {
                    log.error("Async sync failed for {}: {}", syncSourceType, error.getMessage(), error);
                } else {
                    log.info("Async sync completed for {}: {} records synced",
                        syncSourceType, result != null ? result.recordsProcessed() : 0);
                }
            });

            runningSyncs.put(sourceType, future);

            response.put("status", "triggered");
            response.put("sourceType", sourceType);
            response.put("syncType", fullSync ? "full" : "incremental");

        } else {
            // Sync all source types
            log.info("Triggering {} sync for ALL source types", fullSync ? "FULL" : "incremental");

            for (String type : List.of("Incident", "WorkOrder", "KnowledgeArticle", "ChangeRequest")) {
                final String finalType = type;
                final boolean finalFullSync = fullSync;
                CompletableFuture<SyncResult> future = CompletableFuture.supplyAsync(() -> {
                    if (finalFullSync) {
                        return syncService.forceFullSync(finalType);
                    } else {
                        return triggerSourceSync(finalType);
                    }
                }, syncExecutor).whenComplete((result, error) -> {
                    // Always clean up the running sync entry
                    runningSyncs.remove(finalType);
                    if (error != null) {
                        log.error("Async sync failed for {}: {}", finalType, error.getMessage(), error);
                    } else {
                        log.info("Async sync completed for {}: {} records synced",
                            finalType, result != null ? result.recordsProcessed() : 0);
                    }
                });
                runningSyncs.put(type, future);
            }

            response.put("status", "triggered");
            response.put("sourceTypes", List.of("Incident", "WorkOrder", "KnowledgeArticle", "ChangeRequest"));
            response.put("syncType", fullSync ? "full" : "incremental");
        }

        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.accepted().body(response);
    }

    /**
     * Get current sync status.
     *
     * @return Current status of all source types
     */
    @GetMapping("/status")
    public ResponseEntity<IngestionStatus> getStatus() {
        log.debug("Status request received");

        Map<String, IngestionStatus.SourceStatus> sourceStatuses = new HashMap<>();

        for (SyncStateEntity state : syncStateRepository.findAll()) {
            sourceStatuses.put(state.getSourceType(), IngestionStatus.SourceStatus.builder()
                .sourceType(state.getSourceType())
                .status(state.getStatus())
                .lastSyncTimestamp(state.getLastSyncTimestamp())
                .lastSyncAt(state.getLastSyncAt())
                .recordsSynced(state.getRecordsSynced())
                .errorMessage(state.getErrorMessage())
                .build());
        }

        // Get statistics
        Map<String, Long> stats = vectorStoreService.getStatistics();

        // Determine overall status
        String overallStatus = "idle";
        if (syncStateRepository.isAnySyncRunning()) {
            overallStatus = "running";
        } else if (sourceStatuses.values().stream().anyMatch(s -> "failed".equals(s.getStatus()))) {
            overallStatus = "error";
        }

        IngestionStatus status = IngestionStatus.builder()
            .status(overallStatus)
            .sources(sourceStatuses)
            .statistics(stats)
            .build();

        return ResponseEntity.ok(status);
    }

    /**
     * Get statistics about stored embeddings.
     *
     * @return Embedding statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getStatistics() {
        return ResponseEntity.ok(vectorStoreService.getStatistics());
    }

    /**
     * Clear all embeddings for a source type.
     *
     * @param sourceType The source type to clear
     * @return Confirmation
     */
    @DeleteMapping("/embeddings/{sourceType}")
    public ResponseEntity<Map<String, Object>> clearEmbeddings(@PathVariable String sourceType) {
        log.warn("Clear embeddings request for: {}", sourceType);

        if (!isValidSourceType(sourceType)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid source type: " + sourceType
            ));
        }

        long countBefore = vectorStoreService.getStatistics().getOrDefault(
            sourceType.toLowerCase() + "s", 0L);

        vectorStoreService.deleteBySourceType(sourceType);

        // Reset sync state
        syncStateRepository.updateSyncCompleted(sourceType, 0L, 0);

        return ResponseEntity.ok(Map.of(
            "status", "cleared",
            "sourceType", sourceType,
            "chunksDeleted", countBefore
        ));
    }

    /**
     * Trigger sync for a specific source type.
     */
    private SyncResult triggerSourceSync(String sourceType) {
        return switch (sourceType) {
            case "Incident" -> syncService.syncIncidents();
            case "WorkOrder" -> syncService.syncWorkOrders();
            case "KnowledgeArticle" -> syncService.syncKnowledgeArticles();
            case "ChangeRequest" -> syncService.syncChangeRequests();
            default -> throw new IllegalArgumentException("Unknown source type: " + sourceType);
        };
    }

    /**
     * Validate source type.
     */
    private boolean isValidSourceType(String sourceType) {
        return Set.of("Incident", "WorkOrder", "KnowledgeArticle", "ChangeRequest")
            .contains(sourceType);
    }
}
