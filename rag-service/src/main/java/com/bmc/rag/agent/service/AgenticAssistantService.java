package com.bmc.rag.agent.service;

import com.bmc.rag.agent.config.AgenticConfig;
import com.bmc.rag.agent.confirmation.ConfirmationService;
import com.bmc.rag.agent.confirmation.PendingAction;
import com.bmc.rag.agent.prompt.AgenticSystemPrompt;
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

    // Patterns for detecting EXPLICIT agentic intent (high confidence)
    private static final List<Pattern> EXPLICIT_CREATION_PATTERNS = List.of(
        // Direct creation verbs: "create an incident", "open a ticket", "raise an issue"
        Pattern.compile("(?i)(create|open|submit|log|raise|file|new)\\s+(an?\\s+)?(incident|ticket|issue|request)"),
        Pattern.compile("(?i)(create|open|submit|log|raise|file|new)\\s+(an?\\s+)?(work\\s*order|wo|task)"),
        // Polite requests: "can you create an incident", "please open a ticket"
        Pattern.compile("(?i)(i\\s+need|i\\s+want|can\\s+you|please)\\s+(create|open|file)?\\s*(an?\\s+)?(new\\s+)?(incident|ticket|issue|work\\s*order)"),
        // Starting with ticket type: "incident for my VPN"
        Pattern.compile("(?i)^(incident|ticket|work\\s*order)\\s+for\\s+"),
        // Arabic patterns
        Pattern.compile("(?i)(أنشئ|افتح|سجل|ارفع)\\s+(بلاغ|تذكرة|حادثة|أمر\\s*عمل)")
    );

    // Patterns for detecting IMPLICIT action intent (requires context + conversation)
    private static final List<Pattern> IMPLICIT_ACTION_PATTERNS = List.of(
        // Frustration + duration signals
        Pattern.compile("(?i)(for|since|been)\\s+(\\d+|several|a few|many)\\s*(days?|hours?|weeks?)"),
        Pattern.compile("(?i)(still|keeps?|constantly|repeatedly)\\s+(happening|failing|crashing|not working)"),
        // Business impact signals
        Pattern.compile("(?i)(blocking|blocks?)\\s+(my|our|the)\\s+(work|job|project|team)"),
        Pattern.compile("(?i)can'?t\\s+(do|complete|finish|access)\\s+(my|the)"),
        // Failed self-service signals
        Pattern.compile("(?i)(already|have)\\s+(tried|attempted|restarted|rebooted)"),
        Pattern.compile("(?i)nothing\\s+(works?|is working|helped)"),
        // Escalation/helplessness signals
        Pattern.compile("(?i)(need(s?)|requires?)\\s+(to be)?\\s*(escalat|fix|resolv)"),
        Pattern.compile("(?i)don'?t\\s+know\\s+what\\s+(else|to do)"),
        Pattern.compile("(?i)please\\s+(help|fix|resolve)\\s+(this|it|the)")
    );

    // Patterns for detecting QUESTION intent (likely NOT an action request)
    private static final List<Pattern> QUESTION_PATTERNS = List.of(
        Pattern.compile("(?i)^(how|what|why|where|when|can i|is it possible)\\s+"),
        Pattern.compile("(?i)\\?\\s*$"),
        Pattern.compile("(?i)^(do|does|did|is|are|was|were|will|would|could|should)\\s+")
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
        return processMessage(sessionId, userId, message, userContext, null);
    }

    /**
     * Process a message that may have agentic intent with conversation history.
     *
     * @param sessionId The session ID
     * @param userId The user ID
     * @param message The user's message
     * @param userContext User context for access control
     * @param conversationHistory Previous messages for context extraction
     * @return The response
     */
    public AgenticResponse processMessage(
            String sessionId,
            String userId,
            String message,
            UserContext userContext,
            List<ChatMessage> conversationHistory) {

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

        // Set tool context with conversation history for context extraction
        RemedyIncidentTool.setContext(sessionId, userId, conversationHistory);

        try {
            // Use the AI service with tools
            String response = assistant.chat(buildAgenticPrompt(message, userContext, conversationHistory));

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
     * Uses a multi-stage detection strategy:
     * 1. Confirmation/cancellation commands (always agentic)
     * 2. Explicit creation patterns (high confidence)
     * 3. Implicit action signals (context-dependent)
     */
    public boolean hasAgenticIntent(String message) {
        if (!agenticConfig.isEnabled()) {
            return false;
        }

        if (message == null || message.isBlank()) {
            return false;
        }

        // Stage 1: Check for confirmation/cancellation commands
        if (CONFIRM_PATTERN.matcher(message).matches() ||
            CANCEL_PATTERN.matcher(message).matches()) {
            return true;
        }

        // Stage 2: Check for explicit creation intent patterns
        for (Pattern pattern : EXPLICIT_CREATION_PATTERNS) {
            if (pattern.matcher(message).find()) {
                log.debug("Explicit agentic intent detected: {}", pattern.pattern());
                return true;
            }
        }

        // Stage 3: Check for implicit action signals
        // These require more context but indicate user needs action
        int implicitSignalCount = 0;
        for (Pattern pattern : IMPLICIT_ACTION_PATTERNS) {
            if (pattern.matcher(message).find()) {
                implicitSignalCount++;
            }
        }

        // If multiple implicit signals present, likely needs action
        if (implicitSignalCount >= 2) {
            log.debug("Multiple implicit action signals detected: {}", implicitSignalCount);
            return true;
        }

        return false;
    }

    /**
     * Classify the user's intent using pattern-based analysis.
     * Uses regex patterns to detect question signals, explicit action requests,
     * and implicit action signals for intent classification.
     *
     * @param message The user's message
     * @param conversationHistory Previous messages for context
     * @return The classified intent
     */
    public IntentClassification classifyIntent(String message, List<ChatMessage> conversationHistory) {
        if (message == null || message.isBlank()) {
            return IntentClassification.QUESTION;
        }
        // First, check if it's clearly a question
        for (Pattern pattern : QUESTION_PATTERNS) {
            if (pattern.matcher(message).find()) {
                // Check if it also has action signals (mixed intent)
                boolean hasActionSignals = IMPLICIT_ACTION_PATTERNS.stream()
                    .anyMatch(p -> p.matcher(message).find());

                if (!hasActionSignals) {
                    return IntentClassification.QUESTION;
                }
                // If both question and action signals, it's ambiguous
                return IntentClassification.AMBIGUOUS_INTENT;
            }
        }

        // Check for explicit action patterns
        for (Pattern pattern : EXPLICIT_CREATION_PATTERNS) {
            if (pattern.matcher(message).find()) {
                // Determine if it's incident or work order
                if (message.toLowerCase().contains("work order") ||
                    message.toLowerCase().contains("work_order") ||
                    message.toLowerCase().contains(" wo ") ||
                    message.contains("أمر عمل")) {
                    return IntentClassification.ACTION_WORKORDER;
                }
                return IntentClassification.ACTION_INCIDENT;
            }
        }

        // Check implicit signals strength
        int implicitCount = 0;
        for (Pattern pattern : IMPLICIT_ACTION_PATTERNS) {
            if (pattern.matcher(message).find()) {
                implicitCount++;
            }
        }

        if (implicitCount >= 2) {
            // Strong implicit signals suggest action needed
            return IntentClassification.ACTION_INCIDENT;
        } else if (implicitCount == 1) {
            // Single implicit signal - ambiguous
            return IntentClassification.AMBIGUOUS_INTENT;
        }

        // No clear signals - treat as question/informational
        return IntentClassification.QUESTION;
    }

    /**
     * Intent classification categories.
     */
    public enum IntentClassification {
        /** User wants information or guidance */
        QUESTION,
        /** User wants to create an incident (something is broken) */
        ACTION_INCIDENT,
        /** User wants a work order (new service or change) */
        ACTION_WORKORDER,
        /** Unclear if user wants information or action */
        AMBIGUOUS_INTENT,
        /** User wants action but unclear if incident or work order */
        AMBIGUOUS_TYPE
    }

    /**
     * Get pending actions for a session.
     */
    public List<PendingAction> getPendingActions(String sessionId) {
        return confirmationService.getPendingActionsForSession(sessionId);
    }

    /**
     * Build the system prompt for agentic interactions.
     * Uses the enhanced AgenticSystemPrompt with comprehensive reasoning capabilities.
     *
     * @see AgenticSystemPrompt for full prompt structure including:
     *      - Intent classification rules
     *      - Ticket type classification (Incident vs Work Order)
     *      - Slot filling with inference rules
     *      - Context extraction from conversation
     */
    private String buildAgenticPrompt(String userMessage, UserContext userContext, List<ChatMessage> conversationHistory) {
        return AgenticSystemPrompt.build(userMessage, userContext, conversationHistory);
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
