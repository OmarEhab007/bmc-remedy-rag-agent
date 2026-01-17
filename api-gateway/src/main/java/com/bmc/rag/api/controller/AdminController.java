package com.bmc.rag.api.controller;

import com.bmc.rag.store.service.EmbeddingRefreshService;
import com.bmc.rag.store.service.VectorStoreService;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin controller for maintenance operations.
 * Provides endpoints for re-embedding data and other administrative tasks.
 *
 * SECURITY NOTE: All endpoints in this controller require ADMIN role.
 * This is enforced by SecurityConfig.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final EmbeddingRefreshService embeddingRefreshService;
    private final VectorStoreService vectorStoreService;

    /**
     * Re-embed all data in the database with real semantic embeddings.
     * This fixes dummy data that was inserted with random embeddings.
     *
     * @return Status and count of refreshed embeddings
     */
    @PostMapping("/reembed")
    @RateLimiter(name = "admin", fallbackMethod = "adminRateLimitFallback")
    public ResponseEntity<Map<String, Object>> reembedAll() {
        log.info("Admin request: Re-embedding all data");

        long startTime = System.currentTimeMillis();
        int count = embeddingRefreshService.refreshAllEmbeddings();
        long duration = System.currentTimeMillis() - startTime;

        log.info("Re-embedding completed: {} records in {}ms", count, duration);

        return ResponseEntity.ok(Map.of(
            "status", "completed",
            "count", count,
            "durationMs", duration
        ));
    }

    /**
     * Re-embed data for a specific source type.
     *
     * @param sourceType The source type to re-embed (e.g., "Incident", "KnowledgeArticle")
     * @return Status and count of refreshed embeddings
     */
    @PostMapping("/reembed/{sourceType}")
    @RateLimiter(name = "admin", fallbackMethod = "adminRateLimitFallbackWithPath")
    public ResponseEntity<Map<String, Object>> reembedBySourceType(@PathVariable String sourceType) {
        log.info("Admin request: Re-embedding data for source type: {}", sourceType);

        long startTime = System.currentTimeMillis();
        int count = embeddingRefreshService.refreshEmbeddingsBySourceType(sourceType);
        long duration = System.currentTimeMillis() - startTime;

        return ResponseEntity.ok(Map.of(
            "status", "completed",
            "sourceType", sourceType,
            "count", count,
            "durationMs", duration
        ));
    }

    /**
     * Get statistics about stored embeddings.
     *
     * @return Embedding statistics by source type
     */
    @GetMapping("/stats")
    @RateLimiter(name = "admin", fallbackMethod = "statsRateLimitFallback")
    public ResponseEntity<Map<String, Long>> getStats() {
        log.info("Admin request: Getting embedding statistics");
        return ResponseEntity.ok(vectorStoreService.getStatistics());
    }

    /**
     * Fallback method when admin rate limit is exceeded.
     */
    @SuppressWarnings("unused")
    private ResponseEntity<Map<String, Object>> adminRateLimitFallback(RequestNotPermitted ex) {
        log.warn("Rate limit exceeded for admin endpoint");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(Map.of(
                "status", "error",
                "message", "Too many requests. Please wait and try again."
            ));
    }

    /**
     * Fallback method when admin rate limit is exceeded (with path variable).
     */
    @SuppressWarnings("unused")
    private ResponseEntity<Map<String, Object>> adminRateLimitFallbackWithPath(String sourceType, RequestNotPermitted ex) {
        log.warn("Rate limit exceeded for admin endpoint (sourceType: {})", sourceType);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(Map.of(
                "status", "error",
                "message", "Too many requests. Please wait and try again.",
                "sourceType", sourceType
            ));
    }

    /**
     * Fallback method when stats rate limit is exceeded.
     */
    @SuppressWarnings("unused")
    private ResponseEntity<Map<String, Long>> statsRateLimitFallback(RequestNotPermitted ex) {
        log.warn("Rate limit exceeded for stats endpoint");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(Map.of());
    }
}
