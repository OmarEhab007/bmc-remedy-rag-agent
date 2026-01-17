package com.bmc.rag.connector.service;

import com.bmc.rag.connector.config.RemedyConnectionConfig;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.connector.extractor.*;
import com.bmc.rag.connector.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Orchestrates the extraction of ITSM data from BMC Remedy.
 * Provides batch processing, circuit breaker pattern, and progress reporting.
 *
 * This is the main entry point for data extraction from Remedy.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractionOrchestrator {

    private final RemedyConnectionConfig config;
    private final ThreadLocalARContext arContext;

    // Extractors
    private final IncidentExtractor incidentExtractor;
    private final WorkOrderExtractor workOrderExtractor;
    private final KnowledgeExtractor knowledgeExtractor;
    private final ChangeRequestExtractor changeRequestExtractor;
    private final WorkLogExtractor workLogExtractor;
    private final AttachmentExtractor attachmentExtractor;

    // Circuit breaker state
    private final AtomicBoolean circuitOpen = new AtomicBoolean(false);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private static final int CIRCUIT_BREAKER_THRESHOLD = 5;
    private static final Duration CIRCUIT_RESET_DELAY = Duration.ofMinutes(5);
    private volatile Instant circuitOpenTime;

    // Extraction state
    private final AtomicBoolean extractionInProgress = new AtomicBoolean(false);

    /**
     * Check if Remedy connection is enabled.
     */
    public boolean isEnabled() {
        return config.isEnabled();
    }

    /**
     * Check if extraction is currently in progress.
     */
    public boolean isExtractionInProgress() {
        return extractionInProgress.get();
    }

    /**
     * Run full extraction of all source types.
     *
     * @param progressCallback Optional callback for progress updates
     * @return Extraction result summary
     */
    public ExtractionResult extractAll(Consumer<ExtractionProgress> progressCallback) {
        if (!config.isEnabled()) {
            return ExtractionResult.disabled();
        }

        if (!extractionInProgress.compareAndSet(false, true)) {
            return ExtractionResult.alreadyRunning();
        }

        if (isCircuitOpen()) {
            extractionInProgress.set(false);
            return ExtractionResult.circuitOpen();
        }

        ExtractionResult.ExtractionResultBuilder result = ExtractionResult.builder();
        Instant startTime = Instant.now();

        try {
            // Extract each source type
            result.incidents(extractIncidents(progressCallback));
            result.workOrders(extractWorkOrders(progressCallback));
            result.knowledgeArticles(extractKnowledgeArticles(progressCallback));
            result.changeRequests(extractChangeRequests(progressCallback));

            result.success(true);
            resetCircuitBreaker();

        } catch (Exception e) {
            log.error("Extraction failed: {}", e.getMessage(), e);
            result.success(false);
            result.errorMessage(e.getMessage());
            recordFailure();
        } finally {
            extractionInProgress.set(false);
            result.duration(Duration.between(startTime, Instant.now()));
        }

        return result.build();
    }

    /**
     * Extract incidents with work logs.
     */
    public SourceExtractionResult extractIncidents(Consumer<ExtractionProgress> progressCallback) {
        return extractWithProgress("Incident", () -> {
            List<IncidentRecord> incidents = incidentExtractor.extractWithQualification(null);

            // Batch fetch work logs
            if (!incidents.isEmpty()) {
                List<String> incidentNumbers = incidents.stream()
                    .map(IncidentRecord::getIncidentNumber)
                    .toList();

                Map<String, List<WorkLogEntry>> workLogs =
                    workLogExtractor.batchExtractIncidentWorkLogs(incidentNumbers);

                for (IncidentRecord incident : incidents) {
                    incident.setWorkLogs(workLogs.get(incident.getIncidentNumber()));
                }
            }

            return new SourceExtractionResult("Incident", incidents.size(), null);
        }, progressCallback);
    }

    /**
     * Extract work orders.
     */
    public SourceExtractionResult extractWorkOrders(Consumer<ExtractionProgress> progressCallback) {
        return extractWithProgress("WorkOrder", () -> {
            List<WorkOrderRecord> workOrders = workOrderExtractor.extractWithQualification(null);

            // Batch fetch work logs
            if (!workOrders.isEmpty()) {
                List<String> workOrderIds = workOrders.stream()
                    .map(WorkOrderRecord::getWorkOrderId)
                    .toList();

                Map<String, List<WorkLogEntry>> workLogs =
                    workLogExtractor.batchExtractWorkOrderWorkLogs(workOrderIds);

                for (WorkOrderRecord wo : workOrders) {
                    wo.setWorkLogs(workLogs.get(wo.getWorkOrderId()));
                }
            }

            return new SourceExtractionResult("WorkOrder", workOrders.size(), null);
        }, progressCallback);
    }

    /**
     * Extract knowledge articles.
     */
    public SourceExtractionResult extractKnowledgeArticles(Consumer<ExtractionProgress> progressCallback) {
        return extractWithProgress("KnowledgeArticle", () -> {
            List<KnowledgeArticle> articles = knowledgeExtractor.extractPublishedArticles(0L);
            return new SourceExtractionResult("KnowledgeArticle", articles.size(), null);
        }, progressCallback);
    }

    /**
     * Extract change requests.
     */
    public SourceExtractionResult extractChangeRequests(Consumer<ExtractionProgress> progressCallback) {
        return extractWithProgress("ChangeRequest", () -> {
            List<ChangeRequestRecord> changes = changeRequestExtractor.extractWithQualification(null);

            // Batch fetch work logs
            if (!changes.isEmpty()) {
                List<String> changeIds = changes.stream()
                    .map(ChangeRequestRecord::getChangeId)
                    .toList();

                Map<String, List<WorkLogEntry>> workLogs =
                    workLogExtractor.batchExtractChangeWorkLogs(changeIds);

                for (ChangeRequestRecord cr : changes) {
                    cr.setWorkLogs(workLogs.get(cr.getChangeId()));
                }
            }

            return new SourceExtractionResult("ChangeRequest", changes.size(), null);
        }, progressCallback);
    }

    /**
     * Extract records modified since a timestamp.
     */
    public ExtractionResult extractModifiedSince(long sinceTimestamp,
                                                  Consumer<ExtractionProgress> progressCallback) {
        if (!config.isEnabled()) {
            return ExtractionResult.disabled();
        }

        if (!extractionInProgress.compareAndSet(false, true)) {
            return ExtractionResult.alreadyRunning();
        }

        ExtractionResult.ExtractionResultBuilder result = ExtractionResult.builder();
        Instant startTime = Instant.now();

        try {
            // Extract modified incidents
            List<IncidentRecord> incidents = incidentExtractor.extractModifiedSince(sinceTimestamp);
            result.incidents(new SourceExtractionResult("Incident", incidents.size(), null));

            // Extract modified work orders
            List<WorkOrderRecord> workOrders = workOrderExtractor.extractModifiedSince(sinceTimestamp);
            result.workOrders(new SourceExtractionResult("WorkOrder", workOrders.size(), null));

            // Extract modified knowledge articles
            List<KnowledgeArticle> articles = knowledgeExtractor.extractPublishedArticles(sinceTimestamp);
            result.knowledgeArticles(new SourceExtractionResult("KnowledgeArticle", articles.size(), null));

            // Extract modified change requests
            List<ChangeRequestRecord> changes = changeRequestExtractor.extractModifiedSince(sinceTimestamp);
            result.changeRequests(new SourceExtractionResult("ChangeRequest", changes.size(), null));

            result.success(true);
            resetCircuitBreaker();

        } catch (Exception e) {
            log.error("Incremental extraction failed: {}", e.getMessage(), e);
            result.success(false);
            result.errorMessage(e.getMessage());
            recordFailure();
        } finally {
            extractionInProgress.set(false);
            result.duration(Duration.between(startTime, Instant.now()));
        }

        return result.build();
    }

    /**
     * Extract with progress reporting and error handling.
     */
    private SourceExtractionResult extractWithProgress(
            String sourceType,
            ExtractionTask task,
            Consumer<ExtractionProgress> progressCallback) {

        if (progressCallback != null) {
            progressCallback.accept(new ExtractionProgress(sourceType, "STARTED", 0, 0));
        }

        try {
            SourceExtractionResult result = task.execute();

            if (progressCallback != null) {
                progressCallback.accept(new ExtractionProgress(
                    sourceType, "COMPLETED", result.recordCount(), result.recordCount()));
            }

            return result;

        } catch (Exception e) {
            log.error("Failed to extract {}: {}", sourceType, e.getMessage(), e);

            if (progressCallback != null) {
                progressCallback.accept(new ExtractionProgress(sourceType, "FAILED", 0, 0));
            }

            return new SourceExtractionResult(sourceType, 0, e.getMessage());
        }
    }

    // Circuit breaker methods

    private boolean isCircuitOpen() {
        if (!circuitOpen.get()) {
            return false;
        }

        // Check if enough time has passed to try again
        if (circuitOpenTime != null &&
            Duration.between(circuitOpenTime, Instant.now()).compareTo(CIRCUIT_RESET_DELAY) > 0) {
            log.info("Circuit breaker reset delay elapsed, attempting recovery");
            return false;
        }

        return true;
    }

    private void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= CIRCUIT_BREAKER_THRESHOLD) {
            circuitOpen.set(true);
            circuitOpenTime = Instant.now();
            log.error("Circuit breaker OPEN after {} consecutive failures", failures);
        }
    }

    private void resetCircuitBreaker() {
        if (consecutiveFailures.get() > 0) {
            consecutiveFailures.set(0);
            circuitOpen.set(false);
            circuitOpenTime = null;
            log.info("Circuit breaker reset after successful extraction");
        }
    }

    // Inner types

    @FunctionalInterface
    private interface ExtractionTask {
        SourceExtractionResult execute();
    }

    /**
     * Progress update for extraction.
     */
    public record ExtractionProgress(
        String sourceType,
        String status,
        int current,
        int total
    ) {}

    /**
     * Result for a single source type extraction.
     */
    public record SourceExtractionResult(
        String sourceType,
        int recordCount,
        String errorMessage
    ) {
        public boolean isSuccess() {
            return errorMessage == null;
        }
    }

    /**
     * Complete extraction result.
     */
    @lombok.Data
    @lombok.Builder
    public static class ExtractionResult {
        private boolean success;
        private String errorMessage;
        private Duration duration;
        private SourceExtractionResult incidents;
        private SourceExtractionResult workOrders;
        private SourceExtractionResult knowledgeArticles;
        private SourceExtractionResult changeRequests;

        public int getTotalRecords() {
            int total = 0;
            if (incidents != null) total += incidents.recordCount();
            if (workOrders != null) total += workOrders.recordCount();
            if (knowledgeArticles != null) total += knowledgeArticles.recordCount();
            if (changeRequests != null) total += changeRequests.recordCount();
            return total;
        }

        public static ExtractionResult disabled() {
            return ExtractionResult.builder()
                .success(false)
                .errorMessage("Remedy connection is disabled")
                .build();
        }

        public static ExtractionResult alreadyRunning() {
            return ExtractionResult.builder()
                .success(false)
                .errorMessage("Extraction already in progress")
                .build();
        }

        public static ExtractionResult circuitOpen() {
            return ExtractionResult.builder()
                .success(false)
                .errorMessage("Circuit breaker is open - too many consecutive failures")
                .build();
        }
    }
}
