package com.bmc.rag.api.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-user rate limiting configuration using Bucket4j.
 * Provides separate rate limits for different API endpoints.
 */
@Slf4j
@Configuration
public class RateLimitConfig {

    @Value("${rate-limit.chat.requests-per-minute:50}")
    private int chatRequestsPerMinute;

    @Value("${rate-limit.search.requests-per-minute:100}")
    private int searchRequestsPerMinute;

    @Value("${rate-limit.admin.requests-per-minute:20}")
    private int adminRequestsPerMinute;

    @Value("${rate-limit.feedback.requests-per-minute:30}")
    private int feedbackRequestsPerMinute;

    @Value("${rate-limit.action.requests-per-hour:10}")
    private int actionRequestsPerHour;

    // User buckets by endpoint type
    private final Map<String, Bucket> chatBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> searchBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> adminBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> feedbackBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> actionBuckets = new ConcurrentHashMap<>();

    // Maximum number of cached buckets per endpoint type
    private static final int MAX_BUCKETS = 10000;

    /**
     * Get or create a rate limit bucket for chat endpoint.
     */
    public Bucket getChatBucket(String userId) {
        return chatBuckets.computeIfAbsent(userId, id -> createBucket(chatRequestsPerMinute));
    }

    /**
     * Get or create a rate limit bucket for search endpoint.
     */
    public Bucket getSearchBucket(String userId) {
        return searchBuckets.computeIfAbsent(userId, id -> createBucket(searchRequestsPerMinute));
    }

    /**
     * Get or create a rate limit bucket for admin endpoint.
     */
    public Bucket getAdminBucket(String userId) {
        return adminBuckets.computeIfAbsent(userId, id -> createBucket(adminRequestsPerMinute));
    }

    /**
     * Get or create a rate limit bucket for feedback endpoint.
     */
    public Bucket getFeedbackBucket(String userId) {
        return feedbackBuckets.computeIfAbsent(userId, id -> createBucket(feedbackRequestsPerMinute));
    }

    /**
     * Get or create a rate limit bucket for action endpoint (agentic operations).
     * Uses hourly limit instead of per-minute.
     */
    public Bucket getActionBucket(String userId) {
        return actionBuckets.computeIfAbsent(userId, id -> createHourlyBucket(actionRequestsPerHour));
    }

    /**
     * Check if rate limit is exceeded for chat endpoint.
     */
    public boolean isRateLimitedChat(String userId) {
        return !getChatBucket(userId).tryConsume(1);
    }

    /**
     * Check if rate limit is exceeded for search endpoint.
     */
    public boolean isRateLimitedSearch(String userId) {
        return !getSearchBucket(userId).tryConsume(1);
    }

    /**
     * Check if rate limit is exceeded for admin endpoint.
     */
    public boolean isRateLimitedAdmin(String userId) {
        return !getAdminBucket(userId).tryConsume(1);
    }

    /**
     * Check if rate limit is exceeded for feedback endpoint.
     */
    public boolean isRateLimitedFeedback(String userId) {
        return !getFeedbackBucket(userId).tryConsume(1);
    }

    /**
     * Check if rate limit is exceeded for action endpoint (agentic operations).
     */
    public boolean isRateLimitedAction(String userId) {
        return !getActionBucket(userId).tryConsume(1);
    }

    /**
     * Create a bucket with the specified requests per minute limit.
     */
    private Bucket createBucket(int requestsPerMinute) {
        Bandwidth limit = Bandwidth.classic(
            requestsPerMinute,
            Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))
        );
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    /**
     * Create a bucket with the specified requests per hour limit (for agentic operations).
     */
    private Bucket createHourlyBucket(int requestsPerHour) {
        Bandwidth limit = Bandwidth.classic(
            requestsPerHour,
            Refill.greedy(requestsPerHour, Duration.ofHours(1))
        );
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    /**
     * Clean up old buckets periodically to prevent memory leaks.
     * Called by SchedulerConfig.
     */
    public void cleanupOldBuckets() {
        if (chatBuckets.size() > MAX_BUCKETS) {
            log.warn("Chat buckets exceeded limit, clearing...");
            chatBuckets.clear();
        }
        if (searchBuckets.size() > MAX_BUCKETS) {
            log.warn("Search buckets exceeded limit, clearing...");
            searchBuckets.clear();
        }
        if (adminBuckets.size() > MAX_BUCKETS) {
            log.warn("Admin buckets exceeded limit, clearing...");
            adminBuckets.clear();
        }
        if (feedbackBuckets.size() > MAX_BUCKETS) {
            log.warn("Feedback buckets exceeded limit, clearing...");
            feedbackBuckets.clear();
        }
        if (actionBuckets.size() > MAX_BUCKETS) {
            log.warn("Action buckets exceeded limit, clearing...");
            actionBuckets.clear();
        }
    }

    /**
     * Get rate limit statistics.
     */
    public Map<String, Integer> getStatistics() {
        return Map.of(
            "chatBuckets", chatBuckets.size(),
            "searchBuckets", searchBuckets.size(),
            "adminBuckets", adminBuckets.size(),
            "feedbackBuckets", feedbackBuckets.size(),
            "actionBuckets", actionBuckets.size()
        );
    }
}
