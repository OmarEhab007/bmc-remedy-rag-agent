package com.bmc.rag.api.controller;

import com.bmc.rag.agent.retrieval.SecureContentRetriever.UserContext;
import com.bmc.rag.agent.service.RagAssistantService;
import com.bmc.rag.api.dto.ChatQueryMessage;
import com.bmc.rag.api.dto.ChatResponseChunk;
import com.bmc.rag.api.dto.ChatResponseChunk.ChunkType;
import com.bmc.rag.api.dto.ChatResponseChunk.Citation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import jakarta.annotation.PreDestroy;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket controller for real-time chat streaming.
 * Handles STOMP messages and streams responses token by token.
 */
@Slf4j
@Controller
public class WebSocketChatController {

    private static final int MAX_QUERY_LENGTH = 10000;

    private final RagAssistantService ragAssistantService;
    private final SimpMessagingTemplate messagingTemplate;

    // Dedicated executor for WebSocket processing to avoid ForkJoinPool exhaustion
    private final ExecutorService websocketExecutor = Executors.newFixedThreadPool(8, r -> {
        Thread t = new Thread(r, "ws-chat-executor");
        t.setDaemon(true);
        return t;
    });

    public WebSocketChatController(RagAssistantService ragAssistantService,
                                   SimpMessagingTemplate messagingTemplate) {
        this.ragAssistantService = ragAssistantService;
        this.messagingTemplate = messagingTemplate;
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down WebSocket executor...");
        websocketExecutor.shutdown();
        try {
            if (!websocketExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                websocketExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            websocketExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Handle incoming chat messages and stream responses.
     *
     * @param message The chat query message
     * @param headerAccessor Message headers for session info
     */
    @MessageMapping("/chat.query")
    public void handleChatQuery(
            @Payload ChatQueryMessage message,
            SimpMessageHeaderAccessor headerAccessor) {

        String wsSessionId = headerAccessor.getSessionId();
        log.info("WebSocket chat query from session {}: messageId={}",
            wsSessionId, message.getMessageId());

        // Generate session ID if not provided
        String sessionId = message.getSessionId() != null
            ? message.getSessionId()
            : UUID.randomUUID().toString();

        String destination = "/queue/response";

        // Validate input to prevent DoS attacks (same as REST endpoint)
        if (message.getText() == null || message.getText().isBlank()) {
            sendChunk(wsSessionId, destination, ChatResponseChunk.builder()
                .messageId(message.getMessageId())
                .sessionId(sessionId)
                .type(ChunkType.ERROR)
                .error("Query text cannot be empty")
                .isComplete(true)
                .build());
            return;
        }

        if (message.getText().length() > MAX_QUERY_LENGTH) {
            log.warn("WebSocket query exceeded max length: {} chars from session {}",
                message.getText().length(), wsSessionId);
            sendChunk(wsSessionId, destination, ChatResponseChunk.builder()
                .messageId(message.getMessageId())
                .sessionId(sessionId)
                .type(ChunkType.ERROR)
                .error("Query exceeds maximum length of " + MAX_QUERY_LENGTH + " characters")
                .isComplete(true)
                .build());
            return;
        }

        // Send "thinking" indicator
        sendChunk(wsSessionId, destination, ChatResponseChunk.builder()
            .messageId(message.getMessageId())
            .sessionId(sessionId)
            .type(ChunkType.THINKING)
            .build());

        // Process asynchronously using dedicated executor
        CompletableFuture.runAsync(() -> {
            try {
                processAndStreamResponse(wsSessionId, destination, message, sessionId);
            } catch (Exception e) {
                log.error("Error processing chat query: {}", e.getMessage(), e);
                sendChunk(wsSessionId, destination, ChatResponseChunk.builder()
                    .messageId(message.getMessageId())
                    .sessionId(sessionId)
                    .type(ChunkType.ERROR)
                    .error("An error occurred while processing your request: " + e.getMessage())
                    .isComplete(true)
                    .build());
            }
        }, websocketExecutor);
    }

    /**
     * Process the chat query and stream the response.
     */
    private void processAndStreamResponse(
            String wsSessionId,
            String destination,
            ChatQueryMessage message,
            String sessionId) {

        // Build user context for ReBAC
        UserContext userContext = new UserContext(
            message.getUserId(),
            message.getUserGroups() != null ? message.getUserGroups() : Collections.emptySet()
        );

        // Use streaming chat
        ragAssistantService.chatWithStreaming(
            sessionId,
            message.getText(),
            userContext,
            // Token consumer - called for each token
            token -> {
                sendChunk(wsSessionId, destination, ChatResponseChunk.builder()
                    .messageId(message.getMessageId())
                    .sessionId(sessionId)
                    .token(token)
                    .type(ChunkType.TOKEN)
                    .build());
            },
            // Completion handler
            new RagAssistantService.StreamingCompletionHandler() {
                @Override
                public void onComplete(List<String> sources, Double confidence) {
                    sendChunk(wsSessionId, destination, ChatResponseChunk.builder()
                        .messageId(message.getMessageId())
                        .sessionId(sessionId)
                        .type(ChunkType.COMPLETE)
                        .isComplete(true)
                        .citations(buildCitations(sources))
                        .confidenceScore(confidence)
                        .build());
                }

                @Override
                public void onError(Throwable error) {
                    log.error("Streaming error for message {}: {}", message.getMessageId(), error.getMessage());
                    sendChunk(wsSessionId, destination, ChatResponseChunk.builder()
                        .messageId(message.getMessageId())
                        .sessionId(sessionId)
                        .type(ChunkType.ERROR)
                        .error("An error occurred: " + error.getMessage())
                        .isComplete(true)
                        .build());
                }
            }
        );
    }

    /**
     * Send a response chunk to the user's WebSocket session.
     */
    private void sendChunk(String wsSessionId, String destination, ChatResponseChunk chunk) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(wsSessionId);
        headerAccessor.setLeaveMutable(true);

        messagingTemplate.convertAndSendToUser(
            wsSessionId,
            destination,
            chunk,
            headerAccessor.getMessageHeaders()
        );
    }

    /**
     * Build citations from source references.
     */
    private List<Citation> buildCitationsFromSources(List<String> sources) {
        if (sources == null || sources.isEmpty()) {
            return Collections.emptyList();
        }

        return sources.stream()
            .map(source -> {
                // Parse source reference (format: "TYPE: ID - Title")
                String[] parts = source.split(":", 2);
                String type = parts.length > 0 ? parts[0].trim() : "UNKNOWN";
                String idAndTitle = parts.length > 1 ? parts[1].trim() : source;

                String[] idParts = idAndTitle.split("-", 2);
                String id = idParts.length > 0 ? idParts[0].trim() : "";
                String title = idParts.length > 1 ? idParts[1].trim() : "";

                return Citation.builder()
                    .sourceType(type)
                    .sourceId(id)
                    .title(title)
                    .build();
            })
            .toList();
    }

    /**
     * Build citations from retrieval results.
     */
    private List<Citation> buildCitations(List<String> sources) {
        return buildCitationsFromSources(sources);
    }
}
