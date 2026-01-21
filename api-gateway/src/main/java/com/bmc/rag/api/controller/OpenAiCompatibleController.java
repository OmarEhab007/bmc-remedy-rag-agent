package com.bmc.rag.api.controller;

import com.bmc.rag.agent.damee.GuidedServiceCreator;
import com.bmc.rag.agent.damee.GuidedServiceCreator.GuidedResponse;
import com.bmc.rag.agent.retrieval.SecureContentRetriever.UserContext;
import com.bmc.rag.agent.service.RagAssistantService;
import com.bmc.rag.api.dto.openai.*;
import com.bmc.rag.api.service.ToolIntentDetector;
import com.bmc.rag.api.service.ToolIntentDetector.Intent;
import com.bmc.rag.api.service.ToolIntentDetector.IntentResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.bmc.rag.agent.retrieval.SecureContentRetriever.RetrievedDocument;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * OpenAI-compatible API controller for Open WebUI integration.
 *
 * This controller implements the OpenAI API specification to allow
 * Open WebUI (and other OpenAI-compatible clients) to connect to
 * the BMC Remedy RAG backend.
 *
 * Endpoints:
 * - POST /v1/chat/completions - Chat completions (streaming & non-streaming)
 * - GET /v1/models - List available models
 * - GET /v1/models/{model} - Get model details
 *
 * Note: This is ADDITIVE to the existing /api/v1/chat endpoints.
 * The web-chat frontend continues to use the original API.
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class OpenAiCompatibleController {

    private static final String MODEL_ID = "bmc-remedy-rag";
    private static final int MAX_QUERY_LENGTH = 10000;
    private static final long SSE_TIMEOUT = 120000L; // 2 minutes

    private final RagAssistantService ragAssistantService;
    private final ObjectMapper objectMapper;
    private final ToolIntentDetector toolIntentDetector;
    private final GuidedServiceCreator guidedServiceCreator;

    // Thread pool for async SSE streaming
    private final ExecutorService streamingExecutor = Executors.newCachedThreadPool();

    /**
     * Chat completions endpoint - OpenAI compatible.
     * Handles both streaming (SSE) and non-streaming based on request.stream field.
     *
     * @param sessionId Optional session ID from header for conversation continuity
     * @param request OpenAI-compatible chat completion request
     * @return Chat completion response (JSON) or SSE stream
     */
    @PostMapping(value = "/chat/completions")
    @RateLimiter(name = "chat", fallbackMethod = "chatCompletionRateLimitFallback")
    public Object chatCompletions(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody ChatCompletionRequest request,
            jakarta.servlet.http.HttpServletResponse response) {

        // Extract user question from messages
        String question = request.getLastUserMessage();

        // Validate query length
        if (question != null && question.length() > MAX_QUERY_LENGTH) {
            if (request.isStream()) {
                response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
                return createErrorSseEmitter("Question exceeds maximum length of " + MAX_QUERY_LENGTH + " characters");
            }
            return ResponseEntity.badRequest().body(Map.of(
                "error", Map.of(
                    "message", "Question exceeds maximum length of " + MAX_QUERY_LENGTH + " characters",
                    "type", "invalid_request_error",
                    "code", "context_length_exceeded"
                )
            ));
        }

        // Generate stable session ID - prefer user field, then header, then generate
        // This ensures the same user gets the same session across requests
        String effectiveSessionId;
        if (request.getUser() != null && !request.getUser().isBlank()) {
            // Use user ID from request (Open WebUI may send this)
            effectiveSessionId = "user-" + request.getUser();
        } else if (sessionId != null && !sessionId.isBlank()) {
            effectiveSessionId = sessionId;
        } else {
            // Generate based on IP or use a default for anonymous users
            effectiveSessionId = "anonymous-" + UUID.randomUUID().toString().substring(0, 8);
        }

        // Also try to extract action ID from conversation history for confirm/cancel
        String actionIdFromHistory = extractActionIdFromHistory(request.getMessages());

        log.info("OpenAI-compatible chat request: session={}, stream={}, messageCount={}, hasTools={}",
            effectiveSessionId, request.isStream(),
            request.getMessages() != null ? request.getMessages().size() : 0,
            request.hasTools());

        // FIRST: Check for active guided service flow
        // This ensures continuity when user responds with "1", "yes", "confirm" etc.
        if (guidedServiceCreator.hasActiveFlow(effectiveSessionId)) {
            log.info("Active guided flow found for session {}, processing response", effectiveSessionId);
            try {
                GuidedResponse guidedResponse = guidedServiceCreator.processMessage(
                    effectiveSessionId,
                    "user-" + effectiveSessionId,
                    question
                );
                if (guidedResponse != null) {
                    String formattedResponse = formatGuidedResponse(guidedResponse);
                    if (request.isStream()) {
                        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
                        response.setHeader("Cache-Control", "no-cache");
                        response.setHeader("Connection", "keep-alive");
                        return createTextStreamingResponse(formattedResponse);
                    }
                    return ResponseEntity.ok(ChatCompletionResponse.of(formattedResponse, MODEL_ID, question));
                }
            } catch (Exception e) {
                log.error("Error processing guided flow: {}", e.getMessage(), e);
                // Fall through to normal processing
            }
        }

        // SECOND: Check for tool intent - always check, even if tools not explicitly passed
        // This enables agentic behavior where the backend decides when to use tools
        IntentResult intent = toolIntentDetector.detectIntent(question, true);

        if (intent.getIntent() != Intent.NONE) {
            log.info("Tool intent detected: {} with params: {}", intent.getIntent(), intent.getParameters());

            // Execute the tool directly and return the result as a chat response
            String toolResult = executeToolDirectly(intent, effectiveSessionId, actionIdFromHistory);
            if (toolResult != null) {
                return ResponseEntity.ok(ChatCompletionResponse.of(toolResult, MODEL_ID, question));
            }
        }

        // Build user context (empty for now - could extract from JWT later)
        UserContext userContext = new UserContext(null, Collections.emptySet());

        if (request.isStream()) {
            // Set content type for SSE
            response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");
            return createStreamingResponse(effectiveSessionId, question, userContext);
        } else {
            // Return non-streaming JSON response
            return ResponseEntity.ok(createNonStreamingResponse(effectiveSessionId, question, userContext));
        }
    }

    /**
     * Create an SSE emitter that sends an error message.
     */
    private SseEmitter createErrorSseEmitter(String errorMessage) {
        SseEmitter errorEmitter = new SseEmitter(SSE_TIMEOUT);
        streamingExecutor.submit(() -> {
            try {
                String errorJson = objectMapper.writeValueAsString(Map.of(
                    "error", Map.of("message", errorMessage, "type", "invalid_request_error")
                ));
                errorEmitter.send(SseEmitter.event().data(errorJson, MediaType.APPLICATION_JSON));
                errorEmitter.send(SseEmitter.event().data("[DONE]"));
                errorEmitter.complete();
            } catch (IOException e) {
                errorEmitter.completeWithError(e);
            }
        });
        return errorEmitter;
    }

    /**
     * Create non-streaming chat completion response.
     */
    private ChatCompletionResponse createNonStreamingResponse(
            String sessionId, String question, UserContext userContext) {

        try {
            // Use the existing RAG service
            RagAssistantService.ChatResponseDto serviceResponse =
                ragAssistantService.chat(sessionId, question, userContext);

            // Build OpenAI-compatible response
            return ChatCompletionResponse.of(
                serviceResponse.getResponse(),
                MODEL_ID,
                question
            );
        } catch (Exception e) {
            log.error("Error processing chat request: {}", e.getMessage(), e);
            // Return error in OpenAI format
            return ChatCompletionResponse.of(
                "I apologize, but I encountered an error processing your request. Please try again.",
                MODEL_ID,
                question
            );
        }
    }

    /**
     * Create SSE streaming response.
     */
    private SseEmitter createStreamingResponse(
            String sessionId, String question, UserContext userContext) {

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // Generate a unique ID for this streaming response
        String completionId = "chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

        streamingExecutor.submit(() -> {
            try {
                // Send initial chunk with role
                StreamingChatCompletionResponse firstChunk = StreamingChatCompletionResponse.builder()
                    .id(completionId)
                    .object("chat.completion.chunk")
                    .created(System.currentTimeMillis() / 1000)
                    .model(MODEL_ID)
                    .choices(Collections.singletonList(StreamChoice.role(0, "assistant")))
                    .build();

                sendSseEvent(emitter, firstChunk);

                // Use streaming chat from RAG service
                ragAssistantService.chatWithStreaming(
                    sessionId,
                    question,
                    userContext,
                    // Token consumer - send each token as SSE event
                    token -> {
                        try {
                            StreamingChatCompletionResponse chunk = StreamingChatCompletionResponse.builder()
                                .id(completionId)
                                .object("chat.completion.chunk")
                                .created(System.currentTimeMillis() / 1000)
                                .model(MODEL_ID)
                                .choices(Collections.singletonList(StreamChoice.content(0, token)))
                                .build();

                            sendSseEvent(emitter, chunk);
                        } catch (Exception e) {
                            log.warn("Error sending SSE token: {}", e.getMessage());
                        }
                    },
                    // Completion handler
                    (documents, confidence) -> {
                        try {
                            // Send formatted sources as additional content if we have documents
                            if (documents != null && !documents.isEmpty()) {
                                String sourcesMarkdown = formatSourcesCitations(documents);

                                StreamingChatCompletionResponse sourcesChunk = StreamingChatCompletionResponse.builder()
                                    .id(completionId)
                                    .object("chat.completion.chunk")
                                    .created(System.currentTimeMillis() / 1000)
                                    .model(MODEL_ID)
                                    .choices(Collections.singletonList(StreamChoice.content(0, sourcesMarkdown)))
                                    .build();

                                sendSseEvent(emitter, sourcesChunk);
                            }

                            // Send finish chunk
                            StreamingChatCompletionResponse finishChunk = StreamingChatCompletionResponse.builder()
                                .id(completionId)
                                .object("chat.completion.chunk")
                                .created(System.currentTimeMillis() / 1000)
                                .model(MODEL_ID)
                                .choices(Collections.singletonList(StreamChoice.finish(0, "stop")))
                                .build();

                            sendSseEvent(emitter, finishChunk);

                            // Send [DONE] marker
                            emitter.send(SseEmitter.event().data("[DONE]"));
                            emitter.complete();

                            log.debug("SSE stream completed for session {} with {} sources", sessionId,
                                documents != null ? documents.size() : 0);
                        } catch (Exception e) {
                            log.warn("Error completing SSE stream: {}", e.getMessage());
                            emitter.completeWithError(e);
                        }
                    }
                );
            } catch (Exception e) {
                log.error("Error in streaming chat: {}", e.getMessage(), e);
                try {
                    // Send error as SSE event
                    StreamingChatCompletionResponse errorChunk = StreamingChatCompletionResponse.builder()
                        .id(completionId)
                        .object("chat.completion.chunk")
                        .created(System.currentTimeMillis() / 1000)
                        .model(MODEL_ID)
                        .choices(Collections.singletonList(
                            StreamChoice.content(0, "Error: " + e.getMessage())
                        ))
                        .build();

                    sendSseEvent(emitter, errorChunk);
                    emitter.send(SseEmitter.event().data("[DONE]"));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        // Handle emitter lifecycle
        emitter.onCompletion(() -> log.debug("SSE emitter completed"));
        emitter.onTimeout(() -> {
            log.warn("SSE emitter timed out");
            emitter.complete();
        });
        emitter.onError(e -> log.error("SSE emitter error: {}", e.getMessage()));

        return emitter;
    }

    /**
     * Send an SSE event with JSON data.
     * OpenAI format: data: {json}\n\n
     */
    private void sendSseEvent(SseEmitter emitter, Object data) throws IOException {
        try {
            String json = objectMapper.writeValueAsString(data);
            // SseEmitter.event().data() adds "data: " prefix automatically
            emitter.send(SseEmitter.event().data(json, MediaType.APPLICATION_JSON));
        } catch (JsonProcessingException e) {
            log.error("Error serializing SSE data: {}", e.getMessage());
            throw new IOException("Failed to serialize SSE data", e);
        }
    }

    /**
     * Format a GuidedResponse into a user-friendly message.
     */
    private String formatGuidedResponse(GuidedResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append(response.getMessage());

        // Add options if available
        if (response.getOptions() != null && !response.getOptions().isEmpty()) {
            sb.append("\n\n");
            for (int i = 0; i < response.getOptions().size(); i++) {
                GuidedResponse.Option opt = response.getOptions().get(i);
                sb.append(String.format("**%d.** %s\n", i + 1, opt.getLabel()));
            }
        }

        return sb.toString();
    }

    /**
     * Create a simple text streaming response for guided flow responses.
     * Sends the entire text as a single chunk for simplicity.
     */
    private SseEmitter createTextStreamingResponse(String text) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        String completionId = "chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

        streamingExecutor.submit(() -> {
            try {
                // Send initial role chunk
                StreamingChatCompletionResponse roleChunk = StreamingChatCompletionResponse.builder()
                    .id(completionId)
                    .object("chat.completion.chunk")
                    .created(System.currentTimeMillis() / 1000)
                    .model(MODEL_ID)
                    .choices(Collections.singletonList(StreamChoice.role(0, "assistant")))
                    .build();
                sendSseEvent(emitter, roleChunk);

                // Send content chunk
                StreamingChatCompletionResponse contentChunk = StreamingChatCompletionResponse.builder()
                    .id(completionId)
                    .object("chat.completion.chunk")
                    .created(System.currentTimeMillis() / 1000)
                    .model(MODEL_ID)
                    .choices(Collections.singletonList(StreamChoice.content(0, text)))
                    .build();
                sendSseEvent(emitter, contentChunk);

                // Send finish chunk
                StreamingChatCompletionResponse finishChunk = StreamingChatCompletionResponse.builder()
                    .id(completionId)
                    .object("chat.completion.chunk")
                    .created(System.currentTimeMillis() / 1000)
                    .model(MODEL_ID)
                    .choices(Collections.singletonList(StreamChoice.finish(0, "stop")))
                    .build();
                sendSseEvent(emitter, finishChunk);

                // Send done signal
                emitter.send(SseEmitter.event().data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                log.error("Error in text streaming: {}", e.getMessage());
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * List available models - OpenAI compatible.
     */
    @GetMapping("/models")
    public ResponseEntity<ModelListResponse> listModels() {
        log.debug("Listing available models");
        return ResponseEntity.ok(ModelListResponse.defaultList());
    }

    /**
     * Get model details - OpenAI compatible.
     */
    @GetMapping("/models/{modelId}")
    public ResponseEntity<?> getModel(@PathVariable String modelId) {
        log.debug("Getting model details for: {}", modelId);

        if (MODEL_ID.equals(modelId)) {
            return ResponseEntity.ok(ModelInfo.bmcRemedyRag());
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
            "error", Map.of(
                "message", "The model '" + modelId + "' does not exist",
                "type", "invalid_request_error",
                "code", "model_not_found"
            )
        ));
    }

    /**
     * Rate limit fallback for chat completions.
     */
    @SuppressWarnings("unused")
    private Object chatCompletionRateLimitFallback(
            String sessionId, String authorization, ChatCompletionRequest request,
            jakarta.servlet.http.HttpServletResponse response, RequestNotPermitted ex) {
        log.warn("Rate limit exceeded for OpenAI chat completions endpoint");

        if (request != null && request.isStream()) {
            response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
            return createErrorSseEmitter("Rate limit exceeded. Please wait and try again.");
        }

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
            "error", Map.of(
                "message", "Rate limit exceeded. Please wait and try again.",
                "type", "rate_limit_error",
                "code", "rate_limit_exceeded"
            )
        ));
    }

    /**
     * Extract action ID from conversation history.
     * Looks for patterns like "Action ID: `abc123`" in assistant messages.
     */
    private String extractActionIdFromHistory(List<ChatMessage> messages) {
        if (messages == null) return null;

        // Look through messages in reverse order (most recent first)
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if ("assistant".equals(msg.getRole()) && msg.getContent() != null) {
                // Look for action ID pattern
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "\\*\\*Action ID:\\*\\*\\s*`([a-f0-9]+)`"
                );
                java.util.regex.Matcher matcher = pattern.matcher(msg.getContent());
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        return null;
    }

    /**
     * Execute a tool directly and return the result as formatted text.
     * This enables agentic behavior without requiring explicit tool calls.
     *
     * @param intent The detected intent with parameters
     * @param sessionId The session ID for tracking
     * @param actionIdFromHistory Optional action ID extracted from conversation history
     */
    private String executeToolDirectly(IntentResult intent, String sessionId, String actionIdFromHistory) {
        try {
            switch (intent.getIntent()) {
                case CREATE_INCIDENT:
                    return executeCreateIncident(intent, sessionId);
                case SEARCH_INCIDENTS:
                    return executeSearchIncidents(intent);
                case GET_INCIDENT:
                    return executeGetIncident(intent);
                case SERVICE_REQUEST:
                    return executeServiceRequest(intent, sessionId);
                case CONFIRM:
                    return executeConfirmAction(sessionId, actionIdFromHistory);
                case CANCEL:
                    return executeCancelAction(sessionId, actionIdFromHistory);
                default:
                    return null;
            }
        } catch (Exception e) {
            log.error("Error executing tool: {}", e.getMessage(), e);
            return "**Error:** Failed to execute action: " + e.getMessage();
        }
    }

    /**
     * Execute service request using the Damee guided service creator.
     */
    private String executeServiceRequest(IntentResult intent, String sessionId) {
        String query = intent.getParameters().get("query");

        if (query == null || query.isBlank()) {
            return null; // Fall back to RAG
        }

        log.info("Executing service_request: query='{}', session={}", query, sessionId);

        try {
            // Use the guided service creator to process the message
            GuidedResponse response = guidedServiceCreator.processMessage(
                sessionId,
                "user-" + sessionId, // userId derived from session
                query
            );

            if (response == null) {
                return null; // Fall back to RAG
            }

            // Build the response text
            StringBuilder sb = new StringBuilder();
            sb.append(response.getMessage());

            // Add options if available
            if (response.getOptions() != null && !response.getOptions().isEmpty()) {
                sb.append("\n\n");
                for (int i = 0; i < response.getOptions().size(); i++) {
                    GuidedResponse.Option opt = response.getOptions().get(i);
                    sb.append(String.format("**%d.** %s\n", i + 1, opt.getLabel()));
                }
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("Failed to process service request: {}", e.getMessage(), e);
            return "**Error:** Failed to process service request: " + e.getMessage();
        }
    }

    /**
     * Execute create incident tool by calling the tool server.
     */
    private String executeCreateIncident(IntentResult intent, String sessionId) {
        String summary = intent.getParameters().get("summary");
        String description = intent.getParameters().get("description");

        if (summary == null || summary.isBlank()) {
            return null; // Fall back to RAG
        }

        log.info("Executing create_incident tool: summary='{}', session={}", summary, sessionId);

        // Call the tool server endpoint internally
        String toolServerUrl = "http://localhost:8080/tool-server/incidents";

        try {
            // Build request
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("summary", summary);
            requestBody.put("description", description != null ? description : summary);
            requestBody.put("impact", 3);
            requestBody.put("urgency", 3);
            requestBody.put("sessionId", sessionId);
            requestBody.put("requireConfirmation", true);

            // Make HTTP request to tool server
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.set("X-Session-Id", sessionId);

            org.springframework.http.HttpEntity<Map<String, Object>> entity =
                new org.springframework.http.HttpEntity<>(requestBody, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(toolServerUrl, entity, Map.class);

            if (response == null) {
                return "**Error:** No response from incident creation service.";
            }

            // Format the response
            String status = (String) response.get("status");

            if ("STAGED".equals(status)) {
                StringBuilder sb = new StringBuilder();
                sb.append("# Incident Staged for Confirmation\n\n");
                sb.append(response.get("preview"));
                sb.append("\n\n");
                sb.append("**Action ID:** `").append(response.get("actionId")).append("`\n");
                sb.append("**Expires:** ").append(response.get("expiresAt")).append("\n\n");
                sb.append("> To confirm and create this incident, reply with **\"confirm\"**\n");
                sb.append("> To cancel, reply with **\"cancel\"**");
                return sb.toString();
            } else if ("DUPLICATE_WARNING".equals(status)) {
                StringBuilder sb = new StringBuilder();
                sb.append("# Potential Duplicates Found\n\n");
                sb.append("> **Warning:** Similar incidents exist. Review before confirming.\n\n");
                sb.append(response.get("preview"));
                sb.append("\n\n**Action ID:** `").append(response.get("actionId")).append("`\n\n");
                sb.append("> To create anyway, reply with **\"confirm\"**\n");
                sb.append("> To cancel, reply with **\"cancel\"**");
                return sb.toString();
            } else if ("CREATED".equals(status)) {
                return "**Success!** Incident **" + response.get("incidentNumber") + "** has been created.";
            } else if ("RATE_LIMITED".equals(status)) {
                return "**Rate Limit Exceeded:** " + response.get("message");
            } else {
                return "**Error:** " + response.getOrDefault("message", "Unknown error occurred");
            }
        } catch (Exception e) {
            log.error("Failed to call tool server: {}", e.getMessage(), e);
            return "**Error:** Failed to stage incident: " + e.getMessage();
        }
    }

    /**
     * Execute search incidents using the tool server.
     */
    private String executeSearchIncidents(IntentResult intent) {
        String query = intent.getParameters().get("query");

        if (query == null || query.isBlank()) {
            return null; // Fall back to RAG
        }

        log.info("Executing search_incidents tool: query='{}'", query);

        String toolServerUrl = "http://localhost:8080/tool-server/incidents/search";

        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            // Build the search request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", query);
            requestBody.put("limit", 5);
            requestBody.put("minScore", 0.3);

            org.springframework.http.HttpEntity<Map<String, Object>> entity =
                new org.springframework.http.HttpEntity<>(requestBody, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(toolServerUrl, entity, Map.class);

            if (response == null) {
                return "**Error:** No response from search service.";
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");

            if (results == null || results.isEmpty()) {
                return "**No incidents found** matching \"" + query + "\".";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("# Search Results for \"").append(query).append("\"\n\n");
            sb.append("Found **").append(results.size()).append("** incident(s):\n\n");

            for (Map<String, Object> incident : results) {
                String incidentId = (String) incident.get("id");
                String title = (String) incident.get("title");
                String status = (String) incident.get("status");
                Object scoreObj = incident.get("score");
                Double score = scoreObj instanceof Number ? ((Number) scoreObj).doubleValue() : null;

                sb.append("### ").append(incidentId).append("\n");
                sb.append("- **Summary:** ").append(title != null ? title : "N/A").append("\n");
                sb.append("- **Status:** ").append(status != null ? status : "N/A").append("\n");
                if (score != null) {
                    sb.append("- **Similarity:** ").append(Math.round(score * 100)).append("%\n");
                }
                sb.append("\n");
            }

            sb.append("---\n");
            sb.append("_To view details of a specific incident, say \"show incident INC000XXXXX\"_");

            return sb.toString();
        } catch (Exception e) {
            log.error("Failed to search incidents: {}", e.getMessage(), e);
            return "**Error:** Failed to search incidents: " + e.getMessage();
        }
    }

    /**
     * Execute get incident details using the tool server.
     */
    private String executeGetIncident(IntentResult intent) {
        String incidentId = intent.getParameters().get("incident_id");

        if (incidentId == null || incidentId.isBlank()) {
            return null; // Fall back to RAG
        }

        log.info("Executing get_incident tool: incidentId='{}'", incidentId);

        String toolServerUrl = "http://localhost:8080/tool-server/incidents/" + incidentId;

        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

            @SuppressWarnings("unchecked")
            Map<String, Object> incident = restTemplate.getForObject(toolServerUrl, Map.class);

            if (incident == null) {
                return "**Incident not found:** " + incidentId;
            }

            // Check if found
            Boolean found = (Boolean) incident.get("found");
            if (found != null && !found) {
                String errorMsg = (String) incident.get("errorMessage");
                return "**Incident not found:** " + (errorMsg != null ? errorMsg : incidentId);
            }

            StringBuilder sb = new StringBuilder();
            String incidentNumber = (String) incident.get("incidentNumber");
            sb.append("# Incident ").append(incidentNumber != null ? incidentNumber : incidentId).append("\n\n");

            String summary = (String) incident.get("summary");
            String description = (String) incident.get("description");
            String status = (String) incident.get("status");
            String priorityLabel = (String) incident.get("priorityLabel");
            String impactLabel = (String) incident.get("impactLabel");
            String urgencyLabel = (String) incident.get("urgencyLabel");
            String assignedGroup = (String) incident.get("assignedGroup");
            String assignedTo = (String) incident.get("assignedTo");
            String resolution = (String) incident.get("resolution");
            String categoryPath = (String) incident.get("categoryPath");
            String submitter = (String) incident.get("submitter");
            Object createDate = incident.get("createDate");
            Object lastModifiedDate = incident.get("lastModifiedDate");

            sb.append("| Field | Value |\n");
            sb.append("|-------|-------|\n");
            sb.append("| **Summary** | ").append(summary != null ? summary : "N/A").append(" |\n");
            sb.append("| **Status** | ").append(status != null ? status : "N/A").append(" |\n");
            sb.append("| **Priority** | ").append(priorityLabel != null ? priorityLabel : "N/A").append(" |\n");
            sb.append("| **Impact** | ").append(impactLabel != null ? impactLabel : "N/A").append(" |\n");
            sb.append("| **Urgency** | ").append(urgencyLabel != null ? urgencyLabel : "N/A").append(" |\n");
            sb.append("| **Category** | ").append(categoryPath != null ? categoryPath : "N/A").append(" |\n");
            sb.append("| **Assigned Group** | ").append(assignedGroup != null ? assignedGroup : "N/A").append(" |\n");
            sb.append("| **Assigned To** | ").append(assignedTo != null ? assignedTo : "N/A").append(" |\n");
            sb.append("| **Submitter** | ").append(submitter != null ? submitter : "N/A").append(" |\n");
            sb.append("| **Created** | ").append(createDate != null ? createDate.toString() : "N/A").append(" |\n");
            sb.append("| **Last Modified** | ").append(lastModifiedDate != null ? lastModifiedDate.toString() : "N/A").append(" |\n");

            if (description != null && !description.isBlank()) {
                sb.append("\n**Description:**\n").append(description).append("\n");
            }

            if (resolution != null && !resolution.isBlank()) {
                sb.append("\n**Resolution:**\n").append(resolution).append("\n");
            }

            return sb.toString();
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            return "**Incident not found:** " + incidentId + " does not exist or you don't have access.";
        } catch (Exception e) {
            log.error("Failed to get incident: {}", e.getMessage(), e);
            return "**Error:** Failed to retrieve incident: " + e.getMessage();
        }
    }

    /**
     * Execute confirm action by finding and confirming the latest pending action.
     *
     * @param sessionId The session ID for looking up pending actions
     * @param actionIdFromHistory Optional action ID extracted from conversation history
     */
    private String executeConfirmAction(String sessionId, String actionIdFromHistory) {
        log.info("Executing confirm action for session: {}, actionIdFromHistory: {}", sessionId, actionIdFromHistory);

        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String actionId;

            // If we have an action ID from conversation history, use it directly
            if (actionIdFromHistory != null && !actionIdFromHistory.isBlank()) {
                actionId = actionIdFromHistory;
                log.info("Using action ID from conversation history: {}", actionId);
            } else {
                // Fall back to looking up pending actions for this session
                String pendingUrl = "http://localhost:8080/tool-server/actions/pending?sessionId=" + sessionId;

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> pendingActions = restTemplate.getForObject(pendingUrl, List.class);

                if (pendingActions == null || pendingActions.isEmpty()) {
                    return "**No pending actions** to confirm. Please create an incident first.";
                }

                // Get the most recent pending action
                Map<String, Object> latestAction = pendingActions.get(0);
                actionId = (String) latestAction.get("actionId");
                log.info("Using action ID from pending actions lookup: {}", actionId);
            }

            // Confirm the action
            String confirmUrl = "http://localhost:8080/tool-server/actions/confirm?actionId=" + actionId + "&sessionId=" + sessionId;

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("X-Session-Id", sessionId);
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(confirmUrl, entity, Map.class);

            if (response == null) {
                return "**Error:** No response from confirmation service.";
            }

            String status = (String) response.get("status");

            if ("EXECUTED".equals(status)) {
                String recordId = (String) response.get("recordId");
                String recordType = (String) response.getOrDefault("recordType", "Record");
                return "**Success!** " + recordType + " **" + recordId + "** has been created.";
            } else if ("EXPIRED".equals(status)) {
                return "**Action Expired:** This action has expired. Please create a new request.";
            } else if ("FAILED".equals(status)) {
                return "**Failed:** " + response.getOrDefault("message", "Could not execute action.");
            } else if ("NOT_FOUND".equals(status)) {
                return "**Action Not Found:** The action ID is invalid or has already been processed.";
            } else {
                return "**" + status + ":** " + response.getOrDefault("message", "Action processed.");
            }
        } catch (Exception e) {
            log.error("Failed to confirm action: {}", e.getMessage(), e);
            return "**Error:** Failed to confirm action: " + e.getMessage();
        }
    }

    /**
     * Execute cancel action by finding and cancelling the latest pending action.
     *
     * @param sessionId The session ID for looking up pending actions
     * @param actionIdFromHistory Optional action ID extracted from conversation history
     */
    private String executeCancelAction(String sessionId, String actionIdFromHistory) {
        log.info("Executing cancel action for session: {}, actionIdFromHistory: {}", sessionId, actionIdFromHistory);

        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String actionId;

            // If we have an action ID from conversation history, use it directly
            if (actionIdFromHistory != null && !actionIdFromHistory.isBlank()) {
                actionId = actionIdFromHistory;
                log.info("Using action ID from conversation history: {}", actionId);
            } else {
                // Fall back to looking up pending actions for this session
                String pendingUrl = "http://localhost:8080/tool-server/actions/pending?sessionId=" + sessionId;

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> pendingActions = restTemplate.getForObject(pendingUrl, List.class);

                if (pendingActions == null || pendingActions.isEmpty()) {
                    return "**No pending actions** to cancel.";
                }

                // Get the most recent pending action
                Map<String, Object> latestAction = pendingActions.get(0);
                actionId = (String) latestAction.get("actionId");
                log.info("Using action ID from pending actions lookup: {}", actionId);
            }

            // Cancel the action
            String cancelUrl = "http://localhost:8080/tool-server/actions/cancel?actionId=" + actionId + "&sessionId=" + sessionId;

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("X-Session-Id", sessionId);
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(cancelUrl, entity, Map.class);

            if (response == null) {
                return "**Error:** No response from cancellation service.";
            }

            String status = (String) response.get("status");

            if ("CANCELLED".equals(status)) {
                return "**Action Cancelled:** The pending action has been cancelled successfully.";
            } else if ("NOT_FOUND".equals(status)) {
                return "**Action Not Found:** The action ID is invalid or has already been processed.";
            } else {
                return "**" + status + ":** " + response.getOrDefault("message", "Action processed.");
            }
        } catch (Exception e) {
            log.error("Failed to cancel action: {}", e.getMessage(), e);
            return "**Error:** Failed to cancel action: " + e.getMessage();
        }
    }

    /**
     * Create a tool call response based on detected intent.
     *
     * @param intent The detected intent with parameters
     * @return ToolCallResponse or null if no valid tool call
     */
    private ToolCallResponse createToolCallResponse(IntentResult intent) {
        switch (intent.getIntent()) {
            case CREATE_INCIDENT:
                String summary = intent.getParameters().get("summary");
                String description = intent.getParameters().get("description");
                if (summary != null && !summary.isBlank()) {
                    return ToolCallResponse.createIncident(summary, description, MODEL_ID);
                }
                break;

            case SEARCH_INCIDENTS:
                String query = intent.getParameters().get("query");
                if (query != null && !query.isBlank()) {
                    return ToolCallResponse.searchIncidents(query, MODEL_ID);
                }
                break;

            case CONFIRM:
                // For confirm, we need to find the action ID from context
                // For now, return a generic confirm call that the tool will handle
                return ToolCallResponse.confirmAction("pending", MODEL_ID);

            case CANCEL:
                // Similar to confirm - the tool will look up pending actions
                return ToolCallResponse.confirmAction("cancel", MODEL_ID);

            default:
                break;
        }
        return null;
    }

    /**
     * Format source citations as a compact horizontal reference section.
     *
     * @param documents The retrieved documents with scores
     * @return Formatted markdown string for display
     */
    private String formatSourcesCitations(List<RetrievedDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n---\n");
        sb.append("**References:** ");

        List<String> refs = new ArrayList<>();
        for (RetrievedDocument doc : documents) {
            int similarityPercent = Math.round(doc.score() * 100);
            refs.add(String.format("`%s` (%d%%)", doc.sourceId(), similarityPercent));
        }

        sb.append(String.join(" · ", refs));

        return sb.toString();
    }
}
