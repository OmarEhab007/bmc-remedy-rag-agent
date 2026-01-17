package com.bmc.rag.api.controller;

import com.bmc.rag.agent.confirmation.ConfirmationService;
import com.bmc.rag.agent.confirmation.PendingAction;
import com.bmc.rag.agent.config.AgenticConfig;
import com.bmc.rag.agent.security.AgenticRateLimiter;
import com.bmc.rag.api.dto.ActionResult;
import com.bmc.rag.api.dto.ConfirmActionRequest;
import com.bmc.rag.api.dto.PendingActionDto;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API controller for agentic action management.
 * Handles confirmation and cancellation of staged actions.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/actions")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agentic.enabled", havingValue = "true")
public class ActionController {

    private final ConfirmationService confirmationService;
    private final AgenticRateLimiter rateLimiter;
    private final AgenticConfig agenticConfig;

    /**
     * Confirm a pending action.
     *
     * @param request The confirmation request
     * @param jwt The authenticated user (optional in dev mode)
     * @return The action result
     */
    @PostMapping("/confirm")
    @RateLimiter(name = "chat")
    public ResponseEntity<ActionResult> confirmAction(
            @Valid @RequestBody ConfirmActionRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = extractUserId(jwt, request.getSessionId());
        log.info("Action confirmation request for {} from user {}", request.getActionId(), userId);

        // Check rate limit
        if (rateLimiter.isRateLimited(userId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ActionResult.failure(request.getActionId(),
                    "Rate limit exceeded. Please wait before confirming more actions."));
        }

        var result = confirmationService.confirm(
            request.getActionId(),
            request.getSessionId(),
            userId
        );

        if (result.success()) {
            return ResponseEntity.ok(ActionResult.success(
                request.getActionId(),
                "EXECUTED",
                result.recordId(),
                result.message()
            ));
        } else {
            return ResponseEntity.badRequest()
                .body(ActionResult.failure(request.getActionId(), result.message()));
        }
    }

    /**
     * Cancel a pending action.
     *
     * @param request The cancellation request
     * @param jwt The authenticated user (optional in dev mode)
     * @return The action result
     */
    @DeleteMapping("/cancel")
    public ResponseEntity<ActionResult> cancelAction(
            @Valid @RequestBody ConfirmActionRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = extractUserId(jwt, request.getSessionId());
        log.info("Action cancellation request for {} from user {}", request.getActionId(), userId);

        var result = confirmationService.cancel(
            request.getActionId(),
            request.getSessionId(),
            userId
        );

        if (result.cancelled()) {
            return ResponseEntity.ok(ActionResult.cancelled(
                request.getActionId(),
                result.message()
            ));
        } else {
            return ResponseEntity.badRequest()
                .body(ActionResult.failure(request.getActionId(), result.message()));
        }
    }

    /**
     * Get pending actions for a session.
     *
     * @param sessionId The session ID
     * @param jwt The authenticated user
     * @return List of pending actions
     */
    @GetMapping("/pending")
    public ResponseEntity<List<PendingActionDto>> getPendingActions(
            @RequestParam String sessionId,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = extractUserId(jwt, sessionId);
        log.info("Getting pending actions for session {} / user {}", sessionId, userId);

        List<PendingAction> pending = confirmationService.getPendingActionsForSession(sessionId);

        // Filter to only show actions belonging to this user
        List<PendingActionDto> dtos = pending.stream()
            .filter(a -> a.belongsTo(sessionId, userId))
            .map(this::toDto)
            .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get a specific pending action.
     *
     * @param actionId The action ID
     * @param sessionId The session ID for ownership verification
     * @param jwt The authenticated user
     * @return The pending action details
     */
    @GetMapping("/{actionId}")
    public ResponseEntity<PendingActionDto> getAction(
            @PathVariable String actionId,
            @RequestParam String sessionId,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = extractUserId(jwt, sessionId);

        return confirmationService.getAction(actionId)
            .filter(a -> a.belongsTo(sessionId, userId))
            .map(this::toDto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get rate limit status for the current user.
     *
     * @param jwt The authenticated user
     * @return Rate limit status
     */
    @GetMapping("/rate-limit")
    public ResponseEntity<Map<String, Object>> getRateLimitStatus(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String sessionId) {

        String userId = extractUserId(jwt, sessionId);
        var status = rateLimiter.getStatus(userId);

        return ResponseEntity.ok(Map.of(
            "maxPerHour", status.maxPerHour(),
            "remaining", status.remaining(),
            "isLimited", status.isLimited()
        ));
    }

    /**
     * Check if agentic operations are enabled.
     *
     * @return Configuration status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAgenticStatus() {
        return ResponseEntity.ok(Map.of(
            "enabled", agenticConfig.isEnabled(),
            "confirmationRequired", agenticConfig.getConfirmation().isRequireConfirmation(),
            "timeoutMinutes", agenticConfig.getConfirmation().getTimeoutMinutes(),
            "maxCreationsPerHour", agenticConfig.getRateLimit().getMaxCreationsPerHour()
        ));
    }

    /**
     * Extract user ID from JWT or fallback to session-based ID.
     */
    private String extractUserId(Jwt jwt, String sessionId) {
        if (jwt != null) {
            // Try standard claims
            String sub = jwt.getClaimAsString("sub");
            if (sub != null) return sub;

            String email = jwt.getClaimAsString("email");
            if (email != null) return email;

            String preferredUsername = jwt.getClaimAsString("preferred_username");
            if (preferredUsername != null) return preferredUsername;
        }

        // Fallback to session-based user ID (for dev mode)
        return "session:" + (sessionId != null ? sessionId : "anonymous");
    }

    /**
     * Convert PendingAction to DTO.
     */
    private PendingActionDto toDto(PendingAction action) {
        long secondsUntilExpiry = action.getExpiresAt() != null
            ? Duration.between(Instant.now(), action.getExpiresAt()).getSeconds()
            : 0;

        return PendingActionDto.builder()
            .actionId(action.getActionId())
            .actionType(action.getActionType().name())
            .preview(action.getPreview())
            .stagedAt(action.getStagedAt())
            .expiresAt(action.getExpiresAt())
            .status(action.getStatus().name())
            .secondsUntilExpiry(Math.max(0, secondsUntilExpiry))
            .build();
    }
}
