package com.bmc.rag.agent.confirmation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a staged action awaiting user confirmation.
 * Actions are session-bound and expire after a configurable TTL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingAction {

    /**
     * Unique action identifier for confirmation.
     */
    @Builder.Default
    private String actionId = UUID.randomUUID().toString().substring(0, 8);

    /**
     * Session ID this action belongs to.
     */
    private String sessionId;

    /**
     * User ID who initiated the action.
     */
    private String userId;

    /**
     * Type of action (INCIDENT_CREATE, WORK_ORDER_CREATE, etc.).
     */
    private ActionType actionType;

    /**
     * Human-readable preview of what will be created/modified.
     */
    private String preview;

    /**
     * The actual data payload for the action.
     */
    private Object payload;

    /**
     * Additional metadata about the action.
     */
    private Map<String, Object> metadata;

    /**
     * When the action was staged.
     */
    @Builder.Default
    private Instant stagedAt = Instant.now();

    /**
     * When the action expires (TTL).
     */
    private Instant expiresAt;

    /**
     * Current status of the action.
     */
    @Builder.Default
    private ActionStatus status = ActionStatus.PENDING;

    /**
     * Result message after execution (for confirmed/rejected actions).
     */
    private String resultMessage;

    /**
     * The created record ID (if action was executed successfully).
     */
    private String createdRecordId;

    /**
     * Types of agentic actions.
     */
    public enum ActionType {
        INCIDENT_CREATE,
        WORK_ORDER_CREATE,
        INCIDENT_UPDATE,
        WORK_ORDER_UPDATE
    }

    /**
     * Status of a pending action.
     */
    public enum ActionStatus {
        PENDING,        // Awaiting user confirmation
        CONFIRMED,      // User confirmed, execution pending
        EXECUTED,       // Successfully executed
        CANCELLED,      // User cancelled
        EXPIRED,        // TTL exceeded
        FAILED          // Execution failed
    }

    /**
     * Check if the action has expired.
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if the action is still valid for confirmation.
     */
    public boolean isConfirmable() {
        return status == ActionStatus.PENDING && !isExpired();
    }

    /**
     * Check if this action belongs to the given session and user.
     */
    public boolean belongsTo(String sessionId, String userId) {
        return this.sessionId != null && this.sessionId.equals(sessionId) &&
               this.userId != null && this.userId.equals(userId);
    }

    /**
     * Mark the action as confirmed.
     */
    public void confirm() {
        this.status = ActionStatus.CONFIRMED;
    }

    /**
     * Mark the action as cancelled.
     */
    public void cancel() {
        this.status = ActionStatus.CANCELLED;
    }

    /**
     * Mark the action as expired.
     */
    public void expire() {
        this.status = ActionStatus.EXPIRED;
    }

    /**
     * Mark the action as executed with result.
     */
    public void markExecuted(String recordId, String message) {
        this.status = ActionStatus.EXECUTED;
        this.createdRecordId = recordId;
        this.resultMessage = message;
    }

    /**
     * Mark the action as failed.
     */
    public void markFailed(String errorMessage) {
        this.status = ActionStatus.FAILED;
        this.resultMessage = errorMessage;
    }

    /**
     * Get a confirmation prompt for the user.
     */
    public String getConfirmationPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("I'll create this ").append(getActionTypeLabel()).append(":\n\n");
        sb.append(preview);
        sb.append("\n\n**To confirm, reply:** `confirm ").append(actionId).append("`\n");
        sb.append("**To cancel, reply:** `cancel ").append(actionId).append("`\n");
        sb.append("\n_This action will expire in 5 minutes._");
        return sb.toString();
    }

    /**
     * Get human-readable action type label.
     */
    public String getActionTypeLabel() {
        return switch (actionType) {
            case INCIDENT_CREATE -> "Incident";
            case WORK_ORDER_CREATE -> "Work Order";
            case INCIDENT_UPDATE -> "Incident Update";
            case WORK_ORDER_UPDATE -> "Work Order Update";
        };
    }

    /**
     * Create a pending action for incident creation.
     */
    public static PendingAction forIncidentCreation(
            String sessionId,
            String userId,
            Object payload,
            String preview,
            Instant expiresAt) {
        return PendingAction.builder()
            .sessionId(sessionId)
            .userId(userId)
            .actionType(ActionType.INCIDENT_CREATE)
            .payload(payload)
            .preview(preview)
            .expiresAt(expiresAt)
            .build();
    }

    /**
     * Create a pending action for work order creation.
     */
    public static PendingAction forWorkOrderCreation(
            String sessionId,
            String userId,
            Object payload,
            String preview,
            Instant expiresAt) {
        return PendingAction.builder()
            .sessionId(sessionId)
            .userId(userId)
            .actionType(ActionType.WORK_ORDER_CREATE)
            .payload(payload)
            .preview(preview)
            .expiresAt(expiresAt)
            .build();
    }
}
