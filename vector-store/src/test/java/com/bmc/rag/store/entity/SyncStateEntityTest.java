package com.bmc.rag.store.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SyncStateEntity.
 * Tests state machine transitions and cursor management.
 */
class SyncStateEntityTest {

    @Test
    void markSyncCompleted_updatesAllFields() {
        // Given
        SyncStateEntity entity = SyncStateEntity.builder()
            .sourceType("Incident")
            .lastSyncTimestamp(1000L)
            .status("running")
            .recordsSynced(0)
            .build();
        Instant beforeSync = Instant.now().minus(1, ChronoUnit.SECONDS);
        long newTimestamp = 2000L;
        int recordCount = 150;

        // When
        entity.markSyncCompleted(newTimestamp, recordCount);
        Instant afterSync = Instant.now().plus(1, ChronoUnit.SECONDS);

        // Then
        assertEquals(newTimestamp, entity.getLastSyncTimestamp());
        assertEquals(recordCount, entity.getRecordsSynced());
        assertEquals("completed", entity.getStatus());
        assertNull(entity.getErrorMessage());
        assertNotNull(entity.getLastSyncAt());
        assertTrue(entity.getLastSyncAt().isAfter(beforeSync));
        assertTrue(entity.getLastSyncAt().isBefore(afterSync));
    }

    @Test
    void markSyncCompleted_clearsErrorMessage() {
        // Given
        SyncStateEntity entity = SyncStateEntity.builder()
            .sourceType("Incident")
            .lastSyncTimestamp(1000L)
            .status("failed")
            .errorMessage("Previous sync failed")
            .build();

        // When
        entity.markSyncCompleted(2000L, 100);

        // Then
        assertEquals("completed", entity.getStatus());
        assertNull(entity.getErrorMessage());
    }

    @Test
    void markSyncRunning_setsStatusAndTimestamp() {
        // Given
        SyncStateEntity entity = SyncStateEntity.builder()
            .sourceType("WorkOrder")
            .lastSyncTimestamp(1000L)
            .status("completed")
            .build();
        Instant beforeRunning = Instant.now().minus(1, ChronoUnit.SECONDS);

        // When
        entity.markSyncRunning();
        Instant afterRunning = Instant.now().plus(1, ChronoUnit.SECONDS);

        // Then
        assertEquals("running", entity.getStatus());
        assertNotNull(entity.getLastSyncAt());
        assertTrue(entity.getLastSyncAt().isAfter(beforeRunning));
        assertTrue(entity.getLastSyncAt().isBefore(afterRunning));
    }

    @Test
    void markSyncFailed_setsStatusAndErrorMessage() {
        // Given
        SyncStateEntity entity = SyncStateEntity.builder()
            .sourceType("KnowledgeArticle")
            .lastSyncTimestamp(1000L)
            .status("running")
            .build();
        Instant beforeFailure = Instant.now().minus(1, ChronoUnit.SECONDS);
        String errorMessage = "ARERR 93: Server timeout";

        // When
        entity.markSyncFailed(errorMessage);
        Instant afterFailure = Instant.now().plus(1, ChronoUnit.SECONDS);

        // Then
        assertEquals("failed", entity.getStatus());
        assertEquals(errorMessage, entity.getErrorMessage());
        assertNotNull(entity.getLastSyncAt());
        assertTrue(entity.getLastSyncAt().isAfter(beforeFailure));
        assertTrue(entity.getLastSyncAt().isBefore(afterFailure));
    }

    @Test
    void stateTransition_completedToRunning_valid() {
        // Given
        SyncStateEntity entity = SyncStateEntity.builder()
            .sourceType("Incident")
            .status("completed")
            .build();

        // When
        entity.markSyncRunning();

        // Then
        assertEquals("running", entity.getStatus());
    }

    @Test
    void stateTransition_runningToCompleted_valid() {
        // Given
        SyncStateEntity entity = SyncStateEntity.builder()
            .sourceType("Incident")
            .status("running")
            .build();

        // When
        entity.markSyncCompleted(5000L, 200);

        // Then
        assertEquals("completed", entity.getStatus());
        assertEquals(5000L, entity.getLastSyncTimestamp());
        assertEquals(200, entity.getRecordsSynced());
    }

    @Test
    void stateTransition_runningToFailed_valid() {
        // Given
        SyncStateEntity entity = SyncStateEntity.builder()
            .sourceType("WorkOrder")
            .status("running")
            .build();

        // When
        entity.markSyncFailed("Connection lost");

        // Then
        assertEquals("failed", entity.getStatus());
        assertEquals("Connection lost", entity.getErrorMessage());
    }

    @Test
    void stateTransition_failedToRunning_retryScenario() {
        // Given
        SyncStateEntity entity = SyncStateEntity.builder()
            .sourceType("ChangeRequest")
            .status("failed")
            .errorMessage("Previous error")
            .build();

        // When
        entity.markSyncRunning();

        // Then
        assertEquals("running", entity.getStatus());
        // Error message should persist until next completion
        assertEquals("Previous error", entity.getErrorMessage());
    }

    @Test
    void cursorManagement_incrementalUpdates() {
        // Given
        SyncStateEntity entity = SyncStateEntity.builder()
            .sourceType("Incident")
            .lastSyncTimestamp(0L)
            .build();

        // When - First sync
        entity.markSyncCompleted(1609459200L, 50);  // 2021-01-01
        long firstCursor = entity.getLastSyncTimestamp();

        // Then
        assertEquals(1609459200L, firstCursor);
        assertEquals(50, entity.getRecordsSynced());

        // When - Second sync
        entity.markSyncCompleted(1640995200L, 30);  // 2022-01-01
        long secondCursor = entity.getLastSyncTimestamp();

        // Then
        assertEquals(1640995200L, secondCursor);
        assertTrue(secondCursor > firstCursor);
        assertEquals(30, entity.getRecordsSynced());
    }

