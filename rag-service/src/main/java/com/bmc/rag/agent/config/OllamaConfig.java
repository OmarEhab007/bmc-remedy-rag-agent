package com.bmc.rag.agent.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.Duration;

/**
 * Configuration for Ollama LLM integration.
 *
 * @deprecated Replaced by ZaiConfig for Z.AI cloud API integration.
 *             Re-enable by uncommenting @Configuration if you need to use local Ollama.
 */
@Data
// @Configuration  // Disabled - using ZaiConfig for Z.AI instead
// @ConfigurationProperties(prefix = "ollama")  // Disabled
// @Validated  // Disabled
public class OllamaConfig {

    /**
     * Ollama server base URL.
     */
    @NotBlank
    private String baseUrl = "http://localhost:11434";

    /**
     * Model name to use (e.g., llama3:8b, mistral:7b).
     */
    @NotBlank
    private String model = "llama3:8b";

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
    private double topP = 0.9;

    /**
     * Number of threads for Ollama inference.
     */
    @Positive
    private int numThreads = 4;

    /**
     * Create the Ollama chat model bean.
     */
    // @Bean  // Disabled - using ZaiConfig for Z.AI instead
    public ChatLanguageModel chatLanguageModel() {
        return OllamaChatModel.builder()
            .baseUrl(baseUrl)
            .modelName(model)
            .temperature(temperature)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .numPredict(maxTokens)
            .topP(topP)
            .build();
    }

    /**
     * Create the Ollama streaming chat model bean for real-time token streaming.
     */
    // @Bean  // Disabled - using ZaiConfig for Z.AI instead
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        return OllamaStreamingChatModel.builder()
            .baseUrl(baseUrl)
            .modelName(model)
            .temperature(temperature)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .numPredict(maxTokens)
            .topP(topP)
            .build();
    }
}
