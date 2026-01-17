package com.bmc.rag.agent.service;

import com.bmc.rag.agent.config.AgenticConfig;
import com.bmc.rag.agent.confirmation.ConfirmationService;
import com.bmc.rag.agent.confirmation.PendingAction;
import com.bmc.rag.agent.retrieval.SecureContentRetriever.UserContext;
import com.bmc.rag.agent.tools.RemedyIncidentTool;
import com.bmc.rag.agent.tools.RemedyWorkOrderTool;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agentic assistant service with tool-calling capabilities.
 * Extends the base RAG assistant with write operations (ticket creation).
 * Only enabled when agentic.enabled=true.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "agentic.enabled", havingValue = "true")
public class AgenticAssistantService {

    private final ChatLanguageModel chatModel;
    private final RemedyIncidentTool incidentTool;
    private final RemedyWorkOrderTool workOrderTool;
    private final ConfirmationService confirmationService;
    private final AgenticConfig agenticConfig;

    // Patterns for detecting confirmation/cancellation commands
    private static final Pattern CONFIRM_PATTERN = Pattern.compile("(?i)^\\s*confirm\\s+([a-zA-Z0-9]+)\\s*$");
    private static final Pattern CANCEL_PATTERN = Pattern.compile("(?i)^\\s*cancel\\s+([a-zA-Z0-9]+)\\s*$");

    // Patterns for detecting agentic intent
    private static final List<Pattern> CREATION_INTENT_PATTERNS = List.of(
        Pattern.compile("(?i)(create|open|submit|log|raise|file|new)\\s+(a\\s+)?(incident|ticket|issue|request)"),
        Pattern.compile("(?i)(create|open|submit|log|raise|file|new)\\s+(a\\s+)?(work\\s*order|wo|task)"),
        Pattern.compile("(?i)(i\\s+need|i\\s+want|can\\s+you|please)\\s+(a\\s+)?(new\\s+)?(incident|ticket|work\\s*order)"),
        Pattern.compile("(?i)^(incident|ticket|work\\s*order)\\s+for\\s+"),
        // Arabic patterns
        Pattern.compile("(?i)(أنشئ|افتح|سجل|ارفع)\\s+(بلاغ|تذكرة|حادثة|أمر\\s*عمل)")
    );

    // LangChain4j AI Service interface with tools
    private final AgenticAssistant assistant;

    public AgenticAssistantService(
            ChatLanguageModel chatModel,
            RemedyIncidentTool incidentTool,
            RemedyWorkOrderTool workOrderTool,
            ConfirmationService confirmationService,
            AgenticConfig agenticConfig) {

        this.chatModel = chatModel;
        this.incidentTool = incidentTool;
        this.workOrderTool = workOrderTool;
        this.confirmationService = confirmationService;
        this.agenticConfig = agenticConfig;

        // Build AI Service with tools registered
        this.assistant = AiServices.builder(AgenticAssistant.class)
            .chatLanguageModel(chatModel)
            .tools(incidentTool, workOrderTool)
            .build();

        log.info("AgenticAssistantService initialized with tools: RemedyIncidentTool, RemedyWorkOrderTool");
    }

    /**
     * Process a message that may have agentic intent.
     *
     * @param sessionId The session ID
     * @param userId The user ID
     * @param message The user's message
     * @param userContext User context for access control
     * @return The response
     */
    public AgenticResponse processMessage(
            String sessionId,
            String userId,
            String message,
            UserContext userContext) {

        log.info("Processing agentic message for session {}: '{}'",
            sessionId, truncateForLog(message));

        // Check for confirmation/cancellation commands first
        var confirmMatch = CONFIRM_PATTERN.matcher(message);
        if (confirmMatch.matches()) {
            String actionId = confirmMatch.group(1);
            return handleConfirmation(actionId, sessionId, userId);
        }

        var cancelMatch = CANCEL_PATTERN.matcher(message);
        if (cancelMatch.matches()) {
            String actionId = cancelMatch.group(1);
            return handleCancellation(actionId, sessionId, userId);
        }

        // Set tool context
        RemedyIncidentTool.setContext(sessionId, userId);

        try {
            // Use the AI service with tools
            String response = assistant.chat(buildAgenticPrompt(message, userContext));

            return AgenticResponse.builder()
                .sessionId(sessionId)
                .response(response)
                .agenticAction(detectAgenticAction(response))
                .build();

        } catch (Exception e) {
            log.error("Error in agentic processing: {}", e.getMessage(), e);
            return AgenticResponse.builder()
                .sessionId(sessionId)
                .response("I encountered an error while processing your request: " + e.getMessage())
                .error(true)
                .build();
        } finally {
            RemedyIncidentTool.clearContext();
        }
    }

