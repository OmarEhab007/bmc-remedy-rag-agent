package com.bmc.rag.agent.confirmation;

import com.bmc.rag.agent.security.AgenticRateLimiter;
import com.bmc.rag.connector.creator.IncidentCreator;
import com.bmc.rag.connector.creator.IncidentUpdater;
import com.bmc.rag.connector.creator.WorkOrderCreator;
import com.bmc.rag.connector.dto.CreationResult;
import com.bmc.rag.connector.dto.IncidentCreationRequest;
import com.bmc.rag.connector.dto.IncidentUpdateRequest;
import com.bmc.rag.connector.dto.WorkOrderCreationRequest;
import com.bmc.rag.store.entity.ActionAuditEntity;
import com.bmc.rag.store.repository.ActionAuditRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing staged actions with mandatory user confirmation.
 * Implements the confirmation workflow for agentic operations.
 */
@Slf4j
@Service
public class ConfirmationService {

    private final IncidentCreator incidentCreator;
    private final IncidentUpdater incidentUpdater;
    private final WorkOrderCreator workOrderCreator;
    private final ActionAuditRepository auditRepository;
    private final AgenticRateLimiter rateLimiter;
    private final Cache<String, PendingAction> pendingActions;
    private final Duration confirmationTimeout;

    public ConfirmationService(
            IncidentCreator incidentCreator,
            IncidentUpdater incidentUpdater,
            WorkOrderCreator workOrderCreator,
            ActionAuditRepository auditRepository,
            AgenticRateLimiter rateLimiter,
            @Value("${agentic.confirmation.timeout-minutes:5}") int timeoutMinutes) {
        this.incidentCreator = incidentCreator;
        this.incidentUpdater = incidentUpdater;
        this.workOrderCreator = workOrderCreator;
        this.auditRepository = auditRepository;
        this.rateLimiter = rateLimiter;
        this.confirmationTimeout = Duration.ofMinutes(timeoutMinutes);

        // Initialize cache with TTL slightly longer than timeout for cleanup margin
        this.pendingActions = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(timeoutMinutes + 1))
            .maximumSize(10_000)
            .recordStats()
            .evictionListener((key, value, cause) -> {
                if (value instanceof PendingAction action && action.getStatus() == PendingAction.ActionStatus.PENDING) {
                    log.debug("Action {} expired from cache (cause: {})", key, cause);
                }
            })
            .build();
    }

    /**
     * Stage an incident creation action for confirmation.
     *
     * @param sessionId The user's session ID
     * @param userId The user's ID
     * @param request The incident creation request
     * @return The pending action with confirmation details
     */
    @Transactional
    public PendingAction stageIncidentCreation(
            String sessionId,
            String userId,
            IncidentCreationRequest request) {

        log.info("Staging incident creation for user {} in session {}", userId, sessionId);

        Instant expiresAt = Instant.now().plus(confirmationTimeout);
        String preview = request.toPreviewString();

        PendingAction action = PendingAction.forIncidentCreation(
            sessionId, userId, request, preview, expiresAt);

        pendingActions.put(action.getActionId(), action);

        // Persist audit record
        ActionAuditEntity audit = ActionAuditEntity.forStaged(
            action.getActionId(),
            sessionId,
            userId,
            ActionAuditEntity.ActionType.INCIDENT_CREATE,
            preview
        );
        auditRepository.save(audit);

        log.info("Staged action {} expires at {}", action.getActionId(), expiresAt);

        return action;
    }

    /**
     * Stage a work order creation action for confirmation.
     *
     * @param sessionId The user's session ID
     * @param userId The user's ID
     * @param request The work order creation request
     * @return The pending action with confirmation details
     */
    @Transactional
    public PendingAction stageWorkOrderCreation(
            String sessionId,
            String userId,
            WorkOrderCreationRequest request) {

        log.info("Staging work order creation for user {} in session {}", userId, sessionId);

        Instant expiresAt = Instant.now().plus(confirmationTimeout);
        String preview = request.toPreviewString();

        PendingAction action = PendingAction.forWorkOrderCreation(
            sessionId, userId, request, preview, expiresAt);

        pendingActions.put(action.getActionId(), action);

        // Persist audit record
        ActionAuditEntity audit = ActionAuditEntity.forStaged(
            action.getActionId(),
            sessionId,
            userId,
            ActionAuditEntity.ActionType.WORK_ORDER_CREATE,
            preview
        );
        auditRepository.save(audit);

        log.info("Staged action {} expires at {}", action.getActionId(), expiresAt);

        return action;
    }

    /**
     * Stage an incident update action for confirmation.
     *
     * @param sessionId The user's session ID
     * @param userId The user's ID
     * @param request The incident update request
     * @return The pending action with confirmation details
     */
    @Transactional
    public PendingAction stageIncidentUpdate(
            String sessionId,
            String userId,
            IncidentUpdateRequest request) {

        log.info("Staging incident update for {} by user {} in session {}",
            request.getIncidentNumber(), userId, sessionId);

        Instant expiresAt = Instant.now().plus(confirmationTimeout);
        String preview = request.toPreviewString();

        PendingAction action = PendingAction.forIncidentUpdate(
            sessionId, userId, request, preview, expiresAt);

        pendingActions.put(action.getActionId(), action);

        // Persist audit record
        ActionAuditEntity audit = ActionAuditEntity.forStaged(
            action.getActionId(),
            sessionId,
            userId,
            ActionAuditEntity.ActionType.INCIDENT_UPDATE,
            preview
        );
        auditRepository.save(audit);

        log.info("Staged update action {} expires at {}", action.getActionId(), expiresAt);

        return action;
    }

    /**
     * Confirm and execute a pending action.
     *
     * @param actionId The action ID to confirm
     * @param sessionId The user's session ID
     * @param userId The user's ID
     * @return The execution result
     */
    public ConfirmationResult confirm(String actionId, String sessionId, String userId) {
        log.info("Confirming action {} for user {} in session {}", actionId, userId, sessionId);

        Optional<PendingAction> actionOpt = getAction(actionId);

        if (actionOpt.isEmpty()) {
            return ConfirmationResult.failure("Action not found or already expired");
        }

        PendingAction action = actionOpt.get();

        // Verify ownership
        if (!action.belongsTo(sessionId, userId)) {
            log.warn("Action {} does not belong to session {} / user {}", actionId, sessionId, userId);
            return ConfirmationResult.failure("Action does not belong to this session");
        }

        // Check if still confirmable
        if (!action.isConfirmable()) {
            if (action.isExpired()) {
                action.expire();
                return ConfirmationResult.failure("Action has expired");
            }
            return ConfirmationResult.failure("Action is no longer pending: " + action.getStatus());
        }

        // Mark as confirmed and execute
        action.confirm();
        return executeAction(action);
    }

    /**
     * Cancel a pending action.
     *
     * @param actionId The action ID to cancel
     * @param sessionId The user's session ID
     * @param userId The user's ID
     * @return The cancellation result
     */
    @Transactional
    public ConfirmationResult cancel(String actionId, String sessionId, String userId) {
        log.info("Cancelling action {} for user {} in session {}", actionId, userId, sessionId);

        Optional<PendingAction> actionOpt = getAction(actionId);

        if (actionOpt.isEmpty()) {
            return ConfirmationResult.failure("Action not found");
        }

        PendingAction action = actionOpt.get();

        // Verify ownership
        if (!action.belongsTo(sessionId, userId)) {
            log.warn("Action {} does not belong to session {} / user {}", actionId, sessionId, userId);
            return ConfirmationResult.failure("Action does not belong to this session");
        }

        // Check if cancellable
        if (action.getStatus() != PendingAction.ActionStatus.PENDING) {
            return ConfirmationResult.failure("Action cannot be cancelled: " + action.getStatus());
        }

        action.cancel();
        pendingActions.invalidate(actionId);

        // Update audit record
        auditRepository.findByActionId(actionId)
            .ifPresent(audit -> {
                audit.markCancelled();
                auditRepository.save(audit);
            });

        return ConfirmationResult.cancelled("Action cancelled successfully");
    }

    /**
     * Get a pending action by ID.
     */
    public Optional<PendingAction> getAction(String actionId) {
        PendingAction action = pendingActions.getIfPresent(actionId);
        return Optional.ofNullable(action);
    }

    /**
     * Get all pending actions for a session.
     */
    public List<PendingAction> getPendingActionsForSession(String sessionId) {
        return pendingActions.asMap().values().stream()
            .filter(action -> sessionId.equals(action.getSessionId()))
            .filter(action -> action.getStatus() == PendingAction.ActionStatus.PENDING)
            .filter(action -> !action.isExpired())
            .collect(Collectors.toList());
    }

    /**
     * Get all pending actions for a user.
     */
    public List<PendingAction> getPendingActionsForUser(String userId) {
        return pendingActions.asMap().values().stream()
            .filter(action -> userId.equals(action.getUserId()))
            .filter(action -> action.getStatus() == PendingAction.ActionStatus.PENDING)
            .filter(action -> !action.isExpired())
            .collect(Collectors.toList());
    }

    /**
     * Execute a confirmed action.
     */
    @Transactional
    private ConfirmationResult executeAction(PendingAction action) {
        try {
            CreationResult result = switch (action.getActionType()) {
                case INCIDENT_CREATE -> {
                    IncidentCreationRequest request = (IncidentCreationRequest) action.getPayload();
                    yield incidentCreator.createIncident(request);
                }
                case INCIDENT_UPDATE -> {
                    IncidentUpdateRequest request = (IncidentUpdateRequest) action.getPayload();
                    yield incidentUpdater.updateIncident(request);
                }
                case WORK_ORDER_CREATE -> {
                    WorkOrderCreationRequest request = (WorkOrderCreationRequest) action.getPayload();
                    yield workOrderCreator.createWorkOrder(request);
                }
                default -> CreationResult.failure("Unsupported action type: " + action.getActionType());
            };

            if (result.isSuccess()) {
                action.markExecuted(result.getRecordId(), result.toUserMessage());

                // Record rate limit ONLY on successful execution
                rateLimiter.recordAction(action.getUserId());

                // Update audit record
                auditRepository.findByActionId(action.getActionId())
                    .ifPresent(audit -> {
                        audit.markExecuted(result.getRecordId());
                        auditRepository.save(audit);
                    });

                log.info("Action {} executed successfully: {}", action.getActionId(), result.getRecordId());
                return ConfirmationResult.success(result.getRecordId(), result.toUserMessage());
            } else {
                action.markFailed(result.getErrorMessage());

                // Update audit record
                auditRepository.findByActionId(action.getActionId())
                    .ifPresent(audit -> {
                        audit.markFailed(result.getErrorMessage());
                        auditRepository.save(audit);
                    });

                log.error("Action {} failed: {}", action.getActionId(), result.getErrorMessage());
                return ConfirmationResult.failure(result.getErrorMessage());
            }
        } catch (Exception e) {
            String errorMessage = "Execution failed: " + e.getMessage();
            action.markFailed(errorMessage);

            // Update audit record
            auditRepository.findByActionId(action.getActionId())
                .ifPresent(audit -> {
                    audit.markFailed(errorMessage);
                    auditRepository.save(audit);
                });

            log.error("Action {} threw exception: {}", action.getActionId(), e.getMessage(), e);
            return ConfirmationResult.failure(errorMessage);
        } finally {
            // Keep the action in cache for audit trail, will be evicted by TTL
            pendingActions.put(action.getActionId(), action);
        }
    }

    /**
     * Get cache statistics for monitoring.
     */
    public CacheStats getCacheStats() {
        var stats = pendingActions.stats();
        return new CacheStats(
            pendingActions.estimatedSize(),
            stats.hitCount(),
            stats.missCount(),
            stats.evictionCount()
        );
    }

    /**
     * Result of a confirmation/cancellation operation.
     */
    public record ConfirmationResult(
        boolean success,
        boolean cancelled,
        String recordId,
        String message
    ) {
        public static ConfirmationResult success(String recordId, String message) {
            return new ConfirmationResult(true, false, recordId, message);
        }

        public static ConfirmationResult failure(String message) {
            return new ConfirmationResult(false, false, null, message);
        }

        public static ConfirmationResult cancelled(String message) {
            return new ConfirmationResult(false, true, null, message);
        }
    }

    /**
     * Cache statistics for monitoring.
     */
    public record CacheStats(
        long size,
        long hitCount,
        long missCount,
        long evictionCount
    ) {}
}
