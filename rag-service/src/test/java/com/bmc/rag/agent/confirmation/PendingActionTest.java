package com.bmc.rag.agent.confirmation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static com.bmc.rag.agent.confirmation.PendingAction.ActionStatus.*;
import static com.bmc.rag.agent.confirmation.PendingAction.ActionType.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PendingAction")
class PendingActionTest {

    @Nested
    @DisplayName("Builder Defaults")
    class BuilderDefaults {

        @Test
        @DisplayName("should generate 8-character actionId by default")
        void shouldGenerateActionIdWithCorrectLength() {
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .build();

            assertThat(action.getActionId())
                .isNotNull()
                .hasSize(8);
        }

        @Test
        @DisplayName("should set stagedAt to current time by default")
        void shouldSetStagedAtToNow() {
            Instant before = Instant.now();
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .build();
            Instant after = Instant.now();

            assertThat(action.getStagedAt())
                .isNotNull()
                .isBetween(before, after);
        }

        @Test
        @DisplayName("should set status to PENDING by default")
        void shouldSetStatusToPending() {
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .build();

            assertThat(action.getStatus()).isEqualTo(PENDING);
        }

        @Test
        @DisplayName("should allow overriding default actionId")
        void shouldAllowOverridingActionId() {
            PendingAction action = PendingAction.builder()
                .actionId("custom-id")
                .sessionId("session1")
                .userId("user1")
                .build();

            assertThat(action.getActionId()).isEqualTo("custom-id");
        }

        @Test
        @DisplayName("should allow overriding default stagedAt")
        void shouldAllowOverridingStagedAt() {
            Instant customTime = Instant.parse("2025-01-15T10:00:00Z");
            PendingAction action = PendingAction.builder()
                .stagedAt(customTime)
                .sessionId("session1")
                .userId("user1")
                .build();

            assertThat(action.getStagedAt()).isEqualTo(customTime);
        }

        @Test
        @DisplayName("should allow overriding default status")
        void shouldAllowOverridingStatus() {
            PendingAction action = PendingAction.builder()
                .status(CONFIRMED)
                .sessionId("session1")
                .userId("user1")
                .build();

            assertThat(action.getStatus()).isEqualTo(CONFIRMED);
        }
    }

    @Nested
    @DisplayName("isExpired()")
    class IsExpired {

        @Test
        @DisplayName("should return false when expiresAt is null")
        void shouldReturnFalseWhenExpiresAtIsNull() {
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .expiresAt(null)
                .build();

            assertThat(action.isExpired()).isFalse();
        }

        @Test
        @DisplayName("should return true when current time is after expiresAt")
        void shouldReturnTrueWhenExpired() {
            Instant pastTime = Instant.now().minusSeconds(60);
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .expiresAt(pastTime)
                .build();

            assertThat(action.isExpired()).isTrue();
        }

        @Test
        @DisplayName("should return false when current time is before expiresAt")
        void shouldReturnFalseWhenNotExpired() {
            Instant futureTime = Instant.now().plusSeconds(300);
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .expiresAt(futureTime)
                .build();

            assertThat(action.isExpired()).isFalse();
        }
    }

    @Nested
    @DisplayName("isConfirmable()")
    class IsConfirmable {

        @Test
        @DisplayName("should return true when status is PENDING and not expired")
        void shouldReturnTrueWhenPendingAndNotExpired() {
            Instant futureTime = Instant.now().plusSeconds(300);
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .status(PENDING)
                .expiresAt(futureTime)
                .build();

            assertThat(action.isConfirmable()).isTrue();
        }

        @Test
        @DisplayName("should return true when status is PENDING and expiresAt is null")
        void shouldReturnTrueWhenPendingAndNoExpiration() {
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .status(PENDING)
                .expiresAt(null)
                .build();

            assertThat(action.isConfirmable()).isTrue();
        }

