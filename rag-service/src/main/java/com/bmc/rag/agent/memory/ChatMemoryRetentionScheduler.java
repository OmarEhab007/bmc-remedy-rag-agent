package com.bmc.rag.agent.memory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task for cleaning up old chat history based on retention policy.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMemoryRetentionScheduler {

    private final PostgresChatMemoryStore chatMemoryStore;

    @Value("${chat.memory.retention-days:30}")
    private int retentionDays;

    /**
     * Run cleanup at 2 AM every day.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldChatHistory() {
        log.info("Starting chat history cleanup (retention: {} days)", retentionDays);

        try {
            int deleted = chatMemoryStore.deleteOldSessions(retentionDays);
            log.info("Chat history cleanup completed: {} messages deleted", deleted);
        } catch (Exception e) {
            log.error("Error during chat history cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Get the current retention policy in days.
     */
    public int getRetentionDays() {
        return retentionDays;
    }

    /**
     * Manually trigger cleanup (for admin use).
     */
    public int triggerCleanup() {
        log.info("Manual chat history cleanup triggered");
        return chatMemoryStore.deleteOldSessions(retentionDays);
    }

    /**
     * Cleanup with custom retention period.
     */
    public int triggerCleanup(int customRetentionDays) {
        log.info("Manual chat history cleanup triggered (retention: {} days)", customRetentionDays);
        return chatMemoryStore.deleteOldSessions(customRetentionDays);
    }
}
