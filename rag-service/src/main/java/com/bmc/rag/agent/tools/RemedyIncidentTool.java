package com.bmc.rag.agent.tools;

import com.bmc.rag.agent.confirmation.ConfirmationService;
import com.bmc.rag.agent.confirmation.PendingAction;
import com.bmc.rag.agent.security.AgenticRateLimiter;
import com.bmc.rag.agent.security.InputValidator;
import com.bmc.rag.connector.dto.IncidentCreationRequest;
import com.bmc.rag.store.service.VectorStoreService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LangChain4j Tool for Incident operations.
 * Provides search and staged creation capabilities via @Tool annotations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemedyIncidentTool {

    private final VectorStoreService vectorStoreService;
    private final ConfirmationService confirmationService;
    private final InputValidator inputValidator;
    private final AgenticRateLimiter rateLimiter;

    @Value("${agentic.duplicate-detection.similarity-threshold:0.85}")
    private double duplicateThreshold;

    // Thread-local context for current user/session
    private static final ThreadLocal<ToolContext> currentContext = new ThreadLocal<>();

    /**
     * Set the context for the current tool execution.
     */
    public static void setContext(String sessionId, String userId) {
        currentContext.set(new ToolContext(sessionId, userId));
    }

    /**
     * Clear the current context after tool execution.
     */
    public static void clearContext() {
        currentContext.remove();
    }

    /**
     * Get the current context.
     */
    private ToolContext getContext() {
        ToolContext ctx = currentContext.get();
        if (ctx == null) {
            throw new IllegalStateException("No context set for tool execution");
        }
        return ctx;
    }

    /**
     * Search for similar existing incidents.
     * Use this to find related incidents before creating new ones.
     *
     * @param query The search query describing the issue
     * @param maxResults Maximum number of results to return
     * @return Formatted list of similar incidents
     */
    @Tool("Search for similar existing incidents in BMC Remedy. Use this before creating a new incident to check for duplicates.")
    public String searchSimilarIncidents(
            @P("Description of the issue to search for") String query,
            @P("Maximum number of results (default 5)") Integer maxResults) {

        log.info("Searching for similar incidents: {}", query);

        int limit = maxResults != null ? Math.min(maxResults, 10) : 5;

        try {
            var results = vectorStoreService.searchByType(query, "Incident", limit, 0.3);

            if (results.isEmpty()) {
                return "No similar incidents found in the knowledge base.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Found ").append(results.size()).append(" similar incident(s):\n\n");

            for (var result : results) {
                sb.append("- **").append(result.getSourceId()).append("**: ");
                String title = result.getMetadata() != null ? result.getMetadata().get("title") : null;
                sb.append(title != null ? title : "No title");
                sb.append(" (Similarity: ").append(String.format("%.0f%%", result.getScore() * 100)).append(")\n");
                if (result.getTextSegment() != null && !result.getTextSegment().isEmpty()) {
                    String preview = result.getTextSegment().length() > 150
                        ? result.getTextSegment().substring(0, 150) + "..."
                        : result.getTextSegment();
                    sb.append("  ").append(preview.replace("\n", " ")).append("\n");
                }
                sb.append("\n");
            }

            // Check for potential duplicates
            long highSimilarityCount = results.stream()
                .filter(r -> r.getScore() >= duplicateThreshold)
                .count();

            if (highSimilarityCount > 0) {
                sb.append("\n⚠️ **Warning:** Found ").append(highSimilarityCount);
                sb.append(" highly similar incident(s) that may be duplicates.\n");
                sb.append("Consider reviewing these before creating a new incident.\n");
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("Error searching incidents: {}", e.getMessage(), e);
            return "Error searching for similar incidents: " + e.getMessage();
        }
    }

    /**
     * Stage an incident creation for user confirmation.
     * This does NOT create the incident immediately - it requires user confirmation.
     *
     * @param summary Brief summary of the incident (required, max 255 chars)
     * @param description Detailed description (required)
     * @param impact Impact level: 1=Extensive, 2=Significant, 3=Moderate, 4=Minor
     * @param urgency Urgency level: 1=Critical, 2=High, 3=Medium, 4=Low
     * @return Confirmation prompt for the user
     */
    @Tool("Stage a new incident for creation. The user must confirm before it's created. Returns a confirmation prompt.")
    public String stageIncidentCreation(
            @P("Brief summary of the incident (max 255 characters)") String summary,
            @P("Detailed description of the issue") String description,
            @P("Impact: 1=Extensive/Widespread, 2=Significant/Large, 3=Moderate/Limited, 4=Minor/Localized") Integer impact,
            @P("Urgency: 1=Critical, 2=High, 3=Medium, 4=Low") Integer urgency) {

        ToolContext ctx = getContext();
        log.info("Staging incident creation for user {} in session {}", ctx.userId(), ctx.sessionId());

        // Check rate limit
        if (rateLimiter.isRateLimited(ctx.userId())) {
            var status = rateLimiter.getStatus(ctx.userId());
            return String.format(
                "⚠️ **Rate limit exceeded.** You've reached the maximum of %d incident creations per hour. " +
                "Please wait before creating more incidents.",
                status.maxPerHour()
            );
        }

        // Validate inputs
        var summaryValidation = inputValidator.validateSummary(summary);
        if (!summaryValidation.valid()) {
            return "❌ **Validation Error:** " + String.join(", ", summaryValidation.errors());
        }

        var descValidation = inputValidator.validateDescription(description);
        if (!descValidation.valid()) {
            return "❌ **Validation Error:** " + String.join(", ", descValidation.errors());
        }

        if (!inputValidator.isValidImpact(impact)) {
            return "❌ **Validation Error:** Impact must be between 1 (Extensive) and 4 (Minor).";
        }

        if (!inputValidator.isValidUrgency(urgency)) {
            return "❌ **Validation Error:** Urgency must be between 1 (Critical) and 4 (Low).";
        }

        // Check for duplicates
        try {
            var duplicates = vectorStoreService.searchByType(summary + " " + description, "Incident", 3, duplicateThreshold);
            if (!duplicates.isEmpty()) {
                StringBuilder warning = new StringBuilder();
                warning.append("⚠️ **Potential duplicates found:**\n\n");
                for (var dup : duplicates) {
                    warning.append("- **").append(dup.getSourceId()).append("**: ");
                    String title = dup.getMetadata() != null ? dup.getMetadata().get("title") : null;
                    warning.append(title != null ? title : "No title");
                    warning.append(" (").append(String.format("%.0f%%", dup.getScore() * 100)).append(" similar)\n");
                }
                warning.append("\nPlease review these before proceeding. ");
                warning.append("If you still want to create the incident, include 'proceed anyway' in your request.\n");
                return warning.toString();
            }
        } catch (Exception e) {
            log.warn("Duplicate check failed: {}", e.getMessage());
            // Continue with creation - duplicate check is best-effort
        }

        // Build the request
        IncidentCreationRequest request = IncidentCreationRequest.builder()
            .summary(summaryValidation.sanitizedInput())
            .description(descValidation.sanitizedInput())
            .impact(impact)
            .urgency(urgency)
            .createdBy(ctx.userId())
            .sessionId(ctx.sessionId())
            .build();

        // Stage the action
        PendingAction action = confirmationService.stageIncidentCreation(
            ctx.sessionId(), ctx.userId(), request);

        // Return confirmation prompt
        return action.getConfirmationPrompt();
    }

    /**
     * Stage an incident creation with optional fields.
     */
    @Tool("Stage a new incident with additional details for creation. The user must confirm before it's created.")
    public String stageIncidentWithDetails(
            @P("Brief summary of the incident (max 255 characters)") String summary,
            @P("Detailed description of the issue") String description,
            @P("Impact: 1=Extensive, 2=Significant, 3=Moderate, 4=Minor") Integer impact,
            @P("Urgency: 1=Critical, 2=High, 3=Medium, 4=Low") Integer urgency,
            @P("Requester's first name (optional)") String requesterFirstName,
            @P("Requester's last name (optional)") String requesterLastName,
            @P("Category (optional)") String category,
            @P("Assigned group (optional)") String assignedGroup) {

        ToolContext ctx = getContext();
        log.info("Staging detailed incident creation for user {} in session {}", ctx.userId(), ctx.sessionId());

        // Check rate limit
        if (rateLimiter.isRateLimited(ctx.userId())) {
            var status = rateLimiter.getStatus(ctx.userId());
            return String.format(
                "⚠️ **Rate limit exceeded.** You've reached the maximum of %d incident creations per hour.",
                status.maxPerHour()
            );
        }

        // Validate required inputs
        var summaryValidation = inputValidator.validateSummary(summary);
        if (!summaryValidation.valid()) {
            return "❌ **Validation Error:** " + String.join(", ", summaryValidation.errors());
        }

        var descValidation = inputValidator.validateDescription(description);
        if (!descValidation.valid()) {
            return "❌ **Validation Error:** " + String.join(", ", descValidation.errors());
        }

        if (!inputValidator.isValidImpact(impact)) {
            return "❌ **Validation Error:** Impact must be between 1 and 4.";
        }

        if (!inputValidator.isValidUrgency(urgency)) {
            return "❌ **Validation Error:** Urgency must be between 1 and 4.";
        }

        // Build the request with optional fields
        IncidentCreationRequest.IncidentCreationRequestBuilder builder = IncidentCreationRequest.builder()
            .summary(summaryValidation.sanitizedInput())
            .description(descValidation.sanitizedInput())
            .impact(impact)
            .urgency(urgency)
            .createdBy(ctx.userId())
            .sessionId(ctx.sessionId());

        if (requesterFirstName != null && !requesterFirstName.isBlank()) {
            var nameValidation = inputValidator.validateName(requesterFirstName);
            if (nameValidation.valid()) {
                builder.requesterFirstName(nameValidation.sanitizedInput());
            }
        }

        if (requesterLastName != null && !requesterLastName.isBlank()) {
            var nameValidation = inputValidator.validateName(requesterLastName);
            if (nameValidation.valid()) {
                builder.requesterLastName(nameValidation.sanitizedInput());
            }
        }

        if (category != null && !category.isBlank()) {
            var catValidation = inputValidator.validateCategory(category);
            if (catValidation.valid()) {
                builder.categoryTier1(catValidation.sanitizedInput());
            }
        }

        if (assignedGroup != null && !assignedGroup.isBlank()) {
            var groupValidation = inputValidator.validateCategory(assignedGroup);
            if (groupValidation.valid()) {
                builder.assignedGroup(groupValidation.sanitizedInput());
            }
        }

        IncidentCreationRequest request = builder.build();

        // Stage the action
        PendingAction action = confirmationService.stageIncidentCreation(
            ctx.sessionId(), ctx.userId(), request);

        return action.getConfirmationPrompt();
    }

    /**
     * Get pending incident creations for the current session.
     */
    @Tool("List pending incident creations awaiting confirmation in the current session")
    public String listPendingIncidents() {
        ToolContext ctx = getContext();

        List<PendingAction> pending = confirmationService.getPendingActionsForSession(ctx.sessionId())
            .stream()
            .filter(a -> a.getActionType() == PendingAction.ActionType.INCIDENT_CREATE)
            .toList();

        if (pending.isEmpty()) {
            return "No pending incident creations in this session.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("**Pending Incident Creations:**\n\n");
        for (PendingAction action : pending) {
            sb.append("- **").append(action.getActionId()).append("**: ");
            sb.append(action.getPreview().split("\n")[2].replace("**Summary:** ", ""));
            sb.append("\n  _Expires: ").append(action.getExpiresAt()).append("_\n\n");
        }
        sb.append("\nTo confirm: `confirm <action_id>`\n");
        sb.append("To cancel: `cancel <action_id>`\n");

        return sb.toString();
    }

    /**
     * Context for tool execution.
     */
    public record ToolContext(String sessionId, String userId) {}
}