        @Test
        @DisplayName("should return false when status is PENDING but expired")
        void shouldReturnFalseWhenPendingButExpired() {
            Instant pastTime = Instant.now().minusSeconds(60);
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .status(PENDING)
                .expiresAt(pastTime)
                .build();

            assertThat(action.isConfirmable()).isFalse();
        }

        @Test
        @DisplayName("should return false when status is CONFIRMED")
        void shouldReturnFalseWhenConfirmed() {
            Instant futureTime = Instant.now().plusSeconds(300);
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .status(CONFIRMED)
                .expiresAt(futureTime)
                .build();

            assertThat(action.isConfirmable()).isFalse();
        }

        @Test
        @DisplayName("should return false when status is CANCELLED")
        void shouldReturnFalseWhenCancelled() {
            Instant futureTime = Instant.now().plusSeconds(300);
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .status(CANCELLED)
                .expiresAt(futureTime)
                .build();

            assertThat(action.isConfirmable()).isFalse();
        }

        @Test
        @DisplayName("should return false when status is EXECUTED")
        void shouldReturnFalseWhenExecuted() {
            Instant futureTime = Instant.now().plusSeconds(300);
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .status(EXECUTED)
                .expiresAt(futureTime)
                .build();

            assertThat(action.isConfirmable()).isFalse();
        }

        @Test
        @DisplayName("should return false when status is EXPIRED")
        void shouldReturnFalseWhenAlreadyExpired() {
            Instant futureTime = Instant.now().plusSeconds(300);
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .status(EXPIRED)
                .expiresAt(futureTime)
                .build();

            assertThat(action.isConfirmable()).isFalse();
        }

        @Test
        @DisplayName("should return false when status is FAILED")
        void shouldReturnFalseWhenFailed() {
            Instant futureTime = Instant.now().plusSeconds(300);
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .status(FAILED)
                .expiresAt(futureTime)
                .build();

            assertThat(action.isConfirmable()).isFalse();
        }
    }

    @Nested
    @DisplayName("belongsTo()")
    class BelongsTo {

        @Test
        @DisplayName("should return true when both sessionId and userId match")
        void shouldReturnTrueWhenBothMatch() {
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .build();

            assertThat(action.belongsTo("session1", "user1")).isTrue();
        }

        @Test
        @DisplayName("should return false when sessionId does not match")
        void shouldReturnFalseWhenSessionIdDoesNotMatch() {
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .build();

            assertThat(action.belongsTo("session2", "user1")).isFalse();
        }

        @Test
        @DisplayName("should return false when userId does not match")
        void shouldReturnFalseWhenUserIdDoesNotMatch() {
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .build();

            assertThat(action.belongsTo("session1", "user2")).isFalse();
        }

        @Test
        @DisplayName("should return false when action sessionId is null")
        void shouldReturnFalseWhenActionSessionIdIsNull() {
            PendingAction action = PendingAction.builder()
                .sessionId(null)
                .userId("user1")
                .build();

            assertThat(action.belongsTo("session1", "user1")).isFalse();
        }

        @Test
        @DisplayName("should return false when action userId is null")
        void shouldReturnFalseWhenActionUserIdIsNull() {
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId(null)
                .build();

            assertThat(action.belongsTo("session1", "user1")).isFalse();
        }

        @Test
        @DisplayName("should return false when both action identifiers are null")
        void shouldReturnFalseWhenBothActionIdentifiersAreNull() {
            PendingAction action = PendingAction.builder()
                .sessionId(null)
                .userId(null)
                .build();

            assertThat(action.belongsTo("session1", "user1")).isFalse();
        }
    }

    @Nested
    @DisplayName("Status Transition Methods")
    class StatusTransitions {

        @Test
        @DisplayName("confirm() should set status to CONFIRMED")
        void confirmShouldSetStatusToConfirmed() {
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .status(PENDING)
                .build();

            action.confirm();

            assertThat(action.getStatus()).isEqualTo(CONFIRMED);
        }

