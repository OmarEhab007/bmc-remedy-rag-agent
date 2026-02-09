package com.bmc.rag.api.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SchedulerConfig.
 * Tests scheduled task execution.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SchedulerConfig Tests")
class SchedulerConfigTest {

    @Mock
    private RateLimitConfig rateLimitConfig;

    private SchedulerConfig schedulerConfig;

    @BeforeEach
    void setUp() {
        schedulerConfig = new SchedulerConfig(rateLimitConfig);
    }

    @Test
    @DisplayName("cleanupRateLimitBuckets_shouldCallCleanupMethod")
    void cleanupRateLimitBuckets_shouldCallCleanupMethod() {
        schedulerConfig.cleanupRateLimitBuckets();

        verify(rateLimitConfig).cleanupOldBuckets();
    }

    @Test
    @DisplayName("logSchedulerStats_shouldCallGetStatistics")
    void logSchedulerStats_shouldCallGetStatistics() {
        when(rateLimitConfig.getStatistics()).thenReturn(Map.of(
            "chatBuckets", 10,
            "searchBuckets", 5
        ));

        schedulerConfig.logSchedulerStats();

        verify(rateLimitConfig).getStatistics();
    }
}
