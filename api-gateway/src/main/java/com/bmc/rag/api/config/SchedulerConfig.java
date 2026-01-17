package com.bmc.rag.api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuration for scheduled tasks.
 * Enables Spring's @Scheduled annotation processing.
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulerConfig {

    private final RateLimitConfig rateLimitConfig;

    /**
     * Clean up old rate limit buckets every hour to prevent memory leaks.
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupRateLimitBuckets() {
        log.debug("Running rate limit bucket cleanup");
        rateLimitConfig.cleanupOldBuckets();
    }

    /**
     * Log scheduler statistics every 5 minutes.
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void logSchedulerStats() {
        log.debug("Scheduler heartbeat - rate limit stats: {}", rateLimitConfig.getStatistics());
    }
}
