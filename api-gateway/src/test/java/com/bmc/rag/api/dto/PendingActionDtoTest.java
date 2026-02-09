package com.bmc.rag.api.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PendingActionDto.
 */
class PendingActionDtoTest {

    @Test
    void builder_shouldCreateInstanceWithAllFields() {
        // Given
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(300);

        // When
        PendingActionDto dto = PendingActionDto.builder()
            .actionId("action-123")
            .actionType("CREATE_INCIDENT")
            .preview("Create incident with summary...")
            .stagedAt(now)
            .expiresAt(expires)
            .status("PENDING")
            .secondsUntilExpiry(300L)
            .build();

        // Then
        assertThat(dto.getActionId()).isEqualTo("action-123");
        assertThat(dto.getActionType()).isEqualTo("CREATE_INCIDENT");
        assertThat(dto.getPreview()).isEqualTo("Create incident with summary...");
        assertThat(dto.getStagedAt()).isEqualTo(now);
        assertThat(dto.getExpiresAt()).isEqualTo(expires);
        assertThat(dto.getStatus()).isEqualTo("PENDING");
        assertThat(dto.getSecondsUntilExpiry()).isEqualTo(300L);
    }

    @Test
    void noArgsConstructor_shouldWork() {
        // When
        PendingActionDto dto = new PendingActionDto();

        // Then
        assertThat(dto).isNotNull();
    }

    @Test
    void allArgsConstructor_shouldWork() {
        // Given
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(300);

        // When
        PendingActionDto dto = new PendingActionDto(
            "action-123",
            "CREATE_INCIDENT",
            "Preview text",
            now,
            expires,
            "PENDING",
            300L
        );

        // Then
        assertThat(dto.getActionId()).isEqualTo("action-123");
        assertThat(dto.getActionType()).isEqualTo("CREATE_INCIDENT");
        assertThat(dto.getPreview()).isEqualTo("Preview text");
        assertThat(dto.getStagedAt()).isEqualTo(now);
        assertThat(dto.getExpiresAt()).isEqualTo(expires);
        assertThat(dto.getStatus()).isEqualTo("PENDING");
        assertThat(dto.getSecondsUntilExpiry()).isEqualTo(300L);
    }

    @Test
    void equalsAndHashCode_shouldWork() {
        // Given
        Instant now = Instant.now();
        PendingActionDto dto1 = PendingActionDto.builder()
            .actionId("action-123")
            .actionType("CREATE_INCIDENT")
            .stagedAt(now)
            .build();

        PendingActionDto dto2 = PendingActionDto.builder()
            .actionId("action-123")
            .actionType("CREATE_INCIDENT")
            .stagedAt(now)
            .build();

        // Then
        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
    }

    @Test
    void toString_shouldContainFields() {
        // Given
        PendingActionDto dto = PendingActionDto.builder()
            .actionId("action-123")
            .actionType("CREATE_INCIDENT")
            .status("PENDING")
            .build();

        // When
        String toString = dto.toString();

        // Then
        assertThat(toString).contains("action-123");
        assertThat(toString).contains("CREATE_INCIDENT");
        assertThat(toString).contains("PENDING");
    }

    @Test
    void setters_shouldUpdateFields() {
        // Given
        PendingActionDto dto = new PendingActionDto();
        Instant now = Instant.now();

        // When
        dto.setActionId("new-action");
        dto.setActionType("UPDATE_INCIDENT");
        dto.setPreview("New preview");
        dto.setStagedAt(now);
        dto.setExpiresAt(now.plusSeconds(600));
        dto.setStatus("EXPIRED");
        dto.setSecondsUntilExpiry(0L);

        // Then
        assertThat(dto.getActionId()).isEqualTo("new-action");
        assertThat(dto.getActionType()).isEqualTo("UPDATE_INCIDENT");
        assertThat(dto.getPreview()).isEqualTo("New preview");
        assertThat(dto.getStagedAt()).isEqualTo(now);
        assertThat(dto.getExpiresAt()).isEqualTo(now.plusSeconds(600));
        assertThat(dto.getStatus()).isEqualTo("EXPIRED");
        assertThat(dto.getSecondsUntilExpiry()).isEqualTo(0L);
    }
}
