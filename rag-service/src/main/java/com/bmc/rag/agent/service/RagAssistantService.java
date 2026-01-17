package com.bmc.rag.agent.service;

import com.bmc.rag.agent.config.AgenticConfig;
import com.bmc.rag.agent.config.RagConfig;
import com.bmc.rag.agent.config.ZaiConfig;
import com.bmc.rag.agent.memory.PostgresChatMemoryStore;
import com.bmc.rag.agent.retrieval.SecureContentRetriever;
import com.bmc.rag.agent.retrieval.SecureContentRetriever.RetrievalResult;
import com.bmc.rag.agent.retrieval.SecureContentRetriever.UserContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Main RAG assistant service.
 * Orchestrates retrieval, memory, and LLM inference for conversational IT support.
 */
@Slf4j
@Service
public class RagAssistantService {

    private final ChatLanguageModel chatModel;
    private final StreamingChatLanguageModel streamingChatModel;
    private final SecureContentRetriever contentRetriever;
    private final PostgresChatMemoryStore chatMemoryStore;
    private final RagConfig ragConfig;
    private final ZaiConfig zaiConfig;
    private final AgenticConfig agenticConfig;

    // Optional agentic assistant (injected when agentic.enabled=true)
    private AgenticAssistantService agenticAssistantService;

    // Session-specific chat memories with eviction policy to prevent OOM
    private final Cache<String, ChatMemory> sessionMemories;

    // Streaming timeout in seconds
    private static final int STREAMING_TIMEOUT_SECONDS = 120;

    // Maximum response size to prevent OOM from unbounded streaming
    private static final int MAX_RESPONSE_SIZE = 50_000;

    // Thinking mode markers for GLM-4.7
    private static final String THINKING_START = "<thinking>";
    private static final String THINKING_END = "</thinking>";

    public RagAssistantService(
            ChatLanguageModel chatModel,
            StreamingChatLanguageModel streamingChatModel,
            SecureContentRetriever contentRetriever,
            PostgresChatMemoryStore chatMemoryStore,
            RagConfig ragConfig,
            ZaiConfig zaiConfig,
            AgenticConfig agenticConfig) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.contentRetriever = contentRetriever;
        this.chatMemoryStore = chatMemoryStore;
        this.ragConfig = ragConfig;
        this.zaiConfig = zaiConfig;
        this.agenticConfig = agenticConfig;

