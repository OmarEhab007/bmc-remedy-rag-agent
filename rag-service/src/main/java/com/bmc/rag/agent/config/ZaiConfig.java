package com.bmc.rag.agent.config;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for Z.AI LLM integration.
 * Z.AI uses an OpenAI-compatible API format.
 *
 * Supports optional thinking mode for GLM-4.7+ models when enabled.
 * Configuration is optional - if no API key is provided, a mock model will be used.
 */
@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "zai")
@ConditionalOnProperty(name = "zai.enabled", havingValue = "true", matchIfMissing = false)
public class ZaiConfig {

    @Autowired(required = false)
    @Lazy
    private ZaiRequestInterceptor thinkingInterceptor;

    /**
     * Z.AI API key for authentication.
     * Optional - if not set, a mock response will be used.
     */
    private String apiKey = "";

    /**
     * Z.AI API base URL.
     */
    private String baseUrl = "https://api.z.ai/api/paas/v4/";

    /**
     * Model name to use (e.g., glm-4.7, glm-4.6, glm-4.5, glm-4.5-flash).
     */
    private String model = "glm-4.7";

    /**
     * Temperature for response generation (0.0 = deterministic).
     */
    private double temperature = 0.0;

    /**
     * Request timeout in seconds.
     */
    private int timeoutSeconds = 120;

    /**
     * Maximum tokens to generate in response.
     */
    private int maxTokens = 2048;

    /**
     * Top P (nucleus sampling) parameter.
     */
    private double topP = 0.95;

    /**
     * Frequency penalty to reduce repetition in responses.
     */
    private double frequencyPenalty = 0.0;

    /**
     * Maximum concurrent requests to Z.AI API (prevents rate limiting).
     */
    private int maxConcurrentRequests = 2;

    /**
     * Semaphore to limit concurrent Z.AI API calls.
     */
    private Semaphore requestSemaphore;

    /**
     * Enable thinking mode for GLM-4.7+ models.
     */
    private boolean thinkingEnabled = false;

    /**
     * Thinking type: enabled, disabled, or retention.
     */
    private String thinkingType = "enabled";

    /**
     * Check if Z.AI is properly configured with an API key.
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @PostConstruct
    public void logConfiguration() {
        // Initialize semaphore for rate limiting
        this.requestSemaphore = new Semaphore(maxConcurrentRequests);

        if (isConfigured()) {
            log.info("Z.AI LLM configured: model={}, temperature={}, maxTokens={}, topP={}, frequencyPenalty={}, maxConcurrent={}",
                model, temperature, maxTokens, topP, frequencyPenalty, maxConcurrentRequests);
            if (thinkingEnabled) {
                log.info("Thinking mode enabled: type={}", thinkingType);
            }
        } else {
            log.warn("Z.AI API key not configured - LLM features will use mock responses. Set ZAI_API_KEY environment variable to enable.");
        }
    }

    /**
     * Get the request semaphore for rate limiting Z.AI calls.
     */
    public Semaphore getRequestSemaphore() {
        return requestSemaphore;
    }

    /**
     * Create custom OkHttpClient with thinking mode interceptor.
     * The interceptor is always added to control reasoning_content in responses.
     */
    private OkHttpClient createHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS);

        // Always add interceptor to control thinking/reasoning_content behavior
        if (thinkingInterceptor != null) {
            builder.addInterceptor(thinkingInterceptor);
            log.debug("Added Z.AI request interceptor to HTTP client");
        }

        return builder.build();
    }

    /**
     * Create the Z.AI chat model bean using OpenAI-compatible client.
     * If no API key is configured, returns a mock model.
     */
    @Bean
    @Primary
    public ChatLanguageModel chatLanguageModel() {
        if (!isConfigured()) {
            log.warn("Creating mock ChatLanguageModel - set ZAI_API_KEY for real LLM responses");
            return new MockChatLanguageModel();
        }

        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(model)
            .temperature(temperature)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .maxTokens(maxTokens)
            .topP(topP)
            .frequencyPenalty(frequencyPenalty);

        // Add custom client with thinking interceptor if enabled
        if (thinkingEnabled && thinkingInterceptor != null) {
            builder.customHeaders(java.util.Map.of(
                "X-Thinking-Mode", thinkingType
            ));
            log.info("Chat model configured with thinking mode support");
        }

        return builder.build();
    }

    /**
     * Create the Z.AI streaming chat model bean for real-time token streaming.
     * Uses custom ZaiStreamingChatModel that properly handles thinking/reasoning_content.
     * If no API key is configured, returns a mock streaming model.
     */
    @Bean
    @Primary
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        if (!isConfigured()) {
            log.warn("Creating mock StreamingChatLanguageModel - set ZAI_API_KEY for real LLM responses");
            return new MockStreamingChatLanguageModel();
        }

        // Use custom Z.AI streaming model with shared OkHttpClient and semaphore for rate limiting
        log.info("Creating ZaiStreamingChatModel with thinking={}, timeout={}s, maxConcurrent={}",
            thinkingEnabled ? thinkingType : "disabled", timeoutSeconds, maxConcurrentRequests);

        return ZaiStreamingChatModel.create(
            apiKey,
            baseUrl,
            model,
            temperature,
            maxTokens,
            topP,
            frequencyPenalty,
            Duration.ofSeconds(timeoutSeconds),
            thinkingEnabled,
            thinkingType,
            requestSemaphore
        );
    }

    /**
     * Check if thinking mode is enabled.
     */
    public boolean isThinkingEnabled() {
        return thinkingEnabled;
    }

    /**
     * Get the thinking type configuration.
     */
    public String getThinkingType() {
        return thinkingType;
    }

    /**
     * Mock ChatLanguageModel for development without API key.
     */
    private static class MockChatLanguageModel implements ChatLanguageModel {
        private static final String MOCK_RESPONSE =
            "I'm running in mock mode because no API key is configured. " +
            "To get real LLM responses, please set the ZAI_API_KEY environment variable. " +
            "I can see your question, but I cannot provide an intelligent response without " +
            "connecting to the Z.AI API.";

        @Override
        public ChatResponse chat(ChatRequest request) {
            return ChatResponse.builder()
                .aiMessage(AiMessage.from(MOCK_RESPONSE))
                .build();
        }
    }

    /**
     * Mock StreamingChatLanguageModel for development without API key.
     * Simulates streaming by sending tokens word by word.
     */
    private static class MockStreamingChatLanguageModel implements StreamingChatLanguageModel {
        private static final org.slf4j.Logger mockLog =
            org.slf4j.LoggerFactory.getLogger(MockStreamingChatLanguageModel.class);
        private static final String MOCK_RESPONSE =
            "I'm running in mock mode because no API key is configured. " +
            "To get real LLM responses, please set the ZAI_API_KEY environment variable.";

        @Override
        public void chat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
            try {
                mockLog.debug("Mock streaming model generating response for request");

                // Simulate streaming by sending the response word by word
                String[] words = MOCK_RESPONSE.split(" ");
                for (int i = 0; i < words.length; i++) {
                    String token = words[i] + (i < words.length - 1 ? " " : "");
                    handler.onPartialResponse(token);
                }

                // Create the response and complete
                ChatResponse response = ChatResponse.builder()
                    .aiMessage(AiMessage.from(MOCK_RESPONSE))
                    .build();

                handler.onCompleteResponse(response);
                mockLog.debug("Mock streaming model completed successfully");

            } catch (Exception e) {
                mockLog.error("Mock streaming model error: {}", e.getMessage(), e);
                handler.onError(e);
            }
        }
    }
}
