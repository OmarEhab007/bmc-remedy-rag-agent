package com.bmc.rag.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for agentic (write) operations.
 * Controls ticket creation capabilities via LangChain4j tools.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "agentic")
public class AgenticConfig {

    /**
     * Enable/disable agentic operations.
     * When false, the agent operates in read-only mode.
     * Default: false (disabled for safety)
     */
    private boolean enabled = false;

    /**
     * Confirmation workflow settings.
     */
    private Confirmation confirmation = new Confirmation();

    /**
     * Rate limiting for agentic operations.
     */
    private RateLimit rateLimit = new RateLimit();

    /**
     * Duplicate detection settings.
     */
    private DuplicateDetection duplicateDetection = new DuplicateDetection();

    /**
     * Audit settings.
     */
    private Audit audit = new Audit();

    @Data
    public static class Confirmation {
        /**
         * Timeout in minutes for pending confirmations.
         * After this time, staged actions expire.
         * Default: 5 minutes
         */
        private int timeoutMinutes = 5;

        /**
         * Whether to require confirmation for all write operations.
         * Should always be true for production safety.
         * Default: true
         */
        private boolean requireConfirmation = true;

        /**
         * Maximum pending actions per session.
         */
        private int maxPendingPerSession = 5;
    }

    @Data
    public static class RateLimit {
        /**
         * Maximum number of creations per user per hour.
         * Default: 10
         */
        private int maxCreationsPerHour = 10;

        /**
         * Whether to enable rate limiting for agentic operations.
         * Default: true
         */
        private boolean enabled = true;
    }

    @Data
    public static class DuplicateDetection {
        /**
         * Similarity threshold for duplicate detection.
         * Tickets with similarity >= this value are flagged as potential duplicates.
         * Default: 0.85 (85%)
         */
        private double similarityThreshold = 0.85;

        /**
         * Whether to block creation if duplicates found.
         * If false, only warns but allows creation.
         * Default: false (warn only)
         */
        private boolean blockOnDuplicate = false;

        /**
         * Maximum number of duplicates to check.
         */
        private int maxDuplicatesToCheck = 5;
    }

    @Data
    public static class Audit {
        /**
         * Whether to log all agentic actions to the audit table.
         * Default: true
         */
        private boolean enabled = true;

        /**
         * Whether to include request payloads in audit logs.
         * May contain sensitive information.
         * Default: false
         */
        private boolean includePayloads = false;

        /**
         * Retention period in days for audit logs.
         * Default: 90 days
         */
        private int retentionDays = 90;
    }

    /**
     * Check if agentic operations are fully enabled.
     */
    public boolean isOperational() {
        return enabled && confirmation.requireConfirmation;
    }
}
