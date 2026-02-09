package com.bmc.rag.connector.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CreationResult.
 */
class CreationResultTest {

    @Test
    void success_allFields_createsSuccessResult() {
        // When
        CreationResult result = CreationResult.success("entry-123", "INC000001", "HPD:Help Desk");

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getEntryId()).isEqualTo("entry-123");
        assertThat(result.getRecordId()).isEqualTo("INC000001");
        assertThat(result.getFormName()).isEqualTo("HPD:Help Desk");
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getCreatedAt()).isBeforeOrEqualTo(Instant.now());
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    void failure_messageAndCode_createsFailureResult() {
        // When
        CreationResult result = CreationResult.failure("Connection timeout", "ARERR 93");

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Connection timeout");
        assertThat(result.getErrorCode()).isEqualTo("ARERR 93");
        assertThat(result.getEntryId()).isNull();
        assertThat(result.getRecordId()).isNull();
        assertThat(result.getFormName()).isNull();
    }

    @Test
    void failure_messageOnly_createsFailureResultWithNullCode() {
        // When
        CreationResult result = CreationResult.failure("Validation error");

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Validation error");
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    void toUserMessage_successWithoutCustomMessage_returnsDefaultMessage() {
        // Given
        CreationResult result = CreationResult.success("entry-123", "INC000001", "HPD:Help Desk");

        // When
        String message = result.toUserMessage();

        // Then
        assertThat(message).isEqualTo("Successfully created INC000001");
    }

    @Test
    void toUserMessage_successWithCustomMessage_returnsCustomMessage() {
        // Given
        CreationResult result = CreationResult.success("entry-123", "INC000001", "HPD:Help Desk")
            .withMessage("Your incident has been created successfully!");

        // When
        String message = result.toUserMessage();

        // Then
        assertThat(message).isEqualTo("Your incident has been created successfully!");
    }

    @Test
    void toUserMessage_failureWithoutCustomMessage_returnsErrorMessage() {
        // Given
        CreationResult result = CreationResult.failure("Connection timeout");

        // When
        String message = result.toUserMessage();

        // Then
        assertThat(message).isEqualTo("Failed to create record: Connection timeout");
    }

    @Test
    void toUserMessage_failureWithCustomMessage_returnsCustomMessage() {
        // Given
        CreationResult result = CreationResult.failure("Connection timeout")
            .withMessage("Unable to connect to Remedy server");

        // When
        String message = result.toUserMessage();

        // Then
        assertThat(message).isEqualTo("Unable to connect to Remedy server");
    }

    @Test
    void withMessage_setsCustomMessage_returnsThis() {
        // Given
        CreationResult result = CreationResult.success("entry-123", "INC000001", "HPD:Help Desk");

        // When
        CreationResult updated = result.withMessage("Custom message");

        // Then
        assertThat(updated).isSameAs(result);
        assertThat(updated.getMessage()).isEqualTo("Custom message");
    }

    @Test
    void builder_allFields_buildsCorrectly() {
        // Given
        Instant now = Instant.now();

        // When
        CreationResult result = CreationResult.builder()
            .success(true)
            .entryId("entry-456")
            .recordId("WO0000001")
            .formName("WOI:WorkOrder")
            .createdAt(now)
            .message("Work order created")
            .build();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getEntryId()).isEqualTo("entry-456");
        assertThat(result.getRecordId()).isEqualTo("WO0000001");
        assertThat(result.getFormName()).isEqualTo("WOI:WorkOrder");
        assertThat(result.getCreatedAt()).isEqualTo(now);
        assertThat(result.getMessage()).isEqualTo("Work order created");
    }

    @Test
    void builder_defaultCreatedAt_setsCurrentTime() {
        // When
        CreationResult result = CreationResult.builder()
            .success(true)
            .entryId("entry-123")
            .recordId("INC000001")
            .formName("HPD:Help Desk")
            .build();

        // Then
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getCreatedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void builder_errorFields_buildsFailureResult() {
        // When
        CreationResult result = CreationResult.builder()
            .success(false)
            .errorMessage("Invalid impact value")
            .errorCode("VALIDATION_ERROR")
            .build();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Invalid impact value");
        assertThat(result.getErrorCode()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void toUserMessage_nullRecordId_handlesGracefully() {
        // Given
        CreationResult result = CreationResult.builder()
            .success(true)
            .entryId("entry-123")
            .recordId(null)
            .build();

        // When
        String message = result.toUserMessage();

        // Then
        assertThat(message).isEqualTo("Successfully created record");
    }

    @Test
    void toUserMessage_nullErrorMessage_handlesGracefully() {
        // Given
        CreationResult result = CreationResult.builder()
            .success(false)
            .errorMessage(null)
            .build();

        // When
        String message = result.toUserMessage();

        // Then
        assertThat(message).isEqualTo("Failed to create record: Unknown error");
    }

    @Test
    void equals_sameObject_returnsTrue() {
        // Given
        CreationResult result = CreationResult.success("entry-123", "INC000001", "HPD:Help Desk");

        // Then
        assertThat(result).isEqualTo(result);
    }

    @Test
    void equals_sameValues_returnsTrue() {
        // Given - Use fixed createdAt to ensure equals() comparison is deterministic
        Instant fixedTime = Instant.parse("2024-01-01T00:00:00Z");
        CreationResult result1 = CreationResult.builder()
            .success(true).entryId("entry-123").recordId("INC000001")
            .formName("HPD:Help Desk").createdAt(fixedTime).build();
        CreationResult result2 = CreationResult.builder()
            .success(true).entryId("entry-123").recordId("INC000001")
            .formName("HPD:Help Desk").createdAt(fixedTime).build();

        // Then
        assertThat(result1).isEqualTo(result2);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }

    @Test
    void equals_differentEntryId_returnsFalse() {
        // Given - Use fixed createdAt so only entryId differs
        Instant fixedTime = Instant.parse("2024-01-01T00:00:00Z");
        CreationResult result1 = CreationResult.builder()
            .success(true).entryId("entry-123").recordId("INC000001")
            .formName("HPD:Help Desk").createdAt(fixedTime).build();
        CreationResult result2 = CreationResult.builder()
            .success(true).entryId("entry-456").recordId("INC000001")
            .formName("HPD:Help Desk").createdAt(fixedTime).build();

        // Then
        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    void toString_includesKeyFields() {
        // Given
        CreationResult result = CreationResult.success("entry-123", "INC000001", "HPD:Help Desk");

        // When
        String str = result.toString();

        // Then
        assertThat(str).contains("success=true");
        assertThat(str).contains("entry-123");
        assertThat(str).contains("INC000001");
    }

    @Test
    void noArgsConstructor_createsObject() {
        // When
        CreationResult result = new CreationResult();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void allArgsConstructor_setsAllFields() {
        // Given
        Instant now = Instant.now();

        // When
        CreationResult result = new CreationResult(
            true,
            "entry-123",
            "INC000001",
            "HPD:Help Desk",
            now,
            null,
            null,
            "Custom message"
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getEntryId()).isEqualTo("entry-123");
        assertThat(result.getRecordId()).isEqualTo("INC000001");
        assertThat(result.getMessage()).isEqualTo("Custom message");
    }

    @Test
    void failure_bothOverloads_workCorrectly() {
        // When
        CreationResult result1 = CreationResult.failure("Error message", "ERR001");
        CreationResult result2 = CreationResult.failure("Error message");

        // Then
        assertThat(result1.getErrorCode()).isEqualTo("ERR001");
        assertThat(result2.getErrorCode()).isNull();
        assertThat(result1.getErrorMessage()).isEqualTo(result2.getErrorMessage());
    }
}
