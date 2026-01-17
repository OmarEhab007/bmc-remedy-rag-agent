package com.bmc.rag.agent.tools;

import com.bmc.rag.agent.confirmation.ConfirmationService;
import com.bmc.rag.agent.confirmation.PendingAction;
import com.bmc.rag.agent.security.AgenticRateLimiter;
import com.bmc.rag.agent.security.InputValidator;
import com.bmc.rag.connector.dto.WorkOrderCreationRequest;
import com.bmc.rag.store.service.VectorStoreService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * LangChain4j Tool for Work Order operations.
 * Provides search and staged creation capabilities via @Tool annotations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemedyWorkOrderTool {

    private final VectorStoreService vectorStoreService;
    private final ConfirmationService confirmationService;
    private final InputValidator inputValidator;
    private final AgenticRateLimiter rateLimiter;

    @Value("${agentic.duplicate-detection.similarity-threshold:0.85}")
    private double duplicateThreshold;

    // Re-use context from RemedyIncidentTool
    private RemedyIncidentTool.ToolContext getContext() {
        // Access the thread-local context set by AgenticAssistantService
        try {
            var field = RemedyIncidentTool.class.getDeclaredField("currentContext");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            ThreadLocal<RemedyIncidentTool.ToolContext> contextHolder =
                (ThreadLocal<RemedyIncidentTool.ToolContext>) field.get(null);
            RemedyIncidentTool.ToolContext ctx = contextHolder.get();
            if (ctx == null) {
                throw new IllegalStateException("No context set for tool execution");
            }
            return ctx;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to get tool context", e);
        }
    }

    /**
     * Search for similar existing work orders.
     *
     * @param query The search query describing the work
     * @param maxResults Maximum number of results to return
     * @return Formatted list of similar work orders
     */
    @Tool("Search for similar existing work orders in BMC Remedy. Use this before creating a new work order to check for duplicates.")
    public String searchSimilarWorkOrders(
            @P("Description of the work to search for") String query,
            @P("Maximum number of results (default 5)") Integer maxResults) {

        log.info("Searching for similar work orders: {}", query);

        int limit = maxResults != null ? Math.min(maxResults, 10) : 5;

        try {
            var results = vectorStoreService.searchByType(query, "WorkOrder", limit, 0.3);

            if (results.isEmpty()) {
                return "No similar work orders found in the knowledge base.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Found ").append(results.size()).append(" similar work order(s):\n\n");

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
                sb.append(" highly similar work order(s) that may be duplicates.\n");
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("Error searching work orders: {}", e.getMessage(), e);
            return "Error searching for similar work orders: " + e.getMessage();
        }
    }

    /**
     * Stage a work order creation for user confirmation.
     *
     * @param summary Brief summary of the work order (required, max 255 chars)
     * @param description Detailed description (required)
     * @param workOrderType Type: 0=General, 1=Project Work, 2=Break/Fix, 3=Move/Add/Change, 4=Release Activity
     * @param priority Priority: 0=Critical, 1=High, 2=Medium, 3=Low
     * @return Confirmation prompt for the user
     */
    @Tool("Stage a new work order for creation. The user must confirm before it's created. Returns a confirmation prompt.")
    public String stageWorkOrderCreation(
            @P("Brief summary of the work order (max 255 characters)") String summary,
            @P("Detailed description of the work") String description,
            @P("Type: 0=General, 1=Project Work, 2=Break/Fix, 3=Move/Add/Change, 4=Release Activity") Integer workOrderType,
            @P("Priority: 0=Critical, 1=High, 2=Medium, 3=Low") Integer priority) {

        var ctx = getContext();
        log.info("Staging work order creation for user {} in session {}", ctx.userId(), ctx.sessionId());

        // Check rate limit
        if (rateLimiter.isRateLimited(ctx.userId())) {
            var status = rateLimiter.getStatus(ctx.userId());
            return String.format(
                "⚠️ **Rate limit exceeded.** You've reached the maximum of %d work order creations per hour. " +
                "Please wait before creating more work orders.",
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

        if (!inputValidator.isValidWorkOrderType(workOrderType)) {
            return "❌ **Validation Error:** Work order type must be between 0 (General) and 4 (Release Activity).";
        }

        if (!inputValidator.isValidPriority(priority)) {
            return "❌ **Validation Error:** Priority must be between 0 (Critical) and 3 (Low).";
        }

        // Check for duplicates
        try {
            var duplicates = vectorStoreService.searchByType(summary + " " + description, "WorkOrder", 3, duplicateThreshold);
            if (!duplicates.isEmpty()) {
                StringBuilder warning = new StringBuilder();
                warning.append("⚠️ **Potential duplicates found:**\n\n");
                for (var dup : duplicates) {
                    warning.append("- **").append(dup.getSourceId()).append("**: ");
                    String title = dup.getMetadata() != null ? dup.getMetadata().get("title") : null;
                    warning.append(title != null ? title : "No title");
                    warning.append(" (").append(String.format("%.0f%%", dup.getScore() * 100)).append(" similar)\n");
                }
                warning.append("\nPlease review these before proceeding.\n");
                return warning.toString();
            }
        } catch (Exception e) {
            log.warn("Duplicate check failed: {}", e.getMessage());
        }

        // Build the request
        WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
            .summary(summaryValidation.sanitizedInput())
            .description(descValidation.sanitizedInput())
            .workOrderType(workOrderType)
            .priority(priority)
            .createdBy(ctx.userId())
            .sessionId(ctx.sessionId())
            .build();

        // Stage the action
        PendingAction action = confirmationService.stageWorkOrderCreation(
            ctx.sessionId(), ctx.userId(), request);

        return action.getConfirmationPrompt();
    }

    /**
     * Stage a work order creation with scheduling and additional details.
     */
    @Tool("Stage a new scheduled work order for creation with additional details. The user must confirm before it's created.")
    public String stageScheduledWorkOrder(
            @P("Brief summary of the work order (max 255 characters)") String summary,
            @P("Detailed description of the work") String description,
            @P("Type: 0=General, 1=Project Work, 2=Break/Fix, 3=Move/Add/Change, 4=Release Activity") Integer workOrderType,
            @P("Priority: 0=Critical, 1=High, 2=Medium, 3=Low") Integer priority,
            @P("Days from now to start (optional, default 1)") Integer startDaysFromNow,
            @P("Duration in days (optional, default 1)") Integer durationDays,
            @P("Category (optional)") String category,
            @P("Assigned group (optional)") String assignedGroup) {

        var ctx = getContext();
        log.info("Staging scheduled work order for user {} in session {}", ctx.userId(), ctx.sessionId());

        // Check rate limit
        if (rateLimiter.isRateLimited(ctx.userId())) {
            var status = rateLimiter.getStatus(ctx.userId());
            return String.format(
                "⚠️ **Rate limit exceeded.** You've reached the maximum of %d work order creations per hour.",
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

        if (!inputValidator.isValidWorkOrderType(workOrderType)) {
            return "❌ **Validation Error:** Work order type must be between 0 and 4.";
        }

        if (!inputValidator.isValidPriority(priority)) {
            return "❌ **Validation Error:** Priority must be between 0 and 3.";
        }

        // Build the request with optional fields
        WorkOrderCreationRequest.WorkOrderCreationRequestBuilder builder = WorkOrderCreationRequest.builder()
            .summary(summaryValidation.sanitizedInput())
            .description(descValidation.sanitizedInput())
            .workOrderType(workOrderType)
            .priority(priority)
            .createdBy(ctx.userId())
            .sessionId(ctx.sessionId());

        // Calculate scheduled dates
        int startDays = startDaysFromNow != null && startDaysFromNow >= 0 ? startDaysFromNow : 1;
        int duration = durationDays != null && durationDays > 0 ? durationDays : 1;

        Instant startDate = Instant.now().plus(startDays, ChronoUnit.DAYS);
        Instant endDate = startDate.plus(duration, ChronoUnit.DAYS);

        builder.scheduledStartDate(startDate);
        builder.scheduledEndDate(endDate);

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

        WorkOrderCreationRequest request = builder.build();

        // Stage the action
        PendingAction action = confirmationService.stageWorkOrderCreation(
            ctx.sessionId(), ctx.userId(), request);

        return action.getConfirmationPrompt();
    }

    /**
     * Get pending work order creations for the current session.
     */
    @Tool("List pending work order creations awaiting confirmation in the current session")
    public String listPendingWorkOrders() {
        var ctx = getContext();

        List<PendingAction> pending = confirmationService.getPendingActionsForSession(ctx.sessionId())
            .stream()
            .filter(a -> a.getActionType() == PendingAction.ActionType.WORK_ORDER_CREATE)
            .toList();

        if (pending.isEmpty()) {
            return "No pending work order creations in this session.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("**Pending Work Order Creations:**\n\n");
        for (PendingAction action : pending) {
            sb.append("- **").append(action.getActionId()).append("**: ");
            sb.append(action.getPreview().split("\n")[2].replace("**Summary:** ", ""));
            sb.append("\n  _Expires: ").append(action.getExpiresAt()).append("_\n\n");
        }
        sb.append("\nTo confirm: `confirm <action_id>`\n");
        sb.append("To cancel: `cancel <action_id>`\n");

        return sb.toString();
    }
}
