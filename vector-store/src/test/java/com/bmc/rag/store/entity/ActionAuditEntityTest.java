package com.bmc.rag.store.entity;

import com.bmc.rag.store.entity.ActionAuditEntity.ActionStatus;
import com.bmc.rag.store.entity.ActionAuditEntity.ActionType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ActionAuditEntity.
 * Tests state transitions, factory methods, and business logic.
 */
class ActionAuditEntityTest {

    @Test
    void forStaged_validInputs_createsEntityWithStagedStatus() {
        // Given
        String actionId = "abc12345";
        String sessionId = "session-123";
        String userId = "user@example.com";
        ActionType actionType = ActionType.INCIDENT_CREATE;
        String summary = "VPN connection failed";

        // When
        ActionAuditEntity entity = ActionAuditEntity.forStaged(actionId, sessionId, userId, actionType, summary);

        // Then
        assertNotNull(entity);
        assertEquals(actionId, entity.getActionId());
        assertEquals(sessionId, entity.getSessionId());
        assertEquals(userId, entity.getUserId());
        assertEquals(actionType, entity.getActionType());
        assertEquals(ActionStatus.STAGED, entity.getStatus());
        assertEquals(summary, entity.getSummary());
        assertNotNull(entity.getStagedAt());
        assertNull(entity.getResolvedAt());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    void forStaged_longSummary_truncatesTo250Characters() {
        // Given
        String longSummary = "A".repeat(300);  // 300 characters
        String expected = "A".repeat(247) + "...";  // Truncated to 247 + "..."

        // When
        ActionAuditEntity entity = ActionAuditEntity.forStaged(
            "abc12345", "session-1", "user@example.com", ActionType.INCIDENT_CREATE, longSummary
        );

        // Then
        assertNotNull(entity.getSummary());
        assertEquals(250, entity.getSummary().length());
        assertEquals(expected, entity.getSummary());
        assertTrue(entity.getSummary().endsWith("..."));
    }

    @Test
    void forStaged_exactlyMaxLength_doesNotTruncate() {
        // Given
        String summary = "A".repeat(250);  // Exactly 250 characters

        // When
        ActionAuditEntity entity = ActionAuditEntity.forStaged(
            "abc12345", "session-1", "user@example.com", ActionType.INCIDENT_CREATE, summary
        );

        // Then
        assertEquals(250, entity.getSummary().length());
        assertFalse(entity.getSummary().endsWith("..."));
    }

    @Test
    void forStaged_summaryWithExactly251Characters_truncatesCorrectly() {
        // Given
        String summary = "A".repeat(251);  // 251 characters (just over limit)
        String expected = "A".repeat(247) + "...";  // Should truncate

        // When
        ActionAuditEntity entity = ActionAuditEntity.forStaged(
            "abc12345", "session-1", "user@example.com", ActionType.INCIDENT_CREATE, summary
        );

        // Then
        assertEquals(250, entity.getSummary().length());
        assertEquals(expected, entity.getSummary());
    }

    @Test
    void forStaged_nullSummary_doesNotThrowException() {
        // Given
        String nullSummary = null;

        // When
        ActionAuditEntity entity = ActionAuditEntity.forStaged(
            "abc12345", "session-1", "user@example.com", ActionType.INCIDENT_CREATE, nullSummary
        );

        // Then
        assertNull(entity.getSummary());
    }

    @Test
    void markExecuted_setsStatusAndRecordId() {
        // Given
        ActionAuditEntity entity = ActionAuditEntity.forStaged(
            "abc12345", "session-1", "user@example.com", ActionType.INCIDENT_CREATE, "Test summary"
        );
        Instant beforeExecution = Instant.now().minus(1, ChronoUnit.SECONDS);
        String recordId = "INC000012345";

        // When
        entity.markExecuted(recordId);
        Instant afterExecution = Instant.now().plus(1, ChronoUnit.SECONDS);

        // Then
        assertEquals(ActionStatus.EXECUTED, entity.getStatus());
        assertEquals(recordId, entity.getRecordId());
        assertNotNull(entity.getResolvedAt());
        assertTrue(entity.getResolvedAt().isAfter(beforeExecution));
        assertTrue(entity.getResolvedAt().isBefore(afterExecution));
    }

    @Test
    void markFailed_setsStatusAndErrorMessage() {
        // Given
        ActionAuditEntity entity = ActionAuditEntity.forStaged(
            "abc12345", "session-1", "user@example.com", ActionType.WORK_ORDER_CREATE, "Test summary"
        );
        Instant beforeFailure = Instant.now().minus(1, ChronoUnit.SECONDS);
        String errorMessage = "ARERR 93: Server timeout";

        // When
        entity.markFailed(errorMessage);
        Instant afterFailure = Instant.now().plus(1, ChronoUnit.SECONDS);

        // Then
        assertEquals(ActionStatus.FAILED, entity.getStatus());
        assertEquals(errorMessage, entity.getErrorMessage());
        assertNotNull(entity.getResolvedAt());
        assertTrue(entity.getResolvedAt().isAfter(beforeFailure));
        assertTrue(entity.getResolvedAt().isBefore(afterFailure));
        assertNull(entity.getRecordId());
    }

    @Test
    void markCancelled_setsStatusAndResolvedTime() {
        // Given
        ActionAuditEntity entity = ActionAuditEntity.forStaged(
            "abc12345", "session-1", "user@example.com", ActionType.INCIDENT_UPDATE, "Test summary"
        );
        Instant beforeCancellation = Instant.now().minus(1, ChronoUnit.SECONDS);

        // When
        entity.markCancelled();
        Instant afterCancellation = Instant.now().plus(1, ChronoUnit.SECONDS);

        // Then
        assertEquals(ActionStatus.CANCELLED, entity.getStatus());
        assertNotNull(entity.getResolvedAt());
        assertTrue(entity.getResolvedAt().isAfter(beforeCancellation));
        assertTrue(entity.getResolvedAt().isBefore(afterCancellation));
        assertNull(entity.getRecordId());
        assertNull(entity.getErrorMessage());
    }

    @Test
    void markExpired_setsStatusAndResolvedTime() {
        // Given
        ActionAuditEntity entity = ActionAuditEntity.forStaged(
            "abc12345", "session-1", "user@example.com", ActionType.WORK_ORDER_UPDATE, "Test summary"
        );
        Instant beforeExpiration = Instant.now().minus(1, ChronoUnit.SECONDS);

        // When
        entity.markExpired();
        Instant afterExpiration = Instant.now().plus(1, ChronoUnit.SECONDS);

        // Then
        assertEquals(ActionStatus.EXPIRED, entity.getStatus());
        assertNotNull(entity.getResolvedAt());
        assertTrue(entity.getResolvedAt().isAfter(beforeExpiration));
        assertTrue(entity.getResolvedAt().isBefore(afterExpiration));
        assertNull(entity.getRecordId());
        assertNull(entity.getErrorMessage());
    }

    @Test
    void stateTransition_stagedToExecuted_valid() {
        // Given
        ActionAuditEntity entity = ActionAuditEntity.forStaged(
            "abc12345", "session-1", "user@example.com", ActionType.INCIDENT_CREATE, "Test"
        );

        // When
        entity.markExecuted("INC000123");

        // Then
        assertEquals(ActionStatus.EXECUTED, entity.getStatus());
        assertEquals("INC000123", entity.getRecordId());
    }

    @Test
    void stateTransition_stagedToFailed_valid() {
        // Given
        ActionAuditEntity entity = ActionAuditEntity.forStaged(
            "abc12345", "session-1", "user@example.com", ActionType.INCIDENT_CREATE, "Test"
        );

        // When
        entity.markFailed("Connection timeout");

        // Then
        assertEquals(ActionStatus.FAILED, entity.getStatus());
        assertEquals("Connection timeout", entity.getErrorMessage());
    }

    @Test
    void stateTransition_stagedToCancelled_valid() {
        // Given
        ActionAuditEntity entity = ActionAuditEntity.forStaged(
            "abc12345", "session-1", "user@example.com", ActionType.INCIDENT_CREATE, "Test"
        );

        // When
        entity.markCancelled();

        // Then
        assertEquals(ActionStatus.CANCELLED, entity.getStatus());
    }

    @Test
    void stateTransition_stagedToExpired_valid() {
        // Given
        ActionAuditEntity entity = ActionAuditEntity.forStaged(
            "abc12345", "session-1", "user@example.com", ActionType.INCIDENT_CREATE, "Test"
        );

        // When
        entity.markExpired();

        // Then
        assertEquals(ActionStatus.EXPIRED, entity.getStatus());
    }

    @Test
    void builder_allFields_createsCompleteEntity() {
        // Given
        Instant now = Instant.now();

        // When
        ActionAuditEntity entity = ActionAuditEntity.builder()
            .id(1L)
            .actionId("abc12345")
            .sessionId("session-123")
            .userId("user@example.com")
            .actionType(ActionType.INCIDENT_CREATE)
            .status(ActionStatus.EXECUTED)
            .summary("Test incident")
            .recordId("INC000123")
            .errorMessage(null)
            .requestPayload("{\"summary\":\"Test\"}")
            .stagedAt(now)
            .resolvedAt(now.plus(5, ChronoUnit.MINUTES))
            .createdAt(now)
            .updatedAt(now)
            .clientIp("192.168.1.100")
            .userAgent("Mozilla/5.0")
            .build();

        // Then
        assertNotNull(entity);
        assertEquals(1L, entity.getId());
        assertEquals("abc12345", entity.getActionId());
        assertEquals("session-123", entity.getSessionId());
        assertEquals("user@example.com", entity.getUserId());
        assertEquals(ActionType.INCIDENT_CREATE, entity.getActionType());
        assertEquals(ActionStatus.EXECUTED, entity.getStatus());
        assertEquals("Test incident", entity.getSummary());
        assertEquals("INC000123", entity.getRecordId());
        assertEquals("{\"summary\":\"Test\"}", entity.getRequestPayload());
        assertEquals("192.168.1.100", entity.getClientIp());
        assertEquals("Mozilla/5.0", entity.getUserAgent());
    }

    @Test
    void onUpdate_setsUpdatedAt() {
        // Given
        ActionAuditEntity entity = ActionAuditEntity.forStaged(
            "abc12345", "session-1", "user@example.com", ActionType.INCIDENT_CREATE, "Test"
        );
        Instant originalUpdatedAt = entity.getUpdatedAt();

        // Wait a tiny bit to ensure different timestamp
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When - simulate @PreUpdate
        entity.onUpdate();

        // Then
        assertNotNull(entity.getUpdatedAt());
        assertTrue(entity.getUpdatedAt().isAfter(originalUpdatedAt) ||
                   entity.getUpdatedAt().equals(originalUpdatedAt));
    }

    @Test
    void actionType_allValues_defined() {
        // Verify all action types are accessible
        assertNotNull(ActionType.INCIDENT_CREATE);
        assertNotNull(ActionType.WORK_ORDER_CREATE);
        assertNotNull(ActionType.INCIDENT_UPDATE);
        assertNotNull(ActionType.WORK_ORDER_UPDATE);
        assertEquals(4, ActionType.values().length);
    }

    @Test
    void actionStatus_allValues_defined() {
        // Verify all action statuses are accessible
        assertNotNull(ActionStatus.STAGED);
        assertNotNull(ActionStatus.CONFIRMED);
        assertNotNull(ActionStatus.EXECUTED);
        assertNotNull(ActionStatus.CANCELLED);
        assertNotNull(ActionStatus.EXPIRED);
        assertNotNull(ActionStatus.FAILED);
        assertEquals(6, ActionStatus.values().length);
    }

    @Test
    void multipleStateTransitions_trackTimestamps() {
        // Given
        ActionAuditEntity entity = ActionAuditEntity.forStaged(
            "abc12345", "session-1", "user@example.com", ActionType.INCIDENT_CREATE, "Test"
        );
        Instant stagedTime = entity.getStagedAt();

        // When - first transition
        entity.markFailed("First attempt failed");
        Instant firstResolvedTime = entity.getResolvedAt();

        // Then - verify first transition
        assertEquals(ActionStatus.FAILED, entity.getStatus());
        assertNotNull(firstResolvedTime);
        assertTrue(firstResolvedTime.isAfter(stagedTime) || firstResolvedTime.equals(stagedTime));

        // When - second transition (override)
        entity.markExecuted("INC000123");
        Instant secondResolvedTime = entity.getResolvedAt();

        // Then - verify second transition updates timestamp
        assertEquals(ActionStatus.EXECUTED, entity.getStatus());
        assertNotNull(secondResolvedTime);
        assertTrue(secondResolvedTime.isAfter(firstResolvedTime) ||
                   secondResolvedTime.equals(firstResolvedTime));
    }
}
