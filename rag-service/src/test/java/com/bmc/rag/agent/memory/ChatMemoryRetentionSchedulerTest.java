package com.bmc.rag.agent.memory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMemoryRetentionSchedulerTest {

    @Mock
    private PostgresChatMemoryStore chatMemoryStore;

    @InjectMocks
    private ChatMemoryRetentionScheduler scheduler;

    @Test
    @DisplayName("cleanupOldChatHistory - delegates to store with configured retention days")
    void cleanupOldChatHistory_delegatesToStore() {
        ReflectionTestUtils.setField(scheduler, "retentionDays", 30);
        when(chatMemoryStore.deleteOldSessions(30)).thenReturn(5);

        scheduler.cleanupOldChatHistory();

        verify(chatMemoryStore).deleteOldSessions(30);
    }

    @Test
    @DisplayName("cleanupOldChatHistory - handles store exception gracefully")
    void cleanupOldChatHistory_storeThrows_doesNotPropagate() {
        ReflectionTestUtils.setField(scheduler, "retentionDays", 30);
        when(chatMemoryStore.deleteOldSessions(30)).thenThrow(new RuntimeException("DB error"));

        // Should not throw
        scheduler.cleanupOldChatHistory();
        verify(chatMemoryStore).deleteOldSessions(30);
    }

    @Test
    @DisplayName("getRetentionDays - returns configured value")
    void getRetentionDays_returnsConfiguredValue() {
        ReflectionTestUtils.setField(scheduler, "retentionDays", 45);
        assertThat(scheduler.getRetentionDays()).isEqualTo(45);
    }

    @Test
    @DisplayName("triggerCleanup - delegates and returns deleted count")
    void triggerCleanup_delegatesAndReturnsCount() {
        ReflectionTestUtils.setField(scheduler, "retentionDays", 30);
        when(chatMemoryStore.deleteOldSessions(30)).thenReturn(12);

        int deleted = scheduler.triggerCleanup();

        assertThat(deleted).isEqualTo(12);
        verify(chatMemoryStore).deleteOldSessions(30);
    }

    @Test
    @DisplayName("triggerCleanup with custom days - uses custom retention")
    void triggerCleanup_customDays_usesCustomRetention() {
        when(chatMemoryStore.deleteOldSessions(7)).thenReturn(20);

        int deleted = scheduler.triggerCleanup(7);

        assertThat(deleted).isEqualTo(20);
        verify(chatMemoryStore).deleteOldSessions(7);
    }

    @Test
    @DisplayName("triggerCleanup - returns zero when no old sessions")
    void triggerCleanup_noOldSessions_returnsZero() {
        ReflectionTestUtils.setField(scheduler, "retentionDays", 30);
        when(chatMemoryStore.deleteOldSessions(30)).thenReturn(0);

        int deleted = scheduler.triggerCleanup();

        assertThat(deleted).isEqualTo(0);
    }
}
