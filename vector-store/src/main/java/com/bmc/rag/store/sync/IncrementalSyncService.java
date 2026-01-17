package com.bmc.rag.store.sync;

import com.bmc.rag.connector.config.RemedyConnectionConfig;
import com.bmc.rag.connector.extractor.*;
import com.bmc.rag.connector.model.*;
import com.bmc.rag.store.repository.SyncStateRepository;
import com.bmc.rag.store.service.VectorStoreService;
import com.bmc.rag.vectorization.chunking.*;
import com.bmc.rag.vectorization.embedding.LocalEmbeddingService;
import com.bmc.rag.vectorization.embedding.LocalEmbeddingService.EmbeddedChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for incremental synchronization of ITSM data.
 * Implements CDC (Change Data Capture) pattern using Last Modified Date field.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncrementalSyncService {

    private final RemedyConnectionConfig remedyConfig;
    private final SyncStateRepository syncStateRepository;
    private final VectorStoreService vectorStoreService;
    private final LocalEmbeddingService embeddingService;

    // Extractors
    private final IncidentExtractor incidentExtractor;
    private final WorkOrderExtractor workOrderExtractor;
    private final KnowledgeExtractor knowledgeExtractor;
    private final ChangeRequestExtractor changeRequestExtractor;
    private final WorkLogExtractor workLogExtractor;

    // Chunk strategies
    private final IncidentChunkStrategy incidentChunkStrategy;
    private final WorkOrderChunkStrategy workOrderChunkStrategy;
    private final KnowledgeChunkStrategy knowledgeChunkStrategy;
    private final ChangeRequestChunkStrategy changeRequestChunkStrategy;

    /**
     * Check if Remedy connection is enabled.
     */
    public boolean isRemedyEnabled() {
        return remedyConfig.isEnabled();
    }

    /**
     * Run incremental sync for all source types.
     * Scheduled to run every 15 minutes by default.
     */
    @Scheduled(fixedRateString = "${sync.interval:900000}")
    public void runIncrementalSync() {
        if (!remedyConfig.isEnabled()) {
            log.debug("Remedy connection disabled, skipping incremental sync");
            return;
        }

        log.info("Starting incremental sync for all source types");

        // Check if any sync is already running
        if (syncStateRepository.isAnySyncRunning()) {
            log.warn("Sync already in progress, skipping this run");
            return;
        }

        try {
            syncIncidents();
            syncWorkOrders();
            syncKnowledgeArticles();
            syncChangeRequests();

            log.info("Incremental sync completed for all source types");
        } catch (Exception e) {
            log.error("Incremental sync failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Sync incidents incrementally.
     */
    @Transactional
    public SyncResult syncIncidents() {
        if (!remedyConfig.isEnabled()) {
            return new SyncResult(0, 0, "Remedy connection disabled");
        }
        return syncSourceType("Incident", () -> {
            long lastSync = getLastSyncTimestamp("Incident");
            log.info("Syncing incidents modified since: {}", Instant.ofEpochSecond(lastSync));

            List<IncidentRecord> incidents = incidentExtractor.extractModifiedSince(lastSync);
            log.info("Found {} modified incidents", incidents.size());

            if (incidents.isEmpty()) {
                return new SyncResult(0, 0);
            }

            // Fetch work logs for all incidents
            List<String> incidentNumbers = incidents.stream()
                .map(IncidentRecord::getIncidentNumber)
                .collect(Collectors.toList());

            Map<String, List<WorkLogEntry>> workLogMap =
                workLogExtractor.batchExtractIncidentWorkLogs(incidentNumbers);

            // Attach work logs to incidents
            for (IncidentRecord incident : incidents) {
                List<WorkLogEntry> workLogs = workLogMap.get(incident.getIncidentNumber());
                if (workLogs != null) {
                    incident.setWorkLogs(workLogs);
                }
            }

            int chunksCreated = 0;
            long maxTimestamp = lastSync;

            for (IncidentRecord incident : incidents) {
                // Delete existing chunks for this incident
                vectorStoreService.deleteBySourceRecord("Incident", incident.getIncidentNumber());

                // Create new chunks
                List<TextChunk> chunks = incidentChunkStrategy.chunk(incident);

                // Embed and store
                List<EmbeddedChunk> embedded = embeddingService.embedChunks(chunks);
                vectorStoreService.storeBatch(embedded);

                chunksCreated += chunks.size();

                // Track latest timestamp
                if (incident.getLastModifiedDate() != null) {
                    maxTimestamp = Math.max(maxTimestamp, incident.getLastModifiedDate().getEpochSecond());
                }
            }

            // Update sync state
            updateSyncTimestamp("Incident", maxTimestamp, incidents.size());

            return new SyncResult(incidents.size(), chunksCreated);
        });
    }

    /**
     * Sync work orders incrementally.
     */
    @Transactional
    public SyncResult syncWorkOrders() {
        if (!remedyConfig.isEnabled()) {
            return new SyncResult(0, 0, "Remedy connection disabled");
        }
        return syncSourceType("WorkOrder", () -> {
            long lastSync = getLastSyncTimestamp("WorkOrder");
            log.info("Syncing work orders modified since: {}", Instant.ofEpochSecond(lastSync));

            List<WorkOrderRecord> workOrders = workOrderExtractor.extractModifiedSince(lastSync);
            log.info("Found {} modified work orders", workOrders.size());

            if (workOrders.isEmpty()) {
                return new SyncResult(0, 0);
            }

            // Fetch work logs
            List<String> workOrderIds = workOrders.stream()
                .map(WorkOrderRecord::getWorkOrderId)
                .collect(Collectors.toList());

            Map<String, List<WorkLogEntry>> workLogMap =
                workLogExtractor.batchExtractWorkOrderWorkLogs(workOrderIds);

            for (WorkOrderRecord workOrder : workOrders) {
                List<WorkLogEntry> workLogs = workLogMap.get(workOrder.getWorkOrderId());
                if (workLogs != null) {
                    workOrder.setWorkLogs(workLogs);
                }
            }

            int chunksCreated = 0;
            long maxTimestamp = lastSync;

            for (WorkOrderRecord workOrder : workOrders) {
                vectorStoreService.deleteBySourceRecord("WorkOrder", workOrder.getWorkOrderId());

                List<TextChunk> chunks = workOrderChunkStrategy.chunk(workOrder);
                List<EmbeddedChunk> embedded = embeddingService.embedChunks(chunks);
                vectorStoreService.storeBatch(embedded);

                chunksCreated += chunks.size();

                if (workOrder.getLastModifiedDate() != null) {
                    maxTimestamp = Math.max(maxTimestamp, workOrder.getLastModifiedDate().getEpochSecond());
                }
            }

            updateSyncTimestamp("WorkOrder", maxTimestamp, workOrders.size());

            return new SyncResult(workOrders.size(), chunksCreated);
        });
    }

    /**
     * Sync knowledge articles incrementally.
     */
    @Transactional
    public SyncResult syncKnowledgeArticles() {
        if (!remedyConfig.isEnabled()) {
            return new SyncResult(0, 0, "Remedy connection disabled");
        }
        return syncSourceType("KnowledgeArticle", () -> {
            long lastSync = getLastSyncTimestamp("KnowledgeArticle");
            log.info("Syncing knowledge articles modified since: {}", Instant.ofEpochSecond(lastSync));

            // Only sync published articles
            List<KnowledgeArticle> articles = knowledgeExtractor.extractPublishedArticles(lastSync);
            log.info("Found {} modified knowledge articles", articles.size());

            if (articles.isEmpty()) {
                return new SyncResult(0, 0);
            }

            int chunksCreated = 0;
            long maxTimestamp = lastSync;

            for (KnowledgeArticle article : articles) {
                vectorStoreService.deleteBySourceRecord("KnowledgeArticle", article.getArticleId());

                List<TextChunk> chunks = knowledgeChunkStrategy.chunk(article);
                List<EmbeddedChunk> embedded = embeddingService.embedChunks(chunks);
                vectorStoreService.storeBatch(embedded);

                chunksCreated += chunks.size();

                if (article.getLastModifiedDate() != null) {
                    maxTimestamp = Math.max(maxTimestamp, article.getLastModifiedDate().getEpochSecond());
                }
            }

            updateSyncTimestamp("KnowledgeArticle", maxTimestamp, articles.size());

            return new SyncResult(articles.size(), chunksCreated);
        });
    }

    /**
     * Sync change requests incrementally.
     */
    @Transactional
    public SyncResult syncChangeRequests() {
        if (!remedyConfig.isEnabled()) {
            return new SyncResult(0, 0, "Remedy connection disabled");
        }
        return syncSourceType("ChangeRequest", () -> {
            long lastSync = getLastSyncTimestamp("ChangeRequest");
            log.info("Syncing change requests modified since: {}", Instant.ofEpochSecond(lastSync));

            List<ChangeRequestRecord> changes = changeRequestExtractor.extractModifiedSince(lastSync);
            log.info("Found {} modified change requests", changes.size());

            if (changes.isEmpty()) {
                return new SyncResult(0, 0);
            }

            // Fetch work logs
            List<String> changeIds = changes.stream()
                .map(ChangeRequestRecord::getChangeId)
                .collect(Collectors.toList());

            Map<String, List<WorkLogEntry>> workLogMap =
                workLogExtractor.batchExtractChangeWorkLogs(changeIds);

            for (ChangeRequestRecord change : changes) {
                List<WorkLogEntry> workLogs = workLogMap.get(change.getChangeId());
                if (workLogs != null) {
                    change.setWorkLogs(workLogs);
                }
            }

            int chunksCreated = 0;
            long maxTimestamp = lastSync;

            for (ChangeRequestRecord change : changes) {
                vectorStoreService.deleteBySourceRecord("ChangeRequest", change.getChangeId());

                List<TextChunk> chunks = changeRequestChunkStrategy.chunk(change);
                List<EmbeddedChunk> embedded = embeddingService.embedChunks(chunks);
                vectorStoreService.storeBatch(embedded);

                chunksCreated += chunks.size();

                if (change.getLastModifiedDate() != null) {
                    maxTimestamp = Math.max(maxTimestamp, change.getLastModifiedDate().getEpochSecond());
                }
            }

            updateSyncTimestamp("ChangeRequest", maxTimestamp, changes.size());

            return new SyncResult(changes.size(), chunksCreated);
        });
    }

    /**
     * Force full sync for a source type (deletes all existing data).
     */
    @Transactional
    public SyncResult forceFullSync(String sourceType) {
        if (!remedyConfig.isEnabled()) {
            return new SyncResult(0, 0, "Remedy connection disabled");
        }
        log.warn("Starting FULL sync for {} - this will delete all existing data", sourceType);

        // Reset sync timestamp to 0
        syncStateRepository.updateSyncCompleted(sourceType, 0L, 0);

        // Delete all existing embeddings
        vectorStoreService.deleteBySourceType(sourceType);

        // Run incremental sync (which will now fetch all records)
        return switch (sourceType) {
            case "Incident" -> syncIncidents();
            case "WorkOrder" -> syncWorkOrders();
            case "KnowledgeArticle" -> syncKnowledgeArticles();
            case "ChangeRequest" -> syncChangeRequests();
            default -> throw new IllegalArgumentException("Unknown source type: " + sourceType);
        };
    }

    /**
     * Get the last sync timestamp for a source type.
     */
    private long getLastSyncTimestamp(String sourceType) {
        return syncStateRepository.getLastSyncTimestamp(sourceType).orElse(0L);
    }

    /**
     * Update sync timestamp after successful sync.
     */
    private void updateSyncTimestamp(String sourceType, long timestamp, int recordCount) {
        syncStateRepository.updateSyncCompleted(sourceType, timestamp, recordCount);
        log.info("Updated sync state for {}: timestamp={}, records={}", sourceType, timestamp, recordCount);
    }

    // Lock timeout in minutes - after this time, a lock is considered stale
    private static final int LOCK_TIMEOUT_MINUTES = 60;

    /**
     * Execute sync operation with proper error handling, state management, and atomic locking.
     * Uses database-level locking to prevent race conditions.
     * Includes stale lock detection and recovery.
     */
    private SyncResult syncSourceType(String sourceType, SyncOperation operation) {
        // Check for and release stale locks first (e.g., from crashed processes)
        if (syncStateRepository.hasStalelock(sourceType, LOCK_TIMEOUT_MINUTES)) {
            log.warn("Detected stale lock for {} (older than {} minutes), releasing it",
                sourceType, LOCK_TIMEOUT_MINUTES);
            syncStateRepository.releaseStaleLocksNative(LOCK_TIMEOUT_MINUTES);
        }

        // Attempt to acquire lock atomically - prevents race conditions
        int lockAcquired = syncStateRepository.tryAcquireLock(sourceType);

        if (lockAcquired == 0) {
            log.warn("Failed to acquire sync lock for {} - another sync may be running", sourceType);
            return new SyncResult(0, 0, "Sync already in progress for " + sourceType);
        }

        try {
            log.info("Lock acquired for sync: {}", sourceType);

            SyncResult result = operation.execute();

            log.info("Sync completed for {}: {} records, {} chunks",
                sourceType, result.recordsProcessed(), result.chunksCreated());

            return result;

        } catch (Exception e) {
            log.error("Sync failed for {}: {}", sourceType, e.getMessage(), e);
            syncStateRepository.markSyncFailed(sourceType, e.getMessage());
            return new SyncResult(0, 0, e.getMessage());
        } finally {
            // Always release the lock
            syncStateRepository.releaseLock(sourceType);
            log.debug("Released sync lock for {}", sourceType);
        }
    }

    /**
     * Functional interface for sync operations.
     */
    @FunctionalInterface
    private interface SyncOperation {
        SyncResult execute();
    }

    /**
     * Sync result record.
     */
    public record SyncResult(int recordsProcessed, int chunksCreated, String errorMessage) {
        public SyncResult(int recordsProcessed, int chunksCreated) {
            this(recordsProcessed, chunksCreated, null);
        }

        public boolean isSuccess() {
            return errorMessage == null;
        }
    }
}