        // Initialize Caffeine cache with eviction policy
        // Session timeout is 30 minutes per security spec (P1.4)
        this.sessionMemories = Caffeine.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)  // Evict after 30 minutes of inactivity
            .maximumSize(10_000)                       // Maximum 10,000 sessions in memory
            .recordStats()                            // Enable statistics for monitoring
            .evictionListener((key, value, cause) ->
                log.debug("Session {} evicted from memory cache (cause: {})", key, cause))
            .build();
    }

    /**
     * Set the agentic assistant service (optional, injected when enabled).
     */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setAgenticAssistantService(AgenticAssistantService agenticAssistantService) {
        this.agenticAssistantService = agenticAssistantService;
        if (agenticAssistantService != null) {
            log.info("Agentic assistant service injected - agentic operations enabled");
        }
    }

    /**
     * Check if the request has agentic intent and should be delegated.
     */
    public boolean hasAgenticIntent(String question) {
        return agenticAssistantService != null &&
               agenticConfig.isEnabled() &&
               agenticAssistantService.hasAgenticIntent(question);
    }

    /**
     * Process a request with potential agentic intent.
     * Delegates to AgenticAssistantService if agentic intent is detected.
     *
     * @param sessionId The conversation session ID
     * @param question The user's question
     * @param userId The user ID
     * @param userContext User context for access control
     * @return The assistant's response
     */
    public ChatResponse chatWithAgenticSupport(String sessionId, String question, String userId, UserContext userContext) {
        // Check for agentic intent
        if (hasAgenticIntent(question)) {
            log.info("Agentic intent detected, delegating to AgenticAssistantService");
            var agenticResponse = agenticAssistantService.processMessage(sessionId, userId, question, userContext);

            return ChatResponse.builder()
                .sessionId(sessionId)
                .response(agenticResponse.getResponse())
                .sources(Collections.emptyList())
                .hasContext(false)
                .build();
        }

        // Fall back to regular chat
        return chat(sessionId, question, userContext);
    }

    /**
     * Process a user question and generate a response.
     *
     * @param sessionId The conversation session ID
     * @param question The user's question
     * @param userContext User context for access control
     * @return The assistant's response
     */
    public ChatResponse chat(String sessionId, String question, UserContext userContext) {
        log.info("Processing chat request for session {}: '{}'",
            sessionId, truncateForLog(question));

        // Get or create chat memory for this session
        ChatMemory memory = getOrCreateMemory(sessionId);

        // Retrieve relevant content
        RetrievalResult retrievalResult = contentRetriever.retrieve(question, userContext);

        // Build messages for LLM
        List<ChatMessage> messages = buildMessages(memory, question, retrievalResult);

        // Call the LLM
        String response;
        try {
            Response<AiMessage> llmResponse = chatModel.generate(messages);
            response = llmResponse.content().text();
            log.debug("LLM response generated: {} chars", response.length());
        } catch (Exception e) {
            log.error("LLM call failed: {}", e.getMessage(), e);
            // Provide bilingual error message based on query language
            response = isArabicQuery(question)
                ? "عذراً، حدث خطأ أثناء معالجة طلبك. يرجى المحاولة مرة أخرى أو إعادة صياغة سؤالك."
                : "I apologize, but I encountered an error while processing your request. Please try again or rephrase your question.";
        }

        // Update memory with the conversation
        memory.add(UserMessage.from(question));
        memory.add(AiMessage.from(response));

        // Build and return response
        return ChatResponse.builder()
            .sessionId(sessionId)
            .response(response)
            .sources(retrievalResult.getSourceReferences())
            .hasContext(!retrievalResult.isEmpty())
            .build();
    }

    /**
     * Chat without context (direct LLM query).
     */
    public ChatResponse chatWithoutContext(String sessionId, String question) {
        log.info("Processing direct chat for session {}", sessionId);

        ChatMemory memory = getOrCreateMemory(sessionId);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(ragConfig.getSystemPrompt()));
        messages.addAll(memory.messages());
        messages.add(UserMessage.from(question));

        String response;
        try {
            Response<AiMessage> llmResponse = chatModel.generate(messages);
            response = llmResponse.content().text();
        } catch (Exception e) {
            log.error("LLM call failed: {}", e.getMessage(), e);
            response = "I encountered an error processing your request.";
        }

        memory.add(UserMessage.from(question));
        memory.add(AiMessage.from(response));

        return ChatResponse.builder()
            .sessionId(sessionId)
            .response(response)
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();
    }

    /**
     * Search for relevant content without generating a response.
     */
    public RetrievalResult search(String query, UserContext userContext) {
        return contentRetriever.retrieve(query, userContext);
    }

    /**
     * Process a user question with streaming response.
     * Tokens are emitted via the tokenConsumer as they're generated.
     *
     * @param sessionId The conversation session ID
     * @param question The user's question
     * @param userContext User context for access control
     * @param tokenConsumer Callback for each generated token
     * @param completionConsumer Callback when generation completes
     */
    public void chatWithStreaming(
            String sessionId,
            String question,
            UserContext userContext,
            Consumer<String> tokenConsumer,
            StreamingCompletionHandler completionConsumer) {

        log.info("Processing streaming chat request for session {}: '{}'",
            sessionId, truncateForLog(question));

        // Get or create chat memory for this session
        ChatMemory memory = getOrCreateMemory(sessionId);

        // Retrieve relevant content
        RetrievalResult retrievalResult = contentRetriever.retrieve(question, userContext);

        // Build messages for LLM
        List<ChatMessage> messages = buildMessages(memory, question, retrievalResult);

        // Track the full response for memory
        StringBuilder fullResponse = new StringBuilder();

        // Use streaming model
        CountDownLatch latch = new CountDownLatch(1);
        List<SecureContentRetriever.RetrievedDocument> citationDocuments = retrievalResult.getDocumentsForCitations();

        streamingChatModel.generate(messages, new StreamingResponseHandler<AiMessage>() {
            private volatile boolean truncated = false;
            private volatile boolean inThinkingBlock = false;
            private final StringBuilder thinkingContent = new StringBuilder();
            private final Object tokenLock = new Object();  // Synchronization lock for token ordering

            @Override
            public void onNext(String token) {
                // Synchronize to ensure tokens are processed and sent in order
                // LangChain4j may invoke onNext() from multiple threads concurrently
                synchronized (tokenLock) {
                    // Check response size to prevent OOM from unbounded streaming
                    if (truncated) {
                        return;  // Stop processing tokens after truncation
                    }

                    // Handle thinking mode tokens (GLM-4.7 format)
                    if (zaiConfig.isThinkingEnabled()) {
                        // Check for thinking block markers
                        if (token.contains(THINKING_START)) {
                            inThinkingBlock = true;
                            thinkingContent.setLength(0);  // Reset thinking buffer
                            log.debug("Entering thinking block for session {}", sessionId);
                            // Extract any content after the start tag
                            int startIdx = token.indexOf(THINKING_START) + THINKING_START.length();
                            if (startIdx < token.length()) {
                                thinkingContent.append(token.substring(startIdx));
                            }
                            return;  // Don't send thinking start marker to client
                        }

                        if (inThinkingBlock) {
                            if (token.contains(THINKING_END)) {
                                // Extract content before the end tag
                                int endIdx = token.indexOf(THINKING_END);
                                if (endIdx > 0) {
                                    thinkingContent.append(token.substring(0, endIdx));
                                }
                                log.debug("Exiting thinking block for session {} ({} chars of reasoning)",
                                    sessionId, thinkingContent.length());
                                inThinkingBlock = false;
                                // Process any content after the end tag
                                int afterIdx = endIdx + THINKING_END.length();
                                if (afterIdx < token.length()) {
                                    String afterThinking = token.substring(afterIdx);
                                    if (!afterThinking.isEmpty()) {
                                        fullResponse.append(afterThinking);
                                        tokenConsumer.accept(afterThinking);
                                    }
                                }
                                return;
                            }
                            // Accumulate thinking content but don't send to client
                            thinkingContent.append(token);
                            return;
                        }
                    }

                    // Normal token processing
                    if (fullResponse.length() + token.length() > MAX_RESPONSE_SIZE) {
                        log.warn("Response exceeded max size ({} chars), truncating for session {}",
                            MAX_RESPONSE_SIZE, sessionId);
                        truncated = true;
                        tokenConsumer.accept("\n\n[Response truncated due to length]");
                        return;
                    }
                    fullResponse.append(token);
                    tokenConsumer.accept(token);
                }
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                try {
                    // Update memory with the conversation
                    String responseText = fullResponse.toString();
                    if (responseText.isEmpty()) {
                        // Handle empty response case with bilingual message
                        responseText = getBilingualErrorMessage(question, !retrievalResult.isEmpty());
                        tokenConsumer.accept(responseText);
                    }

                    memory.add(UserMessage.from(question));
                    memory.add(AiMessage.from(responseText));

                    // Calculate confidence as average of top retrieval scores
                    double confidence = retrievalResult.isEmpty()
                        ? 0.5
                        : citationDocuments.stream()
                            .mapToDouble(doc -> doc.score())
                            .average()
                            .orElse(0.5);

                    // Call completion handler with full document details
                    completionConsumer.onComplete(citationDocuments, confidence);
                } catch (Exception e) {
                    log.error("Error in onComplete handler: {}", e.getMessage(), e);
                    completionConsumer.onError(e);
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public void onError(Throwable error) {
                log.error("Streaming error: {}", error.getMessage(), error);

                // Provide a fallback response if we have partial content
                String responseText = fullResponse.toString();
                if (responseText.isEmpty()) {
                    // Send a bilingual user-friendly error message
                    String errorMessage = isArabicQuery(question)
                        ? "عذراً، حدث خطأ أثناء معالجة طلبك. يرجى المحاولة مرة أخرى."
                        : "I apologize, but I encountered an error while processing your request. Please try again.";
                    tokenConsumer.accept(errorMessage);
                    responseText = errorMessage;
                }

                // Still save to memory for context continuity
                try {
                    memory.add(UserMessage.from(question));
                    memory.add(AiMessage.from(responseText));
                } catch (Exception e) {
                    log.warn("Failed to save conversation to memory: {}", e.getMessage());
                }

                completionConsumer.onError(error);
                latch.countDown();
            }
        });

        // Wait for streaming to complete with timeout to prevent thread pool exhaustion
        try {
            if (!latch.await(STREAMING_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                log.error("Streaming timed out after {} seconds for session {}", STREAMING_TIMEOUT_SECONDS, sessionId);
                completionConsumer.onError(new TimeoutException("LLM streaming timed out after " + STREAMING_TIMEOUT_SECONDS + " seconds"));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Streaming interrupted for session {}", sessionId);
            completionConsumer.onError(e);
        }
    }

    /**
     * Functional interface for streaming completion handling.
     */
    @FunctionalInterface
    public interface StreamingCompletionHandler {
        void onComplete(List<SecureContentRetriever.RetrievedDocument> documents, Double confidence);

        default void onError(Throwable error) {
            // Default error handling - can be overridden
        }
    }

    /**
     * Clear conversation history for a session.
     */
    public void clearSession(String sessionId) {
        sessionMemories.invalidate(sessionId);
        chatMemoryStore.deleteMessages(sessionId);
        log.info("Cleared session: {}", sessionId);
    }

    /**
     * Get conversation history for a session.
     */
    public List<ChatMessage> getConversationHistory(String sessionId) {
        return chatMemoryStore.getMessages(sessionId);
    }

    /**
     * Get summaries of all chat sessions.
     */
    public List<PostgresChatMemoryStore.SessionInfo> getSessionSummaries() {
        return chatMemoryStore.getSessionSummaries();
    }

    /**
     * Build the message list for LLM.
     */
    private List<ChatMessage> buildMessages(
            ChatMemory memory,
            String question,
            RetrievalResult retrievalResult) {

        List<ChatMessage> messages = new ArrayList<>();

        // System message with instructions
        messages.add(SystemMessage.from(ragConfig.getSystemPrompt()));

        // Add conversation history
        messages.addAll(memory.messages());

        // Build user message with context
        StringBuilder userPrompt = new StringBuilder();

        if (!retrievalResult.isEmpty()) {
            userPrompt.append("Based on the following context from the ITSM knowledge base:\n\n");
            userPrompt.append(retrievalResult.formattedContext());
            userPrompt.append("\nPlease answer the following question:\n\n");
        }

        userPrompt.append(question);

        if (!retrievalResult.isEmpty() && ragConfig.isIncludeCitations()) {
            userPrompt.append("\n\nRemember to cite your sources using [SOURCE: record_type record_id] format.");
        }

        messages.add(UserMessage.from(userPrompt.toString()));

        return messages;
    }

    /**
     * Get or create chat memory for a session.
     */
    private ChatMemory getOrCreateMemory(String sessionId) {
        return sessionMemories.get(sessionId, id -> {
            ChatMemory memory = MessageWindowChatMemory.builder()
                .id(id)
                .maxMessages(ragConfig.getMaxMemoryMessages())
                .chatMemoryStore(chatMemoryStore)
                .build();
            log.debug("Created new chat memory for session: {}", id);
            return memory;
        });
    }

    /**
     * Get cache statistics for monitoring.
     */
    public Map<String, Long> getCacheStats() {
        var stats = sessionMemories.stats();
        Map<String, Long> result = new HashMap<>();
        result.put("hitCount", stats.hitCount());
        result.put("missCount", stats.missCount());
        result.put("evictionCount", stats.evictionCount());
        result.put("estimatedSize", sessionMemories.estimatedSize());
        return result;
    }

    /**
     * Check if the query contains Arabic characters.
     */
    private boolean isArabicQuery(String text) {
        if (text == null || text.isEmpty()) return false;
        // Check for Arabic Unicode range (0x0600-0x06FF)
        for (char c : text.toCharArray()) {
            if (c >= 0x0600 && c <= 0x06FF) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get bilingual error message based on query language.
     */
    private String getBilingualErrorMessage(String question, boolean hasContext) {
        if (isArabicQuery(question)) {
            return hasContext
                ? "عذراً، لم أتمكن من إنشاء رد. يرجى المحاولة مرة أخرى."
                : "لم أجد معلومات ذات صلة في قاعدة المعرفة.";
        } else {
            return hasContext
                ? "I apologize, but I couldn't generate a response. Please try again."
                : "No relevant information found in the knowledge base.";
        }
    }

    /**
     * Truncate text for logging.
     */
    private String truncateForLog(String text) {
        if (text == null) return "";
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }

    /**
     * Chat response DTO.
     */
    @lombok.Data
    @lombok.Builder
    public static class ChatResponse {
        private String sessionId;
        private String response;
        private List<String> sources;
        private boolean hasContext;
    }
}