    @Test
    void builder_defaultValues_setCorrectly() {
        // When
        SyncStateEntity entity = SyncStateEntity.builder()
            .sourceType("Incident")
            .build();

        // Then
        assertNotNull(entity);
        assertEquals("Incident", entity.getSourceType());
        assertEquals(0L, entity.getLastSyncTimestamp());
        assertEquals(0, entity.getRecordsSynced());
        assertEquals("completed", entity.getStatus());
        assertNull(entity.getErrorMessage());
        assertNull(entity.getLastSyncAt());
    }

    @Test
    void builder_allFields_createsCompleteEntity() {
        // Given
        Instant now = Instant.now();

        // When
        SyncStateEntity entity = SyncStateEntity.builder()
            .id(1)
            .sourceType("WorkOrder")
            .lastSyncTimestamp(1640995200L)
            .lastSyncAt(now)
            .recordsSynced(500)
            .status("completed")
            .errorMessage(null)
            .build();

        // Then
        assertNotNull(entity);
        assertEquals(1, entity.getId());
        assertEquals("WorkOrder", entity.getSourceType());
        assertEquals(1640995200L, entity.getLastSyncTimestamp());
        assertEquals(now, entity.getLastSyncAt());
        assertEquals(500, entity.getRecordsSynced());
        assertEquals("completed", entity.getStatus());
        assertNull(entity.getErrorMessage());
    }

    @Test
    void markSyncCompleted_zeroRecords_valid() {
        // Given
        SyncStateEntity entity = SyncStateEntity.builder()
            .sourceType("Incident")
            .lastSyncTimestamp(1000L)
            .build();

        // When - No new records found
        entity.markSyncCompleted(2000L, 0);

        // Then
        assertEquals("completed", entity.getStatus());
        assertEquals(2000L, entity.getLastSyncTimestamp());
        assertEquals(0, entity.getRecordsSynced());
        assertNull(entity.getErrorMessage());
    }

    @Test
    void markSyncFailed_nullErrorMessage_setsStatus() {
        // Given
        SyncStateEntity entity = SyncStateEntity.builder()
            .sourceType("Incident")
            .status("running")
            .build();

        // When
        entity.markSyncFailed(null);

        // Then
        assertEquals("failed", entity.getStatus());
        assertNull(entity.getErrorMessage());
    }

    @Test
    void markSyncFailed_emptyErrorMessage_setsStatus() {
        // Given
        SyncStateEntity entity = SyncStateEntity.builder()
            .sourceType("Incident")
            .status("running")
            .build();

        // When
        entity.markSyncFailed("");

        // Then
        assertEquals("failed", entity.getStatus());
        assertEquals("", entity.getErrorMessage());
    }

    @Test
    void multipleTransitions_maintainsCursor() {
        // Given
        SyncStateEntity entity = SyncStateEntity.builder()
            .sourceType("Incident")
            .lastSyncTimestamp(1000L)
            .build();

        // When - Sequence of state transitions
        entity.markSyncRunning();
        entity.markSyncCompleted(2000L, 100);
        entity.markSyncRunning();
        entity.markSyncFailed("Temporary error");
        entity.markSyncRunning();
        entity.markSyncCompleted(3000L, 50);

        // Then - Cursor should be at latest successful sync
        assertEquals("completed", entity.getStatus());
        assertEquals(3000L, entity.getLastSyncTimestamp());
        assertEquals(50, entity.getRecordsSynced());
        assertNull(entity.getErrorMessage());
    }

    @Test
    void markSyncCompleted_negativeTimestamp_allowed() {
        // Given - Edge case: negative Unix epoch (pre-1970)
        SyncStateEntity entity = SyncStateEntity.builder()
            .sourceType("Incident")
            .build();

        // When
        entity.markSyncCompleted(-1000L, 10);

        // Then
        assertEquals(-1000L, entity.getLastSyncTimestamp());
    }

    @Test
    void markSyncCompleted_largeTimestamp_handled() {
        // Given - Far future timestamp
        SyncStateEntity entity = SyncStateEntity.builder()
            .sourceType("Incident")
            .build();
        long farFuture = 2147483647L;  // Year 2038 problem boundary

        // When
        entity.markSyncCompleted(farFuture, 100);

        // Then
        assertEquals(farFuture, entity.getLastSyncTimestamp());
    }

    @Test
    void concurrentStateChanges_lastWins() {
        // Given
        SyncStateEntity entity = SyncStateEntity.builder()
            .sourceType("Incident")
            .status("running")
            .build();

        // When - Simulate concurrent updates (last one wins)
        entity.markSyncCompleted(2000L, 100);
        entity.markSyncFailed("Later failure");

        // Then - Last operation determines final state
        assertEquals("failed", entity.getStatus());
        assertEquals("Later failure", entity.getErrorMessage());
        // But cursor keeps last successful timestamp
        assertEquals(2000L, entity.getLastSyncTimestamp());
    }

    @Test
    void statusValues_definedCorrectly() {
        // Test all known status values
        SyncStateEntity entity = SyncStateEntity.builder()
            .sourceType("Incident")
            .build();

        entity.markSyncRunning();
        assertEquals("running", entity.getStatus());

        entity.markSyncCompleted(1000L, 10);
        assertEquals("completed", entity.getStatus());

        entity.markSyncFailed("Error");
        assertEquals("failed", entity.getStatus());
    }
}
