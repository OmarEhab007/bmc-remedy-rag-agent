package com.bmc.rag.agent.damee;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * State object for tracking a guided service request creation.
 * Maintains conversation context and collected field values.
 */
@Data
@Builder
public class GuidedCreationState {

    /**
     * Current phase of the guided flow.
     */
    public enum Phase {
        INITIAL,                    // User just started, no service selected
        AWAITING_SERVICE_SELECTION, // Multiple matches, waiting for user to pick one
        CONFIRMING_SERVICE,         // High-confidence match, confirming with user
        GATHERING_FIELDS,           // Collecting required fields
        AWAITING_CONFIRMATION,      // All fields collected, awaiting final confirmation
        SUBMITTED,                  // Request submitted successfully
        CANCELLED                   // User cancelled the flow
    }

    /**
     * Current phase.
     */
    private Phase phase;

    /**
     * Selected Damee service (null if not yet selected).
     */
    private DameeService selectedService;

    /**
     * List of candidate services (for AWAITING_SERVICE_SELECTION phase).
     */
    private List<DameeService> candidateServices;

    /**
     * Collected field values.
     */
    @Builder.Default
    private Map<String, String> collectedFields = new HashMap<>();

    /**
     * Current field being collected.
     */
    private String currentFieldName;

    /**
     * Index of current field in the required fields list.
     */
    private int currentFieldIndex;

    /**
     * User ID for the request.
     */
    private String userId;

    /**
     * Session ID.
     */
    private String sessionId;

    /**
     * Original user query that started this flow.
     */
    private String originalQuery;

    /**
     * When this state was created.
     */
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * When this state was last updated.
     */
    @Builder.Default
    private Instant updatedAt = Instant.now();

    /**
     * Generated request/confirmation token.
     */
    private String confirmationToken;

    /**
     * Created request number (after submission).
     */
    private String requestNumber;

    /**
     * Pending action ID from ConfirmationService (for staged actions).
     */
    private String pendingActionId;

    // ========================
    // Factory Methods
    // ========================

    /**
     * Create initial state for a new session.
     */
    public static GuidedCreationState initial(String sessionId, String userId) {
        return GuidedCreationState.builder()
                .phase(Phase.INITIAL)
                .sessionId(sessionId)
                .userId(userId)
                .build();
    }

    /**
     * Create state for service selection (multiple candidates).
     */
    public static GuidedCreationState selectingService(String sessionId, String userId,
                                                        List<DameeService> candidates, String query) {
        return GuidedCreationState.builder()
                .phase(Phase.AWAITING_SERVICE_SELECTION)
                .sessionId(sessionId)
                .userId(userId)
                .candidateServices(candidates)
                .originalQuery(query)
                .build();
    }

    /**
     * Create state for confirming a high-confidence match.
     */
    public static GuidedCreationState confirmingService(String sessionId, String userId,
                                                         DameeService service, String query) {
        return GuidedCreationState.builder()
                .phase(Phase.CONFIRMING_SERVICE)
                .sessionId(sessionId)
                .userId(userId)
                .selectedService(service)
                .originalQuery(query)
                .build();
    }

    // ========================
    // State Transitions
    // ========================

    /**
     * Transition to gathering fields phase.
     */
    public void startGatheringFields(DameeService service) {
        this.phase = Phase.GATHERING_FIELDS;
        this.selectedService = service;
        this.currentFieldIndex = 0;
        this.currentFieldName = getNextRequiredField();
        this.updatedAt = Instant.now();
    }

    /**
     * Record a field value and advance to next field.
     */
    public void recordFieldValue(String fieldName, String value) {
        this.collectedFields.put(fieldName, value);
        this.currentFieldIndex++;
        this.currentFieldName = getNextRequiredField();
        this.updatedAt = Instant.now();

        // Check if all fields are collected
        if (areAllFieldsCollected()) {
            this.phase = Phase.AWAITING_CONFIRMATION;
        }
    }

    /**
     * Mark as submitted.
     */
    public void markSubmitted(String requestNumber) {
        this.phase = Phase.SUBMITTED;
        this.requestNumber = requestNumber;
        this.updatedAt = Instant.now();
    }

    /**
     * Mark as cancelled.
     */
    public void markCancelled() {
        this.phase = Phase.CANCELLED;
        this.updatedAt = Instant.now();
    }

    // ========================
    // Helper Methods
    // ========================

    /**
     * Get the next required field that hasn't been collected.
     */
    public String getNextRequiredField() {
        if (selectedService == null || selectedService.getRequiredFields() == null) {
            return null;
        }

        List<String> requiredFields = selectedService.getRequiredFields();
        for (String field : requiredFields) {
            if (!collectedFields.containsKey(field)) {
                return field;
            }
        }
        return null;
    }

    /**
     * Check if all required fields have been collected.
     */
    public boolean areAllFieldsCollected() {
        if (selectedService == null || selectedService.getRequiredFields() == null) {
            return true;
        }

        for (String field : selectedService.getRequiredFields()) {
            if (!collectedFields.containsKey(field)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get number of remaining fields to collect.
     */
    public int getRemainingFieldCount() {
        if (selectedService == null || selectedService.getRequiredFields() == null) {
            return 0;
        }
        return (int) selectedService.getRequiredFields().stream()
                .filter(f -> !collectedFields.containsKey(f))
                .count();
    }

    /**
     * Get a summary of collected information.
     */
    public String getCollectedFieldsSummary() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : collectedFields.entrySet()) {
            sb.append("- **").append(formatFieldName(entry.getKey())).append("**: ")
                    .append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Format a field name for display (camelCase to Title Case).
     */
    private String formatFieldName(String fieldName) {
        if (fieldName == null) return "";

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (i == 0) {
                result.append(Character.toUpperCase(c));
            } else if (Character.isUpperCase(c)) {
                result.append(' ').append(c);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Check if the state has expired (default: 30 minutes).
     */
    public boolean isExpired() {
        return Instant.now().isAfter(createdAt.plusSeconds(30 * 60));
    }

    /**
     * Check if this state is in an active (non-terminal) phase.
     */
    public boolean isActive() {
        return phase != Phase.SUBMITTED && phase != Phase.CANCELLED;
    }
}
