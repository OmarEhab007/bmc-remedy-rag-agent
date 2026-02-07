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

    // Thread-local context for current user/session and conversation
    private static final ThreadLocal<ToolContext> currentContext = new ThreadLocal<>();

    /**
     * Set the context for the current tool execution.
     */
    public static void setContext(String sessionId, String userId) {
        currentContext.set(new ToolContext(sessionId, userId, null));
    }

    /**
     * Set the context with conversation history for context extraction.
     */
    public static void setContext(String sessionId, String userId, List<dev.langchain4j.data.message.ChatMessage> conversationHistory) {
        currentContext.set(new ToolContext(sessionId, userId, conversationHistory));
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
    @Tool("Stage a new incident for creation. CRITICAL: Extract the SPECIFIC technical issue from conversation history using format '[System] - [Symptom]'. Examples: 'Outlook email - cannot login with password', 'VPN - connection timeout error'. NEVER use vague summaries like 'email issue', 'this issue', or 'login problem' - these will be REJECTED.")
    public String stageIncidentCreation(
            @P("SPECIFIC summary: '[System Name] - [Exact Symptom]'. Example: 'CST Outlook email - cannot login with username/password'. NEVER use vague text like 'email issue' or 'with this issue'") String summary,
            @P("Detailed description extracted from conversation: what system, what error, what symptoms, what user tried") String description,
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

        // Truncate summary if too long (max 255 chars for BMC Remedy)
        String enrichedSummary = summary;
        if (enrichedSummary != null && enrichedSummary.length() > 250) {
            enrichedSummary = enrichedSummary.substring(0, 247) + "...";
            log.info("Summary truncated from {} to {} chars", summary.length(), enrichedSummary.length());
        }
        String enrichedDescription = description;
        var summaryValidation = inputValidator.validateSummary(enrichedSummary);
        if (!summaryValidation.valid()) {
            // Check if it's a vague summary error (LLM failed to extract issue)
            boolean isVagueSummaryError = summaryValidation.errors().stream()
                .anyMatch(e -> e.startsWith(InputValidator.VAGUE_SUMMARY_ERROR_PREFIX));

            if (isVagueSummaryError) {
                log.info("Vague summary detected, attempting to extract issue from conversation history");
                String extractedIssue = extractIssueFromConversation(ctx);

                if (extractedIssue != null) {
                    enrichedSummary = generateSummaryFromIssue(extractedIssue);
                    enrichedDescription = extractedIssue;
                    log.info("Enriched summary from conversation: '{}'", enrichedSummary);

                    // Re-validate with enriched summary
                    summaryValidation = inputValidator.validateSummary(enrichedSummary);
                    if (!summaryValidation.valid()) {
                        return "❌ **Validation Error:** " + String.join(", ", summaryValidation.errors());
                    }
                } else {
                    return "❌ **Validation Error:** " + String.join(", ", summaryValidation.errors());
                }
            } else {
                return "❌ **Validation Error:** " + String.join(", ", summaryValidation.errors());
            }
        }

        var descValidation = inputValidator.validateDescription(enrichedDescription);
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

        // Build the request with enriched/sanitized values
        IncidentCreationRequest request = IncidentCreationRequest.builder()
            .summary(summaryValidation.sanitizedInput())
            .description(descValidation.sanitizedInput() != null ? descValidation.sanitizedInput() : enrichedDescription)
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
     * Context for tool execution including conversation history.
     */
    public record ToolContext(
        String sessionId,
        String userId,
        List<dev.langchain4j.data.message.ChatMessage> conversationHistory
    ) {}

    /**
     * Extract the actual technical issue from conversation history.
     * Used when LLM passes vague summaries like "this issue".
     */
    private String extractIssueFromConversation(ToolContext ctx) {
        if (ctx.conversationHistory() == null || ctx.conversationHistory().isEmpty()) {
            return null;
        }

        StringBuilder issueDetails = new StringBuilder();
        String primaryIssue = null;

        // Scan conversation from oldest to newest, building up issue context
        for (int i = 0; i < ctx.conversationHistory().size(); i++) {
            var msg = ctx.conversationHistory().get(i);
            if (msg instanceof dev.langchain4j.data.message.UserMessage userMsg) {
                String content = userMsg.singleText();
                if (content == null) continue;

                // Skip messages that are just ticket creation requests
                String lower = content.toLowerCase();
                // Detect ticket creation requests using proximity: intent verb near ticket noun
                boolean isTicketRequest = lower.matches(
                    ".*(create|open|submit|log|raise|file).{0,30}(ticket|incident|issue).*") ||
                    lower.matches(
                    ".*(ticket|incident|issue).{0,30}(create|open|submit|log|raise|file).*");

                if (isTicketRequest && !containsProblemIndicators(content)) {
                    continue;
                }

                // Check if this message describes a problem
                if (containsProblemIndicators(content)) {
                    if (primaryIssue == null) {
                        primaryIssue = content;
                    }
                    // Add to issue details (avoiding duplicates)
                    if (!issueDetails.toString().contains(content)) {
                        if (issueDetails.length() > 0) {
                            issueDetails.append(" ");
                        }
                        issueDetails.append(content);
                    }
                }
            }
        }

        if (primaryIssue != null) {
            String fullIssue = issueDetails.toString().trim();
            log.info("Extracted issue from conversation: '{}' (full context: {} chars)",
                primaryIssue.length() > 60 ? primaryIssue.substring(0, 60) + "..." : primaryIssue,
                fullIssue.length());
            return fullIssue.isEmpty() ? primaryIssue : fullIssue;
        }

        return null;
    }

    /**
     * Check if text contains problem/issue indicators.
     */
    private boolean containsProblemIndicators(String text) {
        if (text == null || text.length() < 10) return false;
        String lower = text.toLowerCase();

        // Problem indicators
        return lower.contains("error") ||
               lower.contains("not working") ||
               lower.contains("can't") ||
               lower.contains("cannot") ||
               lower.contains("won't") ||
               lower.contains("doesn't") ||
               lower.contains("failed") ||
               lower.contains("crash") ||
               lower.contains("broken") ||
               lower.contains("issue") ||
               lower.contains("problem") ||
               lower.contains("stuck") ||
               lower.contains("slow") ||
               lower.contains("timeout") ||
               lower.contains("unable to") ||
               lower.contains("need help") ||
               lower.contains("not receiving") ||
               lower.contains("stopped") ||
               lower.contains("password") ||
               lower.contains("login") ||
               lower.contains("access") ||
               lower.contains("reset");
    }

    /**
     * Generate a clean summary from the extracted issue.
     */
    private String generateSummaryFromIssue(String issue) {
        if (issue == null) return null;

        // Take first sentence or first 100 chars
        String summary = issue;
        int periodIdx = issue.indexOf('.');
        if (periodIdx > 20 && periodIdx < 200) {
            summary = issue.substring(0, periodIdx);
        }

        // Truncate to max 200 chars for summary
        if (summary.length() > 200) {
            summary = summary.substring(0, 197) + "...";
        }

        // Clean up
        summary = summary.replaceAll("\\s+", " ").trim();

        return summary;
    }
}
