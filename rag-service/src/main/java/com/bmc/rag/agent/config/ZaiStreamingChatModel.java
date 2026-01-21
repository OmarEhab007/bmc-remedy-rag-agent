package com.bmc.rag.agent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.ConnectionPool;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Custom Z.AI streaming chat model that properly handles the thinking/reasoning_content
 * behavior by adding "thinking": {"type": "disabled"} to requests when needed.
 *
 * This is necessary because Z.AI's glm-4.5-flash model returns reasoning_content
 * by default in streaming responses, which LangChain4j's OpenAI client doesn't handle.
 */
@Slf4j
@Builder
public class ZaiStreamingChatModel implements StreamingChatLanguageModel {

    private final String apiKey;
    private final String baseUrl;
    private final String modelName;
    private final Double temperature;
    private final Integer maxTokens;
    private final Double topP;
    private final Double frequencyPenalty;
    private final Duration timeout;
    private final boolean thinkingEnabled;
    private final String thinkingType;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final MediaType JSON = MediaType.parse("application/json");

    // Reuse OkHttpClient instance for connection pooling and performance
    private final OkHttpClient client;

    // Semaphore to limit concurrent requests (prevents Z.AI rate limiting)
    private final Semaphore requestSemaphore;

    // Static factory method to build with client initialization
    public static ZaiStreamingChatModel create(String apiKey, String baseUrl, String modelName,
            Double temperature, Integer maxTokens, Double topP, Double frequencyPenalty,
            Duration timeout, boolean thinkingEnabled, String thinkingType,
            Semaphore requestSemaphore) {
        OkHttpClient sharedClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)  // Fast connection timeout
            .readTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))  // Connection reuse
            .build();
        return ZaiStreamingChatModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(modelName)
            .temperature(temperature)
            .maxTokens(maxTokens)
            .topP(topP)
            .frequencyPenalty(frequencyPenalty)
            .timeout(timeout)
            .thinkingEnabled(thinkingEnabled)
            .thinkingType(thinkingType)
            .client(sharedClient)
            .requestSemaphore(requestSemaphore)
            .build();
    }

    @Override
    public void chat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
        List<ChatMessage> messages = chatRequest.messages();

        // Acquire semaphore to limit concurrent requests (prevents Z.AI rate limiting)
        boolean acquired = false;
        try {
            if (requestSemaphore != null) {
                log.debug("Waiting for Z.AI request slot (available: {})", requestSemaphore.availablePermits());
                acquired = requestSemaphore.tryAcquire(30, TimeUnit.SECONDS);
                if (!acquired) {
                    log.warn("Timeout waiting for Z.AI request slot - too many concurrent requests");
                    handler.onError(new RuntimeException("Z.AI request queue full - please try again"));
                    return;
                }
                log.debug("Acquired Z.AI request slot (remaining: {})", requestSemaphore.availablePermits());
            }

            chatInternal(messages, handler, acquired);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handler.onError(e);
            if (acquired && requestSemaphore != null) {
                requestSemaphore.release();
            }
        }
    }

    private void chatInternal(List<ChatMessage> messages, StreamingChatResponseHandler handler, boolean semaphoreAcquired) {
        try {
            String requestBody = buildRequestBody(messages);
            int inputChars = messages.stream()
                .mapToInt(m -> {
                    if (m instanceof SystemMessage) return ((SystemMessage) m).text().length();
                    if (m instanceof UserMessage) return ((UserMessage) m).singleText().length();
                    if (m instanceof AiMessage) return ((AiMessage) m).text().length();
                    return 0;
                })
                .sum();
            // Approximate token count (1 token ≈ 4 chars for English, less for Chinese)
            int approxInputTokens = inputChars / 3;
            log.info("[LATENCY] Z.AI request: model={}, inputChars={}, approxTokens={}, maxTokens={}",
                modelName, inputChars, approxInputTokens, maxTokens);
            log.debug("Z.AI streaming request: {}", requestBody.substring(0, Math.min(500, requestBody.length())));

            Request request = new Request.Builder()
                .url(baseUrl + "chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .post(RequestBody.create(requestBody, JSON))
                .build();

            StringBuilder fullResponse = new StringBuilder();
            // Track whether completion has been signaled to prevent double onComplete
            AtomicBoolean completed = new AtomicBoolean(false);
            // Track whether semaphore has been released
            AtomicBoolean semaphoreReleased = new AtomicBoolean(false);

            // Helper to release semaphore once
            Runnable releaseSemaphore = () -> {
                if (semaphoreAcquired && requestSemaphore != null && semaphoreReleased.compareAndSet(false, true)) {
                    requestSemaphore.release();
                    log.debug("Released Z.AI request slot (available: {})", requestSemaphore.availablePermits());
                }
            };

            // Timing instrumentation
            final long requestStartTime = System.currentTimeMillis();
            final long[] firstTokenTime = {0}; // Array to allow mutation in lambda
            final int[] tokenCount = {0};

            EventSource.Factory factory = EventSources.createFactory(client);
            factory.newEventSource(request, new EventSourceListener() {
                @Override
                public void onOpen(EventSource eventSource, okhttp3.Response response) {
                    long connectionTime = System.currentTimeMillis() - requestStartTime;
                    log.info("[LATENCY] Z.AI connection established in {}ms (status: {})", connectionTime, response.code());
                }

                @Override
                public void onEvent(EventSource eventSource, String id, String type, String data) {
                    if ("[DONE]".equals(data)) {
                        // Stream complete - signal only if not already completed
                        if (completed.compareAndSet(false, true)) {
                            releaseSemaphore.run();  // Release slot for next request
                            long totalTime = System.currentTimeMillis() - requestStartTime;
                            long streamingTime = totalTime - (firstTokenTime[0] > 0 ? firstTokenTime[0] - requestStartTime : 0);
                            log.info("[LATENCY] Z.AI streaming complete: totalTime={}ms, TTFT={}ms, tokens={}, throughput={} tokens/sec",
                                totalTime,
                                firstTokenTime[0] > 0 ? firstTokenTime[0] - requestStartTime : 0,
                                tokenCount[0],
                                streamingTime > 0 ? (tokenCount[0] * 1000L / streamingTime) : 0);
                            AiMessage aiMessage = AiMessage.from(fullResponse.toString());
                            ChatResponse response = ChatResponse.builder()
                                .aiMessage(aiMessage)
                                .build();
                            handler.onCompleteResponse(response);
                        }
                        return;
                    }

                    try {
                        JsonNode root = MAPPER.readTree(data);
                        JsonNode choices = root.get("choices");
                        if (choices != null && choices.isArray() && choices.size() > 0) {
                            JsonNode delta = choices.get(0).get("delta");
                            if (delta != null) {
                                // Extract content (the actual response text)
                                JsonNode contentNode = delta.get("content");
                                if (contentNode != null && !contentNode.isNull()) {
                                    String content = contentNode.asText();
                                    if (!content.isEmpty()) {
                                        // Track first token time
                                        if (firstTokenTime[0] == 0) {
                                            firstTokenTime[0] = System.currentTimeMillis();
                                            log.info("[LATENCY] Z.AI Time To First Token (TTFT): {}ms", firstTokenTime[0] - requestStartTime);
                                        }
                                        tokenCount[0]++;
                                        fullResponse.append(content);
                                        handler.onPartialResponse(content);
                                    }
                                }

                                // If thinking is enabled, also process reasoning_content
                                if (thinkingEnabled) {
                                    JsonNode reasoningNode = delta.get("reasoning_content");
                                    if (reasoningNode != null && !reasoningNode.isNull()) {
                                        // Optionally log reasoning but don't send to client
                                        log.trace("Reasoning: {}", reasoningNode.asText());
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse SSE event: {}", e.getMessage());
                    }
                }

                @Override
                public void onFailure(EventSource eventSource, Throwable t, okhttp3.Response response) {
                    // Only handle if not already completed
                    if (completed.get()) {
                        log.debug("Ignoring failure after completion");
                        return;
                    }

                    String errorMsg = "Z.AI streaming failed";
                    if (response != null) {
                        try {
                            String body = response.body() != null ? response.body().string() : "no body";
                            errorMsg = String.format("Z.AI streaming failed: %d - %s", response.code(), body);
                        } catch (IOException e) {
                            errorMsg = String.format("Z.AI streaming failed: %d", response.code());
                        }
                    }
                    log.error(errorMsg, t);

                    // If we have partial content, complete with it
                    if (completed.compareAndSet(false, true)) {
                        releaseSemaphore.run();  // Release slot for next request
                        if (fullResponse.length() > 0) {
                            AiMessage aiMessage = AiMessage.from(fullResponse.toString());
                            ChatResponse chatResponse = ChatResponse.builder()
                                .aiMessage(aiMessage)
                                .build();
                            handler.onCompleteResponse(chatResponse);
                        } else {
                            handler.onError(t != null ? t : new RuntimeException(errorMsg));
                        }
                    }
                }

                @Override
                public void onClosed(EventSource eventSource) {
                    log.debug("Z.AI streaming connection closed");
                    releaseSemaphore.run();  // Always release on close
                    // Ensure completion is called if not already (fallback for connections that close without [DONE])
                    if (completed.compareAndSet(false, true)) {
                        if (fullResponse.length() > 0) {
                            AiMessage aiMessage = AiMessage.from(fullResponse.toString());
                            ChatResponse chatResponse = ChatResponse.builder()
                                .aiMessage(aiMessage)
                                .build();
                            handler.onCompleteResponse(chatResponse);
                        }
                    }
                }
            });

        } catch (Exception e) {
            log.error("Failed to initiate Z.AI streaming: {}", e.getMessage(), e);
            // Release semaphore on error
            if (semaphoreAcquired && requestSemaphore != null) {
                requestSemaphore.release();
                log.debug("Released Z.AI request slot on error");
            }
            handler.onError(e);
        }
    }

    private String buildRequestBody(List<ChatMessage> messages) throws Exception {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("model", modelName);
        root.put("stream", true);

        if (temperature != null) {
            root.put("temperature", temperature);
        }
        if (maxTokens != null) {
            root.put("max_tokens", maxTokens);
        }
        if (topP != null) {
            root.put("top_p", topP);
        }
        if (frequencyPenalty != null) {
            root.put("frequency_penalty", frequencyPenalty);
        }

        // Add thinking configuration - disable to prevent reasoning_content in streaming
        ObjectNode thinking = MAPPER.createObjectNode();
        if (thinkingEnabled) {
            thinking.put("type", thinkingType != null ? thinkingType : "enabled");
        } else {
            thinking.put("type", "disabled");
        }
        root.set("thinking", thinking);

        // Build messages array
        ArrayNode messagesArray = MAPPER.createArrayNode();
        for (ChatMessage message : messages) {
            ObjectNode msgNode = MAPPER.createObjectNode();

            if (message instanceof SystemMessage) {
                msgNode.put("role", "system");
                msgNode.put("content", ((SystemMessage) message).text());
            } else if (message instanceof UserMessage) {
                msgNode.put("role", "user");
                msgNode.put("content", ((UserMessage) message).singleText());
            } else if (message instanceof AiMessage) {
                msgNode.put("role", "assistant");
                msgNode.put("content", ((AiMessage) message).text());
            }

            messagesArray.add(msgNode);
        }
        root.set("messages", messagesArray);

        return MAPPER.writeValueAsString(root);
    }
}
