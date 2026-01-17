package com.bmc.rag.store.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * JPA entity for auditing agentic actions.
 * Provides complete audit trail for all ticket creation attempts.
 */
@Entity
@Table(name = "action_audit",
       indexes = {
           @Index(name = "idx_action_audit_user", columnList = "user_id"),
           @Index(name = "idx_action_audit_session", columnList = "session_id"),
           @Index(name = "idx_action_audit_status", columnList = "status"),
           @Index(name = "idx_action_audit_created", columnList = "created_at")
       })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The action ID (8-character identifier).
     */
    @Column(name = "action_id", nullable = false, length = 8)
    private String actionId;

    /**
     * The session ID where the action was initiated.
     */
    @Column(name = "session_id", nullable = false)
    private String sessionId;

    /**
     * The user who initiated the action.
     */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /**
     * Type of action (INCIDENT_CREATE, WORK_ORDER_CREATE, etc.).
     */
    @Column(name = "action_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    /**
     * Current status of the action.
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ActionStatus status;

    /**
     * Summary of the request (e.g., incident summary).
     */
    @Column(name = "summary", length = 255)
    private String summary;

    /**
     * The created record ID (if successfully executed).
     */
    @Column(name = "record_id", length = 50)
    private String recordId;

    /**
     * Error message if the action failed.
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /**
     * JSON payload of the request (optional, may contain sensitive data).
     */
    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    /**
     * When the action was staged.
     */
    @Column(name = "staged_at", nullable = false)
    private Instant stagedAt;

    /**
     * When the action was confirmed/cancelled/expired.
     */
    @Column(name = "resolved_at")
    private Instant resolvedAt;

    /**
     * Audit timestamp (when this record was created).
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Last update timestamp.
     */
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    /**
     * IP address of the client (for security audit).
     */
    @Column(name = "client_ip", length = 45)
    private String clientIp;

    /**
     * User agent of the client (for debugging).
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Types of agentic actions (mirrors PendingAction.ActionType).
     */
    public enum ActionType {
        INCIDENT_CREATE,
        WORK_ORDER_CREATE,
        INCIDENT_UPDATE,
        WORK_ORDER_UPDATE
    }

    /**
     * Status of an action in the audit trail.
     */
    public enum ActionStatus {
        STAGED,         // Action was staged for confirmation
        CONFIRMED,      // User confirmed the action
        EXECUTED,       // Action was successfully executed
        CANCELLED,      // User cancelled the action
        EXPIRED,        // Action expired without confirmation
        FAILED          // Action execution failed
    }

    /**
     * Update the entity before saving.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Create an audit entry for a staged action.
     */
    public static ActionAuditEntity forStaged(
            String actionId,
            String sessionId,
            String userId,
            ActionType actionType,
            String summary) {
        return ActionAuditEntity.builder()
            .actionId(actionId)
            .sessionId(sessionId)
            .userId(userId)
            .actionType(actionType)
            .status(ActionStatus.STAGED)
            .summary(summary)
            .stagedAt(Instant.now())
            .build();
    }

    /**
     * Mark the action as executed.
     */
    public void markExecuted(String recordId) {
        this.status = ActionStatus.EXECUTED;
        this.recordId = recordId;
        this.resolvedAt = Instant.now();
    }

    /**
     * Mark the action as failed.
     */
    public void markFailed(String errorMessage) {
        this.status = ActionStatus.FAILED;
        this.errorMessage = errorMessage;
        this.resolvedAt = Instant.now();
    }

    /**
     * Mark the action as cancelled.
     */
    public void markCancelled() {
        this.status = ActionStatus.CANCELLED;
        this.resolvedAt = Instant.now();
    }

    /**
     * Mark the action as expired.
     */
    public void markExpired() {
        this.status = ActionStatus.EXPIRED;
        this.resolvedAt = Instant.now();
    }
}
