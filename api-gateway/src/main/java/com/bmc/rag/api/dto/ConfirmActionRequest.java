package com.bmc.rag.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for confirming or cancelling a pending action.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmActionRequest {

    /**
     * The action ID to confirm or cancel.
     */
    @NotBlank(message = "Action ID is required")
    @Size(min = 8, max = 8, message = "Action ID must be 8 characters")
    private String actionId;

    /**
     * The session ID (for ownership verification).
     */
    @NotBlank(message = "Session ID is required")
    private String sessionId;

    /**
     * Optional confirmation message from the user.
     */
    @Size(max = 500, message = "Message must not exceed 500 characters")
    private String message;
}