    /**
     * Handle a confirmation command.
     */
    private AgenticResponse handleConfirmation(String actionId, String sessionId, String userId) {
        log.info("Handling confirmation for action {} in session {}", actionId, sessionId);

        var result = confirmationService.confirm(actionId, sessionId, userId);

        if (result.success()) {
            return AgenticResponse.builder()
                .sessionId(sessionId)
                .response("✅ " + result.message())
                .agenticAction(AgenticAction.EXECUTED)
                .createdRecordId(result.recordId())
                .build();
        } else {
            return AgenticResponse.builder()
                .sessionId(sessionId)
                .response("❌ " + result.message())
                .agenticAction(result.cancelled() ? AgenticAction.CANCELLED : AgenticAction.FAILED)
                .build();
        }
    }

    /**
     * Handle a cancellation command.
     */
    private AgenticResponse handleCancellation(String actionId, String sessionId, String userId) {
        log.info("Handling cancellation for action {} in session {}", actionId, sessionId);

        var result = confirmationService.cancel(actionId, sessionId, userId);

        if (result.cancelled()) {
            return AgenticResponse.builder()
                .sessionId(sessionId)
                .response("Action cancelled. " + result.message())
                .agenticAction(AgenticAction.CANCELLED)
                .build();
        } else {
            return AgenticResponse.builder()
                .sessionId(sessionId)
                .response("❌ " + result.message())
                .error(true)
                .build();
        }
    }

    /**
     * Check if a message appears to have agentic intent.
     */
    public boolean hasAgenticIntent(String message) {
        if (!agenticConfig.isEnabled()) {
            return false;
        }

        if (message == null || message.isBlank()) {
            return false;
        }

        // Check for confirmation/cancellation commands
        if (CONFIRM_PATTERN.matcher(message).matches() ||
            CANCEL_PATTERN.matcher(message).matches()) {
            return true;
        }

        // Check for creation intent patterns
        for (Pattern pattern : CREATION_INTENT_PATTERNS) {
            if (pattern.matcher(message).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get pending actions for a session.
     */
    public List<PendingAction> getPendingActions(String sessionId) {
        return confirmationService.getPendingActionsForSession(sessionId);
    }

    /**
     * Build the system prompt for agentic interactions.
     */
    private String buildAgenticPrompt(String userMessage, UserContext userContext) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an IT support assistant that can search for and create tickets in BMC Remedy.\n\n");

        prompt.append("When the user wants to create an incident or work order:\n");
        prompt.append("1. First search for similar existing tickets to avoid duplicates\n");
        prompt.append("2. If no duplicates found, stage the creation for user confirmation\n");
        prompt.append("3. Always require user confirmation before creating anything\n\n");

        prompt.append("Available tools:\n");
        prompt.append("- searchSimilarIncidents: Find similar incidents\n");
        prompt.append("- stageIncidentCreation: Stage a new incident (requires confirmation)\n");
        prompt.append("- searchSimilarWorkOrders: Find similar work orders\n");
        prompt.append("- stageWorkOrderCreation: Stage a new work order (requires confirmation)\n\n");

        if (userContext != null && userContext.userId() != null) {
            prompt.append("Current user: ").append(userContext.userId()).append("\n\n");
        }

        prompt.append("User request: ").append(userMessage);

        return prompt.toString();
    }

    /**
     * Detect what type of agentic action was taken.
     */
    private AgenticAction detectAgenticAction(String response) {
        if (response.contains("confirm ") && response.contains("To confirm")) {
            return AgenticAction.STAGED;
        }
        if (response.contains("Successfully created")) {
            return AgenticAction.EXECUTED;
        }
        if (response.contains("similar incident") || response.contains("similar work order")) {
            return AgenticAction.SEARCH;
        }
        return AgenticAction.NONE;
    }

    /**
     * Truncate text for logging.
     */
    private String truncateForLog(String text) {
        if (text == null) return "";
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }

    /**
     * Check if the service is enabled.
     */
    public boolean isEnabled() {
        return agenticConfig.isEnabled();
    }

    /**
     * AI Service interface for LangChain4j.
     */
    interface AgenticAssistant {
        String chat(String message);
    }

    /**
     * Types of agentic actions.
     */
    public enum AgenticAction {
        NONE,       // No agentic action taken
        SEARCH,     // Searched for existing tickets
        STAGED,     // Staged an action for confirmation
        EXECUTED,   // Executed a confirmed action
        CANCELLED,  // Action was cancelled
        FAILED      // Action failed
    }

    /**
     * Response from agentic processing.
     */
    @lombok.Data
    @lombok.Builder
    public static class AgenticResponse {
        private String sessionId;
        private String response;
        @lombok.Builder.Default
        private AgenticAction agenticAction = AgenticAction.NONE;
        private String createdRecordId;
        @lombok.Builder.Default
        private boolean error = false;
    }
}