        @Test
        @DisplayName("cancel() should set status to CANCELLED")
        void cancelShouldSetStatusToCancelled() {
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .status(PENDING)
                .build();

            action.cancel();

            assertThat(action.getStatus()).isEqualTo(CANCELLED);
        }

        @Test
        @DisplayName("expire() should set status to EXPIRED")
        void expireShouldSetStatusToExpired() {
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .status(PENDING)
                .build();

            action.expire();

            assertThat(action.getStatus()).isEqualTo(EXPIRED);
        }

        @Test
        @DisplayName("markExecuted() should set status to EXECUTED and populate result fields")
        void markExecutedShouldSetStatusAndResultFields() {
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .status(CONFIRMED)
                .build();

            action.markExecuted("INC000123", "Incident created successfully");

            assertThat(action.getStatus()).isEqualTo(EXECUTED);
            assertThat(action.getCreatedRecordId()).isEqualTo("INC000123");
            assertThat(action.getResultMessage()).isEqualTo("Incident created successfully");
        }

        @Test
        @DisplayName("markFailed() should set status to FAILED and populate error message")
        void markFailedShouldSetStatusAndErrorMessage() {
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .status(CONFIRMED)
                .build();

            action.markFailed("Connection timeout");

            assertThat(action.getStatus()).isEqualTo(FAILED);
            assertThat(action.getResultMessage()).isEqualTo("Connection timeout");
        }
    }

    @Nested
    @DisplayName("getConfirmationPrompt()")
    class GetConfirmationPrompt {

        @Test
        @DisplayName("should generate prompt with incident label")
        void shouldGeneratePromptWithIncidentLabel() {
            PendingAction action = PendingAction.builder()
                .actionId("abc12345")
                .sessionId("session1")
                .userId("user1")
                .actionType(INCIDENT_CREATE)
                .preview("Summary: Server down\nPriority: High")
                .build();

            String prompt = action.getConfirmationPrompt();

            assertThat(prompt)
                .contains("I'll create this Incident:")
                .contains("Summary: Server down\nPriority: High")
                .contains("confirm abc12345")
                .contains("cancel abc12345")
                .contains("This action will expire in 5 minutes");
        }

        @Test
        @DisplayName("should generate prompt with work order label")
        void shouldGeneratePromptWithWorkOrderLabel() {
            PendingAction action = PendingAction.builder()
                .actionId("xyz98765")
                .sessionId("session1")
                .userId("user1")
                .actionType(WORK_ORDER_CREATE)
                .preview("Task: Install patch")
                .build();

            String prompt = action.getConfirmationPrompt();

            assertThat(prompt)
                .contains("I'll create this Work Order:")
                .contains("Task: Install patch")
                .contains("confirm xyz98765")
                .contains("cancel xyz98765");
        }

        @Test
        @DisplayName("should generate prompt with incident update label")
        void shouldGeneratePromptWithIncidentUpdateLabel() {
            PendingAction action = PendingAction.builder()
                .actionId("upd12345")
                .sessionId("session1")
                .userId("user1")
                .actionType(INCIDENT_UPDATE)
                .preview("INC000123: Change status to Resolved")
                .build();

            String prompt = action.getConfirmationPrompt();

            assertThat(prompt).contains("I'll create this Incident Update:");
        }

        @Test
        @DisplayName("should generate prompt with work order update label")
        void shouldGeneratePromptWithWorkOrderUpdateLabel() {
            PendingAction action = PendingAction.builder()
                .actionId("woupd123")
                .sessionId("session1")
                .userId("user1")
                .actionType(WORK_ORDER_UPDATE)
                .preview("WO000456: Assign to Network Team")
                .build();

            String prompt = action.getConfirmationPrompt();

            assertThat(prompt).contains("I'll create this Work Order Update:");
        }
    }

    @Nested
    @DisplayName("getActionTypeLabel()")
    class GetActionTypeLabel {

