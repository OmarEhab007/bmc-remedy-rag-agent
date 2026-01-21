package com.bmc.rag.agent.damee;

import com.bmc.rag.agent.confirmation.ConfirmationService;
import com.bmc.rag.agent.confirmation.PendingAction;
import com.bmc.rag.agent.damee.GuidedCreationState.Phase;
import com.bmc.rag.agent.damee.ServiceIntentMatcher.ServiceMatchResult;
import com.bmc.rag.connector.dto.WorkOrderCreationRequest;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates guided service request creation.
 * Manages conversation state and guides users through the service selection and field collection process.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuidedServiceCreator {

    private final ServiceIntentMatcher intentMatcher;
    private final DameeServiceCatalog catalog;
    private final ConfirmationService confirmationService;

    /**
     * Session states keyed by session ID.
     */
    private final Map<String, GuidedCreationState> sessionStates = new ConcurrentHashMap<>();

    /**
     * Process a message in the guided flow.
     *
     * @param sessionId The session ID
     * @param userId The user ID
     * @param message The user's message
     * @return Response to send back to the user
     */
    public GuidedResponse processMessage(String sessionId, String userId, String message) {
        // Get or create session state
        GuidedCreationState state = sessionStates.get(sessionId);

        if (state == null || !state.isActive() || state.isExpired()) {
            // Start new guided flow
            state = GuidedCreationState.initial(sessionId, userId);
            sessionStates.put(sessionId, state);
        }

        log.debug("Processing message for session {} in phase {}", sessionId, state.getPhase());

        return switch (state.getPhase()) {
            case INITIAL -> handleInitialQuery(state, message);
            case AWAITING_SERVICE_SELECTION -> handleServiceSelection(state, message);
            case CONFIRMING_SERVICE -> handleServiceConfirmation(state, message);
            case GATHERING_FIELDS -> handleFieldInput(state, message);
            case AWAITING_CONFIRMATION -> handleFinalConfirmation(state, message);
            default -> GuidedResponse.error("Unexpected state. Please start over with your request.");
        };
    }

    /**
     * Handle initial query - detect intent and match service.
     */
    private GuidedResponse handleInitialQuery(GuidedCreationState state, String message) {
        state.setOriginalQuery(message);

        // Try to match service
        ServiceMatchResult match = intentMatcher.matchService(message);

        if (match.isHighConfidence()) {
            // High confidence - confirm with user
            DameeService service = match.getService();
            state.setPhase(Phase.CONFIRMING_SERVICE);
            state.setSelectedService(service);
            sessionStates.put(state.getSessionId(), state);

            return GuidedResponse.builder()
                    .message(match.getDisplayMessage())
                    .options(List.of(
                            new GuidedResponse.Option("proceed", "Yes, proceed"),
                            new GuidedResponse.Option("other", "No, show other options"),
                            new GuidedResponse.Option("cancel", "Cancel")
                    ))
                    .state(state)
                    .build();
        }

        if (match.needsClarification()) {
            // Multiple matches - ask user to select
            state.setPhase(Phase.AWAITING_SERVICE_SELECTION);
            state.setCandidateServices(match.getMatches());
            sessionStates.put(state.getSessionId(), state);

            return GuidedResponse.builder()
                    .message(match.getDisplayMessage())
                    .state(state)
                    .build();
        }

        // No match - show categories
        return GuidedResponse.builder()
                .message(match.getDisplayMessage())
                .showCategories(true)
                .state(state)
                .build();
    }

    /**
     * Handle service selection from multiple options.
     */
    private GuidedResponse handleServiceSelection(GuidedCreationState state, String message) {
        String input = message.trim().toLowerCase();

        // Check for cancel
        if (input.equals("cancel") || input.equals("إلغاء")) {
            state.markCancelled();
            sessionStates.remove(state.getSessionId());
            return GuidedResponse.cancelled();
        }

        // Try to parse as number
        try {
            int selection = Integer.parseInt(message.trim());
            if (selection >= 1 && selection <= state.getCandidateServices().size()) {
                DameeService selected = state.getCandidateServices().get(selection - 1);
                return transitionToGatheringFields(state, selected);
            }
        } catch (NumberFormatException ignored) {
        }

        // Try to match by service name
        for (DameeService candidate : state.getCandidateServices()) {
            if (candidate.getNameEn().toLowerCase().contains(input) ||
                    (candidate.getNameAr() != null && candidate.getNameAr().contains(message))) {
                return transitionToGatheringFields(state, candidate);
            }
        }

        // Could not parse - try again with original query
        ServiceMatchResult match = intentMatcher.matchService(message);
        if (match.isHighConfidence()) {
            return transitionToGatheringFields(state, match.getService());
        }

        return GuidedResponse.builder()
                .message("I didn't understand your selection. Please enter a number (1-" +
                        state.getCandidateServices().size() + ") or describe your need more specifically.")
                .state(state)
                .build();
    }

    /**
     * Handle service confirmation response.
     */
    private GuidedResponse handleServiceConfirmation(GuidedCreationState state, String message) {
        String input = message.trim().toLowerCase();

        // Positive responses
        if (input.matches("(?i)(yes|proceed|نعم|موافق|1|y)")) {
            return transitionToGatheringFields(state, state.getSelectedService());
        }

        // Negative - show other options
        if (input.matches("(?i)(no|other|لا|أخرى|2|n)")) {
            // Show service categories
            state.setPhase(Phase.INITIAL);
            state.setSelectedService(null);
            sessionStates.put(state.getSessionId(), state);

            return GuidedResponse.builder()
                    .message(intentMatcher.getServiceCategoriesDisplay() +
                            "\n\nWhich category, or describe what you need?")
                    .showCategories(true)
                    .state(state)
                    .build();
        }

        // Cancel
        if (input.matches("(?i)(cancel|إلغاء|3)")) {
            state.markCancelled();
            sessionStates.remove(state.getSessionId());
            return GuidedResponse.cancelled();
        }

        // Unknown response
        return GuidedResponse.builder()
                .message("Please respond with 'Yes' to proceed, 'No' to see other options, or 'Cancel'.")
                .options(List.of(
                        new GuidedResponse.Option("proceed", "Yes, proceed"),
                        new GuidedResponse.Option("other", "No, show other options"),
                        new GuidedResponse.Option("cancel", "Cancel")
                ))
                .state(state)
                .build();
    }

    /**
     * Handle field input during gathering phase.
     */
    private GuidedResponse handleFieldInput(GuidedCreationState state, String message) {
        String input = message.trim();

        // Check for cancel
        if (input.toLowerCase().matches("(?i)(cancel|إلغاء)")) {
            state.markCancelled();
            sessionStates.remove(state.getSessionId());
            return GuidedResponse.cancelled();
        }

        // Check for back/skip
        if (input.toLowerCase().matches("(?i)(back|skip|تخطي)")) {
            // For now, just ask again
            return askForCurrentField(state);
        }

        // Validate and record the field value
        String currentField = state.getCurrentFieldName();
        if (currentField != null) {
            // Basic validation (not empty)
            if (input.isBlank()) {
                return GuidedResponse.builder()
                        .message("Please provide a value for " + currentField + ".")
                        .state(state)
                        .build();
            }

            // Record the value
            state.recordFieldValue(currentField, input);
            sessionStates.put(state.getSessionId(), state);

            // Check if all fields are collected
            if (state.areAllFieldsCollected()) {
                return transitionToConfirmation(state);
            }

            // Ask for next field
            return askForCurrentField(state);
        }

        return GuidedResponse.error("Unexpected state in field collection.");
    }

    /**
     * Handle final confirmation before submission.
     * If there's a pending action, executes it through ConfirmationService.
     */
    private GuidedResponse handleFinalConfirmation(GuidedCreationState state, String message) {
        String input = message.trim().toLowerCase();

        // Confirm submission
        if (input.matches("(?i)(confirm|yes|submit|موافق|نعم|تأكيد)")) {
            // Check if we have a pending action to confirm
            String actionId = state.getPendingActionId();
            if (actionId != null) {
                return executeConfirmedAction(state, actionId);
            }
            // No pending action yet, stage first
            return submitRequest(state);
        }

        // Cancel
        if (input.matches("(?i)(cancel|no|إلغاء|لا)")) {
            // If there's a pending action, cancel it through ConfirmationService
            String actionId = state.getPendingActionId();
            if (actionId != null) {
                try {
                    confirmationService.cancel(actionId, state.getSessionId(), state.getUserId());
                } catch (Exception e) {
                    log.warn("Failed to cancel pending action {}: {}", actionId, e.getMessage());
                }
            }
            state.markCancelled();
            sessionStates.remove(state.getSessionId());
            return GuidedResponse.cancelled();
        }

        // Edit - go back to gathering
        if (input.matches("(?i)(edit|modify|تعديل)")) {
            // Cancel any existing pending action
            String actionId = state.getPendingActionId();
            if (actionId != null) {
                try {
                    confirmationService.cancel(actionId, state.getSessionId(), state.getUserId());
                    state.setPendingActionId(null);
                } catch (Exception e) {
                    log.warn("Failed to cancel pending action for edit: {}", e.getMessage());
                }
            }
            state.setPhase(Phase.GATHERING_FIELDS);
            state.getCollectedFields().clear();
            state.setCurrentFieldIndex(0);
            state.setCurrentFieldName(state.getNextRequiredField());
            sessionStates.put(state.getSessionId(), state);
            return askForCurrentField(state);
        }

        return GuidedResponse.builder()
                .message("Please respond with 'Confirm' to submit, 'Cancel' to cancel, or 'Edit' to modify your answers.")
                .options(List.of(
                        new GuidedResponse.Option("confirm", "Confirm"),
                        new GuidedResponse.Option("edit", "Edit"),
                        new GuidedResponse.Option("cancel", "Cancel")
                ))
                .state(state)
                .build();
    }

    /**
     * Execute a confirmed action through ConfirmationService.
     */
    private GuidedResponse executeConfirmedAction(GuidedCreationState state, String actionId) {
        try {
            ConfirmationService.ConfirmationResult result = confirmationService.confirm(
                    actionId,
                    state.getSessionId(),
                    state.getUserId()
            );

            if (result.success()) {
                state.markSubmitted(result.recordId());
                sessionStates.remove(state.getSessionId());

                DameeService service = state.getSelectedService();
                String message = String.format(
                        "✅ **Request Submitted Successfully!**\n\n" +
                                "**Request Number:** %s\n" +
                                "**Service:** %s\n" +
                                "**Status:** Pending Processing\n\n" +
                                "You can track this request at: %s\n\n" +
                                "Would you like me to help with anything else?",
                        result.recordId(),
                        service != null ? service.getNameEn() : "Service Request",
                        service != null && service.getUrl() != null ? service.getUrl() : "https://itsmweb.mewa.gov.sa/jahz/index.html"
                );

                return GuidedResponse.builder()
                        .message(message)
                        .submitted(true)
                        .requestNumber(result.recordId())
                        .state(state)
                        .build();
            } else {
                log.error("Action confirmation failed: {}", result.message());
                return GuidedResponse.error("Failed to submit request: " + result.message());
            }

        } catch (Exception e) {
            log.error("Exception during action confirmation: {}", e.getMessage(), e);
            return GuidedResponse.error("Failed to submit request: " + e.getMessage());
        }
    }

    // ========================
    // State Transitions
    // ========================

    /**
     * Transition to gathering fields phase.
     */
    private GuidedResponse transitionToGatheringFields(GuidedCreationState state, DameeService service) {
        state.startGatheringFields(service);
        sessionStates.put(state.getSessionId(), state);

        String intro = String.format(
                "Great! Starting request for: **%s**\n\n" +
                        "I'll guide you through the required information.\n\n",
                service.getNameEn());

        return GuidedResponse.builder()
                .message(intro + getFieldPrompt(state))
                .state(state)
                .build();
    }

    /**
     * Transition to confirmation phase.
     */
    private GuidedResponse transitionToConfirmation(GuidedCreationState state) {
        state.setPhase(Phase.AWAITING_CONFIRMATION);
        sessionStates.put(state.getSessionId(), state);

        String preview = buildSubmissionPreview(state);

        return GuidedResponse.builder()
                .message(preview)
                .options(List.of(
                        new GuidedResponse.Option("confirm", "Confirm"),
                        new GuidedResponse.Option("edit", "Edit"),
                        new GuidedResponse.Option("cancel", "Cancel")
                ))
                .state(state)
                .build();
    }

    /**
     * Submit the request through ConfirmationService.
     * Creates a WorkOrderCreationRequest and stages it for user confirmation.
     */
    private GuidedResponse submitRequest(GuidedCreationState state) {
        DameeService service = state.getSelectedService();
        Map<String, String> fields = state.getCollectedFields();

        // Build description from collected fields
        StringBuilder description = new StringBuilder();
        description.append("Service Request: ").append(service.getNameEn()).append("\n");
        description.append("Service ID: ").append(service.getServiceId()).append("\n\n");
        description.append("Original Query: ").append(state.getOriginalQuery()).append("\n\n");
        description.append("Collected Information:\n");
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            description.append("- ").append(formatFieldName(entry.getKey())).append(": ").append(entry.getValue()).append("\n");
        }

        // Build WorkOrderCreationRequest
        WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                .summary("Service Request: " + service.getNameEn())
                .description(description.toString())
                .workOrderType(3) // Move/Add/Change - default for service requests
                .priority(2) // Medium - default
                .categoryTier1(service.getCategory())
                .categoryTier2(service.getSubcategory())
                .createdBy(state.getUserId())
                .sessionId(state.getSessionId())
                .build();

        try {
            // Stage through ConfirmationService
            PendingAction pendingAction = confirmationService.stageWorkOrderCreation(
                    state.getSessionId(),
                    state.getUserId(),
                    request
            );

            // Update state to track the pending action
            state.setPendingActionId(pendingAction.getActionId());
            sessionStates.put(state.getSessionId(), state);

            String message = String.format(
                    "📋 **Service Request Staged for Confirmation**\n\n" +
                            "**Service:** %s\n" +
                            "**Action ID:** %s\n" +
                            "**Status:** Awaiting Confirmation\n\n" +
                            "The request has been staged and requires your confirmation.\n" +
                            "Reply **'confirm'** to submit or **'cancel'** to discard.\n\n" +
                            "**Preview:**\n%s",
                    service.getNameEn(),
                    pendingAction.getActionId(),
                    pendingAction.getPreview()
            );

            return GuidedResponse.builder()
                    .message(message)
                    .pendingActionId(pendingAction.getActionId())
                    .state(state)
                    .options(List.of(
                            new GuidedResponse.Option("confirm", "Confirm & Submit"),
                            new GuidedResponse.Option("cancel", "Cancel")
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Failed to stage work order creation: {}", e.getMessage(), e);
            return GuidedResponse.error("Failed to stage request: " + e.getMessage());
        }
    }

    // ========================
    // Helper Methods
    // ========================

    /**
     * Ask for the current field value.
     */
    private GuidedResponse askForCurrentField(GuidedCreationState state) {
        return GuidedResponse.builder()
                .message(getFieldPrompt(state))
                .state(state)
                .build();
    }

    /**
     * Get the prompt for the current field.
     */
    private String getFieldPrompt(GuidedCreationState state) {
        String field = state.getCurrentFieldName();
        if (field == null) {
            return "All required information has been collected.";
        }

        int total = state.getSelectedService().getRequiredFields() != null ?
                state.getSelectedService().getRequiredFields().size() : 0;
        int current = state.getCurrentFieldIndex() + 1;

        return String.format("**Question %d/%d:** %s",
                current, total, getFieldQuestion(field));
    }

    /**
     * Get a human-readable question for a field.
     */
    private String getFieldQuestion(String fieldName) {
        return switch (fieldName.toLowerCase()) {
            case "description" -> "Please describe your request in detail.";
            case "justification" -> "What is the business justification for this request?";
            case "applicationname" -> "Which application is this for?";
            case "serviceoption" -> "Which service option do you need?";
            case "softwarename" -> "Which software do you need installed?";
            case "vpntype" -> "What type of VPN access do you need?";
            case "devicetype" -> "What type of device is this for?";
            case "requestdate" -> "When do you need this?";
            case "casedetails" -> "Please provide the case details.";
            case "datarequirements" -> "What data or requirements do you need?";
            default -> "Please provide: " + formatFieldName(fieldName);
        };
    }

    /**
     * Format a field name for display.
     */
    private String formatFieldName(String fieldName) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (i == 0) {
                result.append(Character.toUpperCase(c));
            } else if (Character.isUpperCase(c)) {
                result.append(' ').append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Build a preview of the submission for confirmation.
     */
    private String buildSubmissionPreview(GuidedCreationState state) {
        DameeService service = state.getSelectedService();
        StringBuilder sb = new StringBuilder();

        sb.append("📋 **Service Request Summary**\n\n");
        sb.append(String.format("**Service:** %s\n", service.getNameEn()));
        sb.append(String.format("**Service ID:** %s\n\n", service.getServiceId()));

        sb.append("**Your Information:**\n");
        sb.append(state.getCollectedFieldsSummary());

        sb.append("\n**Approval Workflow:**\n");
        if (service.getWorkflow() != null) {
            for (int i = 0; i < service.getWorkflow().size(); i++) {
                DameeService.WorkflowStep step = service.getWorkflow().get(i);
                String icon = (i == 0) ? "🔵" : "⚪";
                sb.append(String.format("%s %d. %s\n", icon, i + 1, step.getDescription()));
            }
        } else {
            sb.append("Standard workflow\n");
        }

        if (service.isVipBypass()) {
            sb.append("\n✅ **VIP Status:** Manager approval may be bypassed.\n");
        }

        sb.append("\n⏱️ **Estimated Time:** 2-3 business days\n\n");
        sb.append("**Ready to submit?** Reply `confirm` or `cancel`");

        return sb.toString();
    }

    /**
     * Clear session state.
     */
    public void clearSession(String sessionId) {
        sessionStates.remove(sessionId);
    }

    /**
     * Check if a session has an active guided flow.
     */
    public boolean hasActiveFlow(String sessionId) {
        GuidedCreationState state = sessionStates.get(sessionId);
        return state != null && state.isActive() && !state.isExpired();
    }

    /**
     * Get the current state for a session.
     */
    public GuidedCreationState getState(String sessionId) {
        return sessionStates.get(sessionId);
    }

    // ========================
    // Response Class
    // ========================

    /**
     * Response from the guided service creator.
     */
    @Data
    @Builder
    public static class GuidedResponse {
        private String message;
        private List<Option> options;
        private boolean showCategories;
        private boolean submitted;
        private boolean cancelled;
        private String requestNumber;
        private String pendingActionId;
        private GuidedCreationState state;
        private String errorMessage;

        @Data
        @lombok.AllArgsConstructor
        public static class Option {
            private String value;
            private String label;
        }

        public static GuidedResponse cancelled() {
            return GuidedResponse.builder()
                    .message("Request cancelled. Let me know if you need help with anything else.")
                    .cancelled(true)
                    .build();
        }

        public static GuidedResponse error(String message) {
            return GuidedResponse.builder()
                    .errorMessage(message)
                    .message("❌ " + message)
                    .build();
        }

        public boolean isError() {
            return errorMessage != null;
        }
    }
}
