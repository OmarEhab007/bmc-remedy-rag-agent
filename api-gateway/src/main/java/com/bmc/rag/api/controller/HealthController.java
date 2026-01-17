package com.bmc.rag.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Basic health check.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());

        // Check database
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            health.put("database", "UP");
        } catch (Exception e) {
            health.put("database", "DOWN");
            health.put("databaseError", e.getMessage());
        }

        // Check pgvector extension
        try {
            jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM pg_extension WHERE extname = 'vector')",
                Boolean.class
            );
            health.put("pgvector", "UP");
        } catch (Exception e) {
            health.put("pgvector", "DOWN");
            health.put("pgvectorError", e.getMessage());
        }

        return ResponseEntity.ok(health);
    }

    /**
     * Readiness check (for Kubernetes).
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, String>> ready() {
        try {
            // Check database connection
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(Map.of("status", "ready"));
        } catch (Exception e) {
            log.warn("Readiness check failed: {}", e.getMessage());
            return ResponseEntity.status(503).body(Map.of(
                "status", "not ready",
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Liveness check (for Kubernetes).
     */
    @GetMapping("/live")
    public ResponseEntity<Map<String, String>> live() {
        return ResponseEntity.ok(Map.of("status", "alive"));
    }
}
