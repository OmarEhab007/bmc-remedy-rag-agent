package com.bmc.rag.agent.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
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
        if (isConfigured()) {
            log.info("Z.AI LLM configured: model={}, temperature={}, maxTokens={}, topP={}, frequencyPenalty={}",
                model, temperature, maxTokens, topP, frequencyPenalty);
            if (thinkingEnabled) {
                log.info("Thinking mode enabled: type={}", thinkingType);
            }
        } else {
            log.warn("Z.AI API key not configured - LLM features will use mock responses. Set ZAI_API_KEY environment variable to enable.");
        }
    }

    /**
     * Create custom OkHttpClient with thinking mode interceptor if enabled.
     */
    private OkHttpClient createHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS);

        if (thinkingEnabled && thinkingInterceptor != null) {
            builder.addInterceptor(thinkingInterceptor);
            log.debug("Added thinking mode interceptor to HTTP client");
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
     * If no API key is configured, returns a mock streaming model.
     */
    @Bean
    @Primary
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        if (!isConfigured()) {
            log.warn("Creating mock StreamingChatLanguageModel - set ZAI_API_KEY for real LLM responses");
            return new MockStreamingChatLanguageModel();
        }

        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(model)
            .temperature(temperature)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .maxTokens(maxTokens)
            .topP(topP)
            .frequencyPenalty(frequencyPenalty);

        // Add custom headers for thinking mode if enabled
        if (thinkingEnabled && thinkingInterceptor != null) {
            builder.customHeaders(java.util.Map.of(
                "X-Thinking-Mode", thinkingType
            ));
            log.info("Streaming chat model configured with thinking mode support");
        }

        return builder.build();
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
        public dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> generate(
                java.util.List<dev.langchain4j.data.message.ChatMessage> messages) {
            return new dev.langchain4j.model.output.Response<>(
                new dev.langchain4j.data.message.AiMessage(MOCK_RESPONSE));
        }

        @Override
        public dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> generate(
                java.util.List<dev.langchain4j.data.message.ChatMessage> messages,
                java.util.List<dev.langchain4j.agent.tool.ToolSpecification> toolSpecifications) {
            return generate(messages);
        }

        @Override
        public dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> generate(
                java.util.List<dev.langchain4j.data.message.ChatMessage> messages,
                dev.langchain4j.agent.tool.ToolSpecification toolSpecification) {
            return generate(messages);
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
        public void generate(java.util.List<dev.langchain4j.data.message.ChatMessage> messages,
                            dev.langchain4j.model.StreamingResponseHandler<dev.langchain4j.data.message.AiMessage> handler) {
            try {
                mockLog.debug("Mock streaming model generating response for {} messages", messages.size());

                // Simulate streaming by sending the response word by word
                String[] words = MOCK_RESPONSE.split(" ");
                for (int i = 0; i < words.length; i++) {
                    String token = words[i] + (i < words.length - 1 ? " " : "");
                    handler.onNext(token);
                }

                // Create the response and complete
                dev.langchain4j.data.message.AiMessage aiMessage =
                    new dev.langchain4j.data.message.AiMessage(MOCK_RESPONSE);
                dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> response =
                    new dev.langchain4j.model.output.Response<>(aiMessage);

                handler.onComplete(response);
                mockLog.debug("Mock streaming model completed successfully");

            } catch (Exception e) {
                mockLog.error("Mock streaming model error: {}", e.getMessage(), e);
                handler.onError(e);
            }
        }

        @Override
        public void generate(java.util.List<dev.langchain4j.data.message.ChatMessage> messages,
                            java.util.List<dev.langchain4j.agent.tool.ToolSpecification> toolSpecifications,
                            dev.langchain4j.model.StreamingResponseHandler<dev.langchain4j.data.message.AiMessage> handler) {
            generate(messages, handler);
        }

        @Override
        public void generate(java.util.List<dev.langchain4j.data.message.ChatMessage> messages,
                            dev.langchain4j.agent.tool.ToolSpecification toolSpecification,
                            dev.langchain4j.model.StreamingResponseHandler<dev.langchain4j.data.message.AiMessage> handler) {
            generate(messages, handler);
        }
    }
}
