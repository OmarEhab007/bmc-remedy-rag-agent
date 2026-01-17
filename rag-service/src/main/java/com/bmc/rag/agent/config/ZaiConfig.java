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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for Z.AI LLM integration.
 * Z.AI uses an OpenAI-compatible API format.
 *
 * Supports optional thinking mode for GLM-4.7+ models when enabled.
 */
@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "zai")
@Validated
public class ZaiConfig {

    @Autowired(required = false)
    @Lazy
    private ZaiRequestInterceptor thinkingInterceptor;

    /**
     * Z.AI API key for authentication.
     */
    @NotBlank
    private String apiKey;

    /**
     * Z.AI API base URL.
     */
    private String baseUrl = "https://api.z.ai/api/paas/v4/";

    /**
     * Model name to use (e.g., glm-4.7, glm-4.6, glm-4.5, glm-4.5-flash).
     */
    @NotBlank
    private String model = "glm-4.7";

    /**
     * Temperature for response generation (0.0 = deterministic).
     */
    private double temperature = 0.0;

    /**
     * Request timeout in seconds.
     */
    @Positive
    private int timeoutSeconds = 120;

    /**
     * Maximum tokens to generate in response.
     */
    @Positive
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

    @PostConstruct
    public void logConfiguration() {
        log.info("Z.AI LLM configured: model={}, temperature={}, maxTokens={}, topP={}, frequencyPenalty={}",
            model, temperature, maxTokens, topP, frequencyPenalty);
        if (thinkingEnabled) {
            log.info("Thinking mode enabled: type={}", thinkingType);
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
     */
    @Bean
    @Primary
    public ChatLanguageModel chatLanguageModel() {
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
     */
    @Bean
    @Primary
    public StreamingChatLanguageModel streamingChatLanguageModel() {
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
}
