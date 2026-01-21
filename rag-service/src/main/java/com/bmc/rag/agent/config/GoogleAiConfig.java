package com.bmc.rag.agent.config;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Configuration for Google AI (Gemini) LLM integration.
 * Uses LangChain4j's Google AI Gemini integration.
 *
 * Enabled when google-ai.enabled=true in application.yml.
 */
@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "google-ai")
@ConditionalOnProperty(name = "google-ai.enabled", havingValue = "true")
public class GoogleAiConfig {

    /**
     * Enable/disable Google AI integration.
     */
    private boolean enabled = false;

    /**
     * Google AI API key for authentication.
     */
    private String apiKey = "";

    /**
     * Model name to use (e.g., gemini-1.5-flash, gemini-1.5-pro, gemini-2.0-flash-exp).
     */
    private String model = "gemini-1.5-flash";

    /**
     * Temperature for response generation (0.0 = deterministic).
     */
    private double temperature = 0.1;

    /**
     * Maximum tokens to generate in response.
     */
    private int maxTokens = 4096;

    /**
     * Top P (nucleus sampling) parameter.
     */
    private double topP = 0.8;

    /**
     * Maximum concurrent requests to prevent rate limiting.
     */
    private int maxConcurrentRequests = 5;

    /**
     * Semaphore to limit concurrent API calls.
     */
    private Semaphore requestSemaphore;

    // Executor for async streaming simulation
    private final ExecutorService streamingExecutor = Executors.newCachedThreadPool();

    /**
     * Check if Google AI is properly configured.
     */
    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    @PostConstruct
    public void logConfiguration() {
        this.requestSemaphore = new Semaphore(maxConcurrentRequests);

        if (isConfigured()) {
            log.info("Google AI (Gemini) configured: model={}, temperature={}, maxTokens={}, topP={}, maxConcurrent={}",
                model, temperature, maxTokens, topP, maxConcurrentRequests);
        } else {
            log.info("Google AI is disabled or not configured");
        }
    }

    /**
     * Get the request semaphore for rate limiting.
     */
    public Semaphore getRequestSemaphore() {
        return requestSemaphore;
    }

    /**
     * Create the Google AI chat model bean.
     */
    @Bean
    @Primary
    public ChatLanguageModel chatLanguageModel() {
        if (!isConfigured()) {
            log.warn("Google AI not configured - returning mock model");
            return new MockChatLanguageModel();
        }

        log.info("Creating Google AI Gemini ChatModel: {}", model);

        return GoogleAiGeminiChatModel.builder()
            .apiKey(apiKey)
            .modelName(model)
            .temperature(temperature)
            .maxOutputTokens(maxTokens)
            .topP(topP)
            .build();
    }

    /**
     * Create a streaming wrapper around the non-streaming model.
     * Google AI's streaming model has a different API, so we wrap the sync model.
     */
    @Bean
    @Primary
    public StreamingChatLanguageModel streamingChatLanguageModel(ChatLanguageModel chatModel) {
        if (!isConfigured()) {
            log.warn("Google AI not configured - returning mock streaming model");
            return new MockStreamingChatLanguageModel();
        }

        log.info("Creating Google AI streaming wrapper for: {}", model);

        // Wrap the synchronous model to simulate streaming
        return new StreamingChatLanguageModel() {
            @Override
            public void chat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
                streamingExecutor.submit(() -> {
                    try {
                        // Call the synchronous model
                        ChatResponse response = chatModel.chat(chatRequest);
                        String text = response.aiMessage().text();

                        // Simulate streaming by sending chunks
                        int chunkSize = 20; // characters per chunk
                        for (int i = 0; i < text.length(); i += chunkSize) {
                            String chunk = text.substring(i, Math.min(i + chunkSize, text.length()));
                            handler.onPartialResponse(chunk);
                        }

                        handler.onCompleteResponse(response);
                    } catch (Exception e) {
                        log.error("Google AI streaming error: {}", e.getMessage(), e);
                        handler.onError(e);
                    }
                });
            }
        };
    }

    /**
     * Dummy method for compatibility.
     */
    public boolean isThinkingEnabled() {
        return false;
    }

    /**
     * Mock ChatLanguageModel for when Google AI is not configured.
     */
    private static class MockChatLanguageModel implements ChatLanguageModel {
        private static final String MOCK_RESPONSE =
            "Google AI is not configured. Please set google-ai.api-key in application.yml.";

        @Override
        public ChatResponse chat(ChatRequest request) {
            return ChatResponse.builder()
                .aiMessage(AiMessage.from(MOCK_RESPONSE))
                .build();
        }
    }

    /**
     * Mock StreamingChatLanguageModel for when Google AI is not configured.
     */
    private static class MockStreamingChatLanguageModel implements StreamingChatLanguageModel {
        private static final String MOCK_RESPONSE =
            "Google AI is not configured. Please set google-ai.api-key in application.yml.";

        @Override
        public void chat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
            String[] words = MOCK_RESPONSE.split(" ");
            for (int i = 0; i < words.length; i++) {
                handler.onPartialResponse(words[i] + (i < words.length - 1 ? " " : ""));
            }
            handler.onCompleteResponse(ChatResponse.builder()
                .aiMessage(AiMessage.from(MOCK_RESPONSE))
                .build());
        }
    }
}