        @Test
        @DisplayName("should return 'Incident' for INCIDENT_CREATE")
        void shouldReturnIncidentForIncidentCreate() {
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .actionType(INCIDENT_CREATE)
                .build();

            assertThat(action.getActionTypeLabel()).isEqualTo("Incident");
        }

        @Test
        @DisplayName("should return 'Work Order' for WORK_ORDER_CREATE")
        void shouldReturnWorkOrderForWorkOrderCreate() {
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .actionType(WORK_ORDER_CREATE)
                .build();

            assertThat(action.getActionTypeLabel()).isEqualTo("Work Order");
        }

        @Test
        @DisplayName("should return 'Incident Update' for INCIDENT_UPDATE")
        void shouldReturnIncidentUpdateForIncidentUpdate() {
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .actionType(INCIDENT_UPDATE)
                .build();

            assertThat(action.getActionTypeLabel()).isEqualTo("Incident Update");
        }

        @Test
        @DisplayName("should return 'Work Order Update' for WORK_ORDER_UPDATE")
        void shouldReturnWorkOrderUpdateForWorkOrderUpdate() {
            PendingAction action = PendingAction.builder()
                .sessionId("session1")
                .userId("user1")
                .actionType(WORK_ORDER_UPDATE)
                .build();

            assertThat(action.getActionTypeLabel()).isEqualTo("Work Order Update");
        }
    }

    @Nested
    @DisplayName("Static Factory Methods")
    class StaticFactoryMethods {

        @Test
        @DisplayName("forIncidentCreation() should create INCIDENT_CREATE action with correct properties")
        void forIncidentCreationShouldCreateCorrectAction() {
            Instant expiresAt = Instant.now().plusSeconds(300);
            Map<String, Object> payload = Map.of("summary", "Server down");

            PendingAction action = PendingAction.forIncidentCreation(
                "session1",
                "user1",
                payload,
                "Preview text",
                expiresAt
            );

            assertThat(action.getSessionId()).isEqualTo("session1");
            assertThat(action.getUserId()).isEqualTo("user1");
            assertThat(action.getActionType()).isEqualTo(INCIDENT_CREATE);
            assertThat(action.getPayload()).isEqualTo(payload);
            assertThat(action.getPreview()).isEqualTo("Preview text");
            assertThat(action.getExpiresAt()).isEqualTo(expiresAt);
            assertThat(action.getStatus()).isEqualTo(PENDING);
            assertThat(action.getActionId()).hasSize(8);
        }

        @Test
        @DisplayName("forWorkOrderCreation() should create WORK_ORDER_CREATE action with correct properties")
        void forWorkOrderCreationShouldCreateCorrectAction() {
            Instant expiresAt = Instant.now().plusSeconds(300);
            Map<String, Object> payload = Map.of("task", "Install patch");

            PendingAction action = PendingAction.forWorkOrderCreation(
                "session2",
                "user2",
                payload,
                "Work order preview",
                expiresAt
            );

            assertThat(action.getSessionId()).isEqualTo("session2");
            assertThat(action.getUserId()).isEqualTo("user2");
            assertThat(action.getActionType()).isEqualTo(WORK_ORDER_CREATE);
            assertThat(action.getPayload()).isEqualTo(payload);
            assertThat(action.getPreview()).isEqualTo("Work order preview");
            assertThat(action.getExpiresAt()).isEqualTo(expiresAt);
        }

        @Test
        @DisplayName("forIncidentUpdate() should create INCIDENT_UPDATE action with correct properties")
        void forIncidentUpdateShouldCreateCorrectAction() {
            Instant expiresAt = Instant.now().plusSeconds(300);
            Map<String, Object> payload = Map.of("status", "Resolved");

            PendingAction action = PendingAction.forIncidentUpdate(
                "session3",
                "user3",
                payload,
                "Update preview",
                expiresAt
            );

            assertThat(action.getSessionId()).isEqualTo("session3");
            assertThat(action.getUserId()).isEqualTo("user3");
            assertThat(action.getActionType()).isEqualTo(INCIDENT_UPDATE);
            assertThat(action.getPayload()).isEqualTo(payload);
            assertThat(action.getPreview()).isEqualTo("Update preview");
            assertThat(action.getExpiresAt()).isEqualTo(expiresAt);
        }

