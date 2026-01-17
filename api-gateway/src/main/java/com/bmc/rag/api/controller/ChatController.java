package com.bmc.rag.api.controller;

import com.bmc.rag.agent.retrieval.SecureContentRetriever;
import com.bmc.rag.agent.retrieval.SecureContentRetriever.RetrievalResult;
import com.bmc.rag.agent.retrieval.SecureContentRetriever.UserContext;
import com.bmc.rag.agent.service.RagAssistantService;
import com.bmc.rag.api.dto.*;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API controller for chat interactions.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private static final int MAX_QUERY_LENGTH = 10000;

    private final RagAssistantService ragAssistantService;
    private final SecureContentRetriever contentRetriever;

    /**
     * Chat endpoint - sends a message and receives a response.
     *
     * @param request Chat request containing the question
     * @return Chat response with the assistant's answer
     */
    @PostMapping
    @RateLimiter(name = "chat", fallbackMethod = "chatRateLimitFallback")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        // Validate query length to prevent DoS
        if (request.getQuestion() != null && request.getQuestion().length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("Question exceeds maximum length of " + MAX_QUERY_LENGTH + " characters");
        }
        log.info("Chat request received for session: {}",
            request.getSessionId() != null ? request.getSessionId() : "new");

        // Generate session ID if not provided
        String sessionId = request.getSessionId() != null
            ? request.getSessionId()
            : UUID.randomUUID().toString();

        // Build user context for ReBAC
        UserContext userContext = buildUserContext(request);

        // Process the chat request
        RagAssistantService.ChatResponse serviceResponse;
        if (request.isSkipContext()) {
            serviceResponse = ragAssistantService.chatWithoutContext(sessionId, request.getQuestion());
        } else {
            serviceResponse = ragAssistantService.chat(sessionId, request.getQuestion(), userContext);
        }

        // Build and return response
        ChatResponse response = ChatResponse.builder()
            .sessionId(sessionId)
            .response(serviceResponse.getResponse())
            .sources(serviceResponse.getSources())
            .hasContext(serviceResponse.isHasContext())
            .timestamp(System.currentTimeMillis())
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Search endpoint - performs semantic search without LLM response.
     *
     * @param request Search request
     * @return Search results
     */
    @PostMapping("/search")
    @RateLimiter(name = "search", fallbackMethod = "searchRateLimitFallback")
    public ResponseEntity<SearchResponse> search(@Valid @RequestBody SearchRequest request) {
        // Validate query length to prevent DoS
        if (request.getQuery() != null && request.getQuery().length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("Query exceeds maximum length of " + MAX_QUERY_LENGTH + " characters");
        }
        log.info("Search request received: '{}'",
            request.getQuery().length() > 50
                ? request.getQuery().substring(0, 50) + "..."
                : request.getQuery());

        UserContext userContext = new UserContext(
            request.getUserId(),
            request.getUserGroups() != null ? request.getUserGroups() : Collections.emptySet()
        );

        RetrievalResult result = contentRetriever.retrieve(request.getQuery(), userContext);

        List<SearchResponse.SearchResultItem> items = result.documents().stream()
            .map(doc -> SearchResponse.SearchResultItem.builder()
                .sourceType(doc.sourceType())
                .sourceId(doc.sourceId())
                .chunkType(doc.chunkType())
                .content(doc.content())
                .title(doc.title())
                .category(doc.category())
                .score(doc.score())
                .build())
            .collect(Collectors.toList());

        SearchResponse response = SearchResponse.builder()
            .resultCount(items.size())
            .results(items)
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Clear session endpoint - removes conversation history.
     *
     * @param sessionId Session ID to clear
     * @return Success response
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, String>> clearSession(@PathVariable String sessionId) {
        log.info("Clearing session: {}", sessionId);

        ragAssistantService.clearSession(sessionId);

        return ResponseEntity.ok(Map.of(
            "status", "cleared",
            "sessionId", sessionId
        ));
    }

    /**
     * Get conversation history for a session.
     *
     * @param sessionId Session ID
     * @return List of messages
     */
    @GetMapping("/sessions/{sessionId}/history")
    public ResponseEntity<Map<String, Object>> getHistory(@PathVariable String sessionId) {
        log.info("Getting history for session: {}", sessionId);

        var messages = ragAssistantService.getConversationHistory(sessionId);

        List<Map<String, String>> messageList = messages.stream()
            .map(msg -> {
                Map<String, String> map = new HashMap<>();
                map.put("type", msg.type().name());
                // Get text content safely
                if (msg instanceof dev.langchain4j.data.message.UserMessage um) {
                    map.put("content", um.singleText());
                } else if (msg instanceof dev.langchain4j.data.message.AiMessage am) {
                    map.put("content", am.text());
                } else if (msg instanceof dev.langchain4j.data.message.SystemMessage sm) {
                    map.put("content", sm.text());
                }
                return map;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "sessionId", sessionId,
            "messageCount", messages.size(),
            "messages", messageList
        ));
    }

    /**
     * Build UserContext from request.
     */
    private UserContext buildUserContext(ChatRequest request) {
        Set<String> groups = request.getUserGroups() != null
            ? request.getUserGroups()
            : Collections.emptySet();

        return new UserContext(request.getUserId(), groups);
    }

    /**
     * Fallback method when chat rate limit is exceeded.
     */
    @SuppressWarnings("unused")
    private ResponseEntity<ChatResponse> chatRateLimitFallback(ChatRequest request, RequestNotPermitted ex) {
        log.warn("Rate limit exceeded for chat endpoint");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(ChatResponse.builder()
                .sessionId(request.getSessionId())
                .response("Too many requests. Please wait and try again.")
                .sources(Collections.emptyList())
                .hasContext(false)
                .timestamp(System.currentTimeMillis())
                .build());
    }

    /**
     * Fallback method when search rate limit is exceeded.
     */
    @SuppressWarnings("unused")
    private ResponseEntity<SearchResponse> searchRateLimitFallback(SearchRequest request, RequestNotPermitted ex) {
        log.warn("Rate limit exceeded for search endpoint");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(SearchResponse.builder()
                .resultCount(0)
                .results(Collections.emptyList())
                .build());
    }
}
