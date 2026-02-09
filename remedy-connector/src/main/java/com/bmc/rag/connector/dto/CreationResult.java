package com.bmc.rag.connector.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Result DTO for record creation operations in BMC Remedy.
 * Contains the created record identifier and metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreationResult {

    /**
     * Whether the creation was successful.
     */
    private boolean success;

    /**
     * The created record's entry ID (internal AR System ID).
     */
    private String entryId;

    /**
     * The created record's display ID (e.g., INC000001234, WO0000001234).
     */
    private String recordId;

    /**
     * The form name the record was created in.
     */
    private String formName;

    /**
     * Timestamp of creation.
     */
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Error message if creation failed.
     */
    private String errorMessage;

    /**
     * Error code if creation failed (AR System error code).
     */
    private String errorCode;

    /**
     * Custom message for user display.
     */
    private String message;

    /**
     * Create a successful result.
     */
    public static CreationResult success(String entryId, String recordId, String formName) {
        return CreationResult.builder()
            .success(true)
            .entryId(entryId)
            .recordId(recordId)
            .formName(formName)
            .build();
    }

    /**
     * Create a failed result.
     */
    public static CreationResult failure(String errorMessage, String errorCode) {
        return CreationResult.builder()
            .success(false)
            .errorMessage(errorMessage)
            .errorCode(errorCode)
            .build();
    }

    /**
     * Create a failed result with just a message.
     */
    public static CreationResult failure(String errorMessage) {
        return failure(errorMessage, null);
    }

    /**
     * Get a user-friendly message about the result.
     */
    public String toUserMessage() {
        if (message != null) {
            return message;
        }
        if (success) {
            return recordId != null
                ? String.format("Successfully created %s", recordId)
                : "Successfully created record";
        } else {
            return String.format("Failed to create record: %s",
                errorMessage != null ? errorMessage : "Unknown error");
        }
    }

    /**
     * Return a new result with a custom message.
     */
    public CreationResult withMessage(String message) {
        this.message = message;
        return this;
    }
}