        @Test
        @DisplayName("forWorkOrderUpdate() should create WORK_ORDER_UPDATE action with correct properties")
        void forWorkOrderUpdateShouldCreateCorrectAction() {
            Instant expiresAt = Instant.now().plusSeconds(300);
            Map<String, Object> payload = Map.of("assignedGroup", "Network Team");

            PendingAction action = PendingAction.forWorkOrderUpdate(
                "session4",
                "user4",
                payload,
                "Work order update preview",
                expiresAt
            );

            assertThat(action.getSessionId()).isEqualTo("session4");
            assertThat(action.getUserId()).isEqualTo("user4");
            assertThat(action.getActionType()).isEqualTo(WORK_ORDER_UPDATE);
            assertThat(action.getPayload()).isEqualTo(payload);
            assertThat(action.getPreview()).isEqualTo("Work order update preview");
            assertThat(action.getExpiresAt()).isEqualTo(expiresAt);
        }
    }

    @Nested
    @DisplayName("Enum Values")
    class EnumValues {

        @Test
        @DisplayName("ActionType should have all expected values")
        void actionTypeShouldHaveAllExpectedValues() {
            assertThat(PendingAction.ActionType.values())
                .containsExactly(
                    INCIDENT_CREATE,
                    WORK_ORDER_CREATE,
                    INCIDENT_UPDATE,
                    WORK_ORDER_UPDATE
                );
        }

        @Test
        @DisplayName("ActionStatus should have all expected values")
        void actionStatusShouldHaveAllExpectedValues() {
            assertThat(PendingAction.ActionStatus.values())
                .containsExactly(
                    PENDING,
                    CONFIRMED,
                    EXECUTED,
                    CANCELLED,
                    EXPIRED,
                    FAILED
                );
        }
    }

    @Nested
    @DisplayName("Builder with All Fields")
    class BuilderWithAllFields {

        @Test
        @DisplayName("should build action with all fields populated")
        void shouldBuildActionWithAllFields() {
            Instant stagedAt = Instant.parse("2025-01-15T10:00:00Z");
            Instant expiresAt = Instant.parse("2025-01-15T10:05:00Z");
            Map<String, Object> metadata = Map.of("key1", "value1", "key2", "value2");
            Map<String, Object> payload = Map.of("summary", "Test incident");

            PendingAction action = PendingAction.builder()
                .actionId("test1234")
                .sessionId("session1")
                .userId("user1")
                .actionType(INCIDENT_CREATE)
                .preview("Test preview")
                .payload(payload)
                .metadata(metadata)
                .stagedAt(stagedAt)
                .expiresAt(expiresAt)
                .status(PENDING)
                .resultMessage("Test message")
                .createdRecordId("INC000123")
                .build();

            assertThat(action.getActionId()).isEqualTo("test1234");
            assertThat(action.getSessionId()).isEqualTo("session1");
            assertThat(action.getUserId()).isEqualTo("user1");
            assertThat(action.getActionType()).isEqualTo(INCIDENT_CREATE);
            assertThat(action.getPreview()).isEqualTo("Test preview");
            assertThat(action.getPayload()).isEqualTo(payload);
            assertThat(action.getMetadata()).isEqualTo(metadata);
            assertThat(action.getStagedAt()).isEqualTo(stagedAt);
            assertThat(action.getExpiresAt()).isEqualTo(expiresAt);
            assertThat(action.getStatus()).isEqualTo(PENDING);
            assertThat(action.getResultMessage()).isEqualTo("Test message");
            assertThat(action.getCreatedRecordId()).isEqualTo("INC000123");
        }
    }
}
