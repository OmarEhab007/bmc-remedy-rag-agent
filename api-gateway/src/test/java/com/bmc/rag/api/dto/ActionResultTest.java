package com.bmc.rag.api.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ActionResult DTO.
 */
class ActionResultTest {

    @Test
    void builder_shouldCreateInstanceWithAllFields() {
        // When
        ActionResult result = ActionResult.builder()
            .success(true)
            .actionId("action-123")
            .status("EXECUTED")
            .recordId("INC000001")
            .message("Success")
            .timestamp(1234567890L)
            .build();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getActionId()).isEqualTo("action-123");
        assertThat(result.getStatus()).isEqualTo("EXECUTED");
        assertThat(result.getRecordId()).isEqualTo("INC000001");
        assertThat(result.getMessage()).isEqualTo("Success");
        assertThat(result.getTimestamp()).isEqualTo(1234567890L);
    }

    @Test
    void builder_shouldUseDefaultTimestamp() {
        // When
        ActionResult result = ActionResult.builder()
            .success(true)
            .build();

        // Then
        assertThat(result.getTimestamp()).isGreaterThan(0);
    }

    @Test
    void success_shouldCreateSuccessResult() {
        // When
        ActionResult result = ActionResult.success(
            "action-123",
            "EXECUTED",
            "INC000001",
            "Incident created successfully"
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getActionId()).isEqualTo("action-123");
        assertThat(result.getStatus()).isEqualTo("EXECUTED");
        assertThat(result.getRecordId()).isEqualTo("INC000001");
        assertThat(result.getMessage()).isEqualTo("Incident created successfully");
        assertThat(result.getTimestamp()).isGreaterThan(0);
    }

    @Test
    void failure_shouldCreateFailureResult() {
        // When
        ActionResult result = ActionResult.failure(
            "action-456",
            "Creation failed"
        );

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getActionId()).isEqualTo("action-456");
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getMessage()).isEqualTo("Creation failed");
        assertThat(result.getRecordId()).isNull();
    }

    @Test
    void cancelled_shouldCreateCancelledResult() {
        // When
        ActionResult result = ActionResult.cancelled(
            "action-789",
            "User cancelled the action"
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getActionId()).isEqualTo("action-789");
        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(result.getMessage()).isEqualTo("User cancelled the action");
    }

    @Test
    void noArgsConstructor_shouldWork() {
        // When
        ActionResult result = new ActionResult();

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    void allArgsConstructor_shouldWork() {
        // When
        ActionResult result = new ActionResult(
            true,
            "action-123",
            "EXECUTED",
            "INC000001",
            "Success",
            1234567890L
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getActionId()).isEqualTo("action-123");
        assertThat(result.getStatus()).isEqualTo("EXECUTED");
        assertThat(result.getRecordId()).isEqualTo("INC000001");
        assertThat(result.getMessage()).isEqualTo("Success");
        assertThat(result.getTimestamp()).isEqualTo(1234567890L);
    }

    @Test
    void equalsAndHashCode_shouldWork() {
        // Given
        ActionResult result1 = ActionResult.builder()
            .success(true)
            .actionId("action-123")
            .status("EXECUTED")
            .timestamp(1234567890L)
            .build();

        ActionResult result2 = ActionResult.builder()
            .success(true)
            .actionId("action-123")
            .status("EXECUTED")
            .timestamp(1234567890L)
            .build();

        // Then
        assertThat(result1).isEqualTo(result2);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }

    @Test
    void toString_shouldContainFields() {
        // Given
        ActionResult result = ActionResult.builder()
            .success(true)
            .actionId("action-123")
            .status("EXECUTED")
            .build();

        // When
        String toString = result.toString();

        // Then
        assertThat(toString).contains("action-123");
        assertThat(toString).contains("EXECUTED");
        assertThat(toString).contains("true");
    }
}
