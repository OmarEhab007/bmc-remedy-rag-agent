package com.bmc.rag.api.controller;

import com.bmc.rag.agent.metrics.RagMetricsService;
import com.bmc.rag.agent.metrics.RagMetricsService.MetricsSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for RAG metrics endpoints.
 * Provides access to RAG-specific metrics for monitoring and dashboards.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final RagMetricsService metricsService;

    /**
     * Get a snapshot of all RAG metrics.
     */
    @GetMapping("/rag")
    public ResponseEntity<MetricsSnapshot> getRagMetrics() {
        log.debug("Fetching RAG metrics snapshot");
        return ResponseEntity.ok(metricsService.getSnapshot());
    }

    /**
     * Get a summary of key metrics for dashboard display.
     */
    @GetMapping("/rag/summary")
    public ResponseEntity<Map<String, Object>> getRagMetricsSummary() {
        MetricsSnapshot snapshot = metricsService.getSnapshot();

        Map<String, Object> summary = Map.of(
            "retrievals", Map.of(
                "total", snapshot.getTotalRetrievals(),
                "active", snapshot.getActiveRetrievals(),
                "lastCount", snapshot.getLastRetrievalCount()
            ),
            "latency", Map.of(
                "retrievalP50Ms", snapshot.getRetrievalP50Ms(),
                "retrievalP95Ms", snapshot.getRetrievalP95Ms(),
                "generationP50Ms", snapshot.getGenerationP50Ms(),
                "generationP95Ms", snapshot.getGenerationP95Ms(),
                "totalP50Ms", snapshot.getTotalP50Ms(),
                "totalP95Ms", snapshot.getTotalP95Ms()
            ),
            "quality", Map.of(
                "groundednessScore", snapshot.getAverageGroundednessScore(),
                "hallucinationsDetected", snapshot.getHallucinationsDetected(),
                "citations", snapshot.getTotalCitations()
            ),
            "cache", Map.of(
                "hitRate", snapshot.getCacheHitRate(),
                "hits", snapshot.getCacheHits(),
                "misses", snapshot.getCacheMisses()
            ),
            "errors", Map.of(
                "total", snapshot.getTotalErrors()
            )
        );

        return ResponseEntity.ok(summary);
    }

    /**
     * Get latency percentiles.
     */
    @GetMapping("/rag/latency")
    public ResponseEntity<Map<String, Object>> getLatencyMetrics() {
        MetricsSnapshot snapshot = metricsService.getSnapshot();

        Map<String, Object> latency = Map.of(
            "retrieval", Map.of(
                "p50", snapshot.getRetrievalP50Ms(),
                "p95", snapshot.getRetrievalP95Ms(),
                "p99", snapshot.getRetrievalP99Ms()
            ),
            "generation", Map.of(
                "p50", snapshot.getGenerationP50Ms(),
                "p95", snapshot.getGenerationP95Ms()
            ),
            "total", Map.of(
                "p50", snapshot.getTotalP50Ms(),
                "p95", snapshot.getTotalP95Ms()
            )
        );

        return ResponseEntity.ok(latency);
    }

    /**
     * Get quality metrics (groundedness, hallucinations).
     */
    @GetMapping("/rag/quality")
    public ResponseEntity<Map<String, Object>> getQualityMetrics() {
        MetricsSnapshot snapshot = metricsService.getSnapshot();

        Map<String, Object> quality = Map.of(
            "groundednessScore", snapshot.getAverageGroundednessScore(),
            "groundednessPercentage", Math.round(snapshot.getAverageGroundednessScore() * 100),
            "hallucinationsDetected", snapshot.getHallucinationsDetected(),
            "totalCitations", snapshot.getTotalCitations(),
            "avgCitationsPerResponse", snapshot.getTotalRetrievals() > 0 ?
                (double) snapshot.getTotalCitations() / snapshot.getTotalRetrievals() : 0
        );

        return ResponseEntity.ok(quality);
    }

    /**
     * Get cache performance metrics.
     */
    @GetMapping("/rag/cache")
    public ResponseEntity<Map<String, Object>> getCacheMetrics() {
        MetricsSnapshot snapshot = metricsService.getSnapshot();

        Map<String, Object> cache = Map.of(
            "hitRate", snapshot.getCacheHitRate(),
            "hitRatePercentage", Math.round(snapshot.getCacheHitRate() * 100),
            "hits", snapshot.getCacheHits(),
            "misses", snapshot.getCacheMisses(),
            "total", snapshot.getCacheHits() + snapshot.getCacheMisses()
        );

        return ResponseEntity.ok(cache);
    }
}
