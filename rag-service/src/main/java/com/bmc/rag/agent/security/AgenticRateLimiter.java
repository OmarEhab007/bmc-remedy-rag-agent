package com.bmc.rag.agent.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter specifically for agentic (write) operations.
 * Limits creations to prevent abuse and accidental mass creation.
 * Default: 10 creations per user per hour.
 *
 * Uses Caffeine cache with expiring counters for simplicity.
 */
@Slf4j
@Component
public class AgenticRateLimiter {

    private final int maxCreationsPerHour;

    // Cache of user ID -> atomic counter, expires after 1 hour
    private final Cache<String, AtomicInteger> userCounters;

    public AgenticRateLimiter(
            @Value("${agentic.rate-limit.max-creations-per-hour:10}") int maxCreationsPerHour) {
        this.maxCreationsPerHour = maxCreationsPerHour;

        // Initialize cache with 1-hour expiry per entry
        this.userCounters = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .maximumSize(10_000)
            .recordStats()
            .build();

        log.info("Agentic rate limiter initialized: {} creations/hour per user", maxCreationsPerHour);
    }

    /**
     * Check if a user has exceeded their creation rate limit.
     * If not exceeded, increments the counter.
     *
     * @param userId The user ID to check
     * @return true if rate limited (action should be blocked), false if allowed
     */
    public boolean isRateLimited(String userId) {
        if (userId == null || userId.isBlank()) {
            log.warn("Rate limit check with null/blank userId - blocking");
            return true;
        }

        AtomicInteger counter = userCounters.get(userId, k -> new AtomicInteger(0));
        int currentCount = counter.incrementAndGet();

        if (currentCount > maxCreationsPerHour) {
            log.warn("User {} exceeded agentic rate limit ({}/hour), current: {}",
                userId, maxCreationsPerHour, currentCount);
            return true;
        }

        log.debug("User {} action count: {}/{}", userId, currentCount, maxCreationsPerHour);
        return false;
    }

    /**
     * Get remaining tokens for a user.
     *
     * @param userId The user ID
     * @return Number of remaining creation tokens
     */
    public long getRemainingTokens(String userId) {
        if (userId == null || userId.isBlank()) {
            return 0;
        }
        AtomicInteger counter = userCounters.getIfPresent(userId);
        if (counter == null) {
            return maxCreationsPerHour;
        }
        return Math.max(0, maxCreationsPerHour - counter.get());
    }

    /**
     * Get a rate limit status for a user.
     */
    public RateLimitStatus getStatus(String userId) {
        long remaining = getRemainingTokens(userId);
        return new RateLimitStatus(
            maxCreationsPerHour,
            remaining,
            remaining <= 0
        );
    }

    /**
     * Clear the rate limit for a specific user (admin operation).
     */
    public void clearRateLimitForUser(String userId) {
        userCounters.invalidate(userId);
        log.info("Cleared rate limit for user: {}", userId);
    }

    /**
     * Get statistics about the rate limiter.
     */
    public RateLimiterStats getStats() {
        return new RateLimiterStats(
            (int) userCounters.estimatedSize(),
            maxCreationsPerHour
        );
    }

    /**
     * Rate limit status for a user.
     */
    public record RateLimitStatus(
        int maxPerHour,
        long remaining,
        boolean isLimited
    ) {}

    /**
     * Rate limiter statistics.
     */
    public record RateLimiterStats(
        int cachedCounters,
        int maxCreationsPerHour
    ) {}
}
