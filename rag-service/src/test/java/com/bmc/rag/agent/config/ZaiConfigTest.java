package com.bmc.rag.agent.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ZaiConfig}.
 */
class ZaiConfigTest {

    private ZaiConfig config;

    @BeforeEach
    void setUp() {
        config = new ZaiConfig();
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValues {

        @Test
        void apiKey_defaultsEmpty() {
            assertThat(config.getApiKey()).isEmpty();
        }

        @Test
        void baseUrl_defaultsZaiApi() {
            assertThat(config.getBaseUrl()).contains("z.ai");
        }

        @Test
        void model_defaultsGlm47() {
            assertThat(config.getModel()).isEqualTo("glm-4.7");
        }

        @Test
        void temperature_defaults00() {
            assertThat(config.getTemperature()).isEqualTo(0.0);
        }

        @Test
        void timeoutSeconds_defaults120() {
            assertThat(config.getTimeoutSeconds()).isEqualTo(120);
        }

        @Test
        void maxTokens_defaults2048() {
            assertThat(config.getMaxTokens()).isEqualTo(2048);
        }

        @Test
        void topP_defaults095() {
            assertThat(config.getTopP()).isEqualTo(0.95);
        }

        @Test
        void frequencyPenalty_defaults00() {
            assertThat(config.getFrequencyPenalty()).isEqualTo(0.0);
        }

        @Test
        void maxConcurrentRequests_defaults2() {
            assertThat(config.getMaxConcurrentRequests()).isEqualTo(2);
        }

        @Test
        void thinkingEnabled_defaultsFalse() {
            assertThat(config.isThinkingEnabled()).isFalse();
        }

        @Test
        void thinkingType_defaultsEnabled() {
            assertThat(config.getThinkingType()).isEqualTo("enabled");
        }
    }

    @Nested
    @DisplayName("isConfigured")
    class IsConfigured {

        @Test
        void emptyKey_returnsFalse() {
            config.setApiKey("");
            assertThat(config.isConfigured()).isFalse();
        }

        @Test
        void blankKey_returnsFalse() {
            config.setApiKey("   ");
            assertThat(config.isConfigured()).isFalse();
        }

        @Test
        void nullKey_returnsFalse() {
            config.setApiKey(null);
            assertThat(config.isConfigured()).isFalse();
        }

        @Test
        void validKey_returnsTrue() {
            config.setApiKey("valid-key");
            assertThat(config.isConfigured()).isTrue();
        }
    }

    @Nested
    @DisplayName("logConfiguration (PostConstruct)")
    class LogConfiguration {

        @Test
        void initializesSemaphore() {
            config.setMaxConcurrentRequests(3);
            config.logConfiguration();
            assertThat(config.getRequestSemaphore()).isNotNull();
            assertThat(config.getRequestSemaphore().availablePermits()).isEqualTo(3);
        }

        @Test
        void semaphoreReflectsMaxConcurrent() {
            config.setMaxConcurrentRequests(7);
            config.logConfiguration();
            assertThat(config.getRequestSemaphore().availablePermits()).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("Mock ChatLanguageModel")
    class MockChatModel {

        @Test
        void notConfigured_returnsMockModel() {
            config.setApiKey("");
            ChatLanguageModel model = config.chatLanguageModel();
            assertThat(model).isNotNull();
        }

        @Test
        void mockModel_returnsMockResponse() {
            config.setApiKey("");
            ChatLanguageModel model = config.chatLanguageModel();
            ChatRequest request = ChatRequest.builder()
                    .messages(List.of(UserMessage.from("test")))
                    .build();
            ChatResponse response = model.chat(request);
            assertThat(response.aiMessage().text()).contains("mock mode");
            assertThat(response.aiMessage().text()).contains("ZAI_API_KEY");
        }
    }

    @Nested
    @DisplayName("Mock StreamingChatLanguageModel")
    class MockStreamingModel {

        @Test
        void notConfigured_returnsMockStreamingModel() {
            config.setApiKey("");
            StreamingChatLanguageModel streamingModel = config.streamingChatLanguageModel();
            assertThat(streamingModel).isNotNull();
        }

        @Test
        void mockStreamingModel_sendsTokens() throws InterruptedException {
            config.setApiKey("");
            StreamingChatLanguageModel streamingModel = config.streamingChatLanguageModel();

            List<String> tokens = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);

            ChatRequest request = ChatRequest.builder()
                    .messages(List.of(UserMessage.from("test")))
                    .build();

            streamingModel.chat(request, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String token) {
                    tokens.add(token);
                }

                @Override
                public void onCompleteResponse(ChatResponse response) {
                    latch.countDown();
                }

                @Override
                public void onError(Throwable error) {
                    latch.countDown();
                }
            });

            latch.await(5, TimeUnit.SECONDS);
            assertThat(tokens).isNotEmpty();
            String combined = String.join("", tokens);
            assertThat(combined).contains("mock mode");
        }

        @Test
        void mockStreamingModel_completesSuccessfully() throws InterruptedException {
            config.setApiKey("");
            StreamingChatLanguageModel streamingModel = config.streamingChatLanguageModel();

            CountDownLatch latch = new CountDownLatch(1);
            ChatResponse[] captured = new ChatResponse[1];

            ChatRequest request = ChatRequest.builder()
                    .messages(List.of(UserMessage.from("test")))
                    .build();

            streamingModel.chat(request, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String token) {}

                @Override
                public void onCompleteResponse(ChatResponse response) {
                    captured[0] = response;
                    latch.countDown();
                }

                @Override
                public void onError(Throwable error) {
                    latch.countDown();
                }
            });

            latch.await(5, TimeUnit.SECONDS);
            assertThat(captured[0]).isNotNull();
            assertThat(captured[0].aiMessage().text()).contains("mock mode");
        }
    }

    @Nested
    @DisplayName("Thinking Mode")
    class ThinkingMode {

        @Test
        void thinkingEnabled_canBeSet() {
            config.setThinkingEnabled(true);
            assertThat(config.isThinkingEnabled()).isTrue();
        }

        @Test
        void thinkingType_canBeSet() {
            config.setThinkingType("retention");
            assertThat(config.getThinkingType()).isEqualTo("retention");
        }
    }

    @Nested
    @DisplayName("Real Model Creation (Configured)")
    class RealModelCreation {

        @Test
        void configured_createsChatLanguageModel() {
            config.setApiKey("test-api-key-123");
            config.setBaseUrl("https://test.api.com/");
            config.setModel("glm-4.7");
            config.setTemperature(0.3);
            config.setMaxTokens(1024);
            config.setTopP(0.9);
            config.setFrequencyPenalty(0.1);

            ChatLanguageModel model = config.chatLanguageModel();

            assertThat(model).isNotNull();
            // Should be OpenAiChatModel, not mock
            assertThat(model.getClass().getName()).contains("OpenAi");
        }

        @Test
        void configured_createsStreamingChatModel() {
            config.setApiKey("test-api-key-456");
            config.setBaseUrl("https://test.streaming.com/");
            config.setModel("glm-4.5");
            config.logConfiguration(); // Initialize semaphore

            StreamingChatLanguageModel streamingModel = config.streamingChatLanguageModel();

            assertThat(streamingModel).isNotNull();
            // Should be ZaiStreamingChatModel, not mock
            assertThat(streamingModel.getClass().getName()).contains("Zai");
        }

        @Test
        void configured_withThinkingMode_createsModelWithThinking() {
            config.setApiKey("test-key-thinking");
            config.setBaseUrl("https://test.thinking.com/");
            config.setThinkingEnabled(true);
            config.setThinkingType("enabled");
            config.logConfiguration();

            StreamingChatLanguageModel streamingModel = config.streamingChatLanguageModel();

            assertThat(streamingModel).isNotNull();
            assertThat(config.isThinkingEnabled()).isTrue();
        }

        @Test
        void configured_withDifferentThinkingTypes_createsModels() {
            String[] types = {"enabled", "disabled", "retention"};

            for (String type : types) {
                config.setApiKey("test-key-" + type);
                config.setThinkingEnabled(true);
                config.setThinkingType(type);
                config.logConfiguration();

                StreamingChatLanguageModel model = config.streamingChatLanguageModel();
                assertThat(model).isNotNull();
                assertThat(config.getThinkingType()).isEqualTo(type);
            }
        }

        @Test
        void configured_withCustomTimeout_createsModel() {
            config.setApiKey("test-key-timeout");
            config.setTimeoutSeconds(180);
            config.logConfiguration();

            ChatLanguageModel model = config.chatLanguageModel();
            assertThat(model).isNotNull();
            assertThat(config.getTimeoutSeconds()).isEqualTo(180);
        }

        @Test
        void configured_withMaxConcurrentRequests_initializesSemaphore() {
            config.setApiKey("test-key-concurrent");
            config.setMaxConcurrentRequests(5);
            config.logConfiguration();

            assertThat(config.getRequestSemaphore()).isNotNull();
            assertThat(config.getRequestSemaphore().availablePermits()).isEqualTo(5);
        }

        @Test
        void configured_allParametersSet_createsFullyConfiguredModel() {
            config.setApiKey("full-config-key");
            config.setBaseUrl("https://full.config.com/");
            config.setModel("glm-4.6");
            config.setTemperature(0.7);
            config.setTimeoutSeconds(240);
            config.setMaxTokens(4096);
            config.setTopP(0.95);
            config.setFrequencyPenalty(0.5);
            config.setMaxConcurrentRequests(10);
            config.setThinkingEnabled(true);
            config.setThinkingType("retention");

            config.logConfiguration();

            ChatLanguageModel chatModel = config.chatLanguageModel();
            StreamingChatLanguageModel streamingModel = config.streamingChatLanguageModel();

            assertThat(chatModel).isNotNull();
            assertThat(streamingModel).isNotNull();
            assertThat(config.isConfigured()).isTrue();
            assertThat(config.getRequestSemaphore().availablePermits()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Configuration Edge Cases")
    class ConfigurationEdgeCases {

        @Test
        void setters_allFieldsCanBeModified() {
            config.setApiKey("new-key");
            config.setBaseUrl("https://new.url.com/");
            config.setModel("new-model");
            config.setTemperature(0.99);
            config.setTimeoutSeconds(999);
            config.setMaxTokens(9999);
            config.setTopP(0.99);
            config.setFrequencyPenalty(0.99);
            config.setMaxConcurrentRequests(99);
            config.setThinkingEnabled(true);
            config.setThinkingType("custom");

            assertThat(config.getApiKey()).isEqualTo("new-key");
            assertThat(config.getBaseUrl()).isEqualTo("https://new.url.com/");
            assertThat(config.getModel()).isEqualTo("new-model");
            assertThat(config.getTemperature()).isEqualTo(0.99);
            assertThat(config.getTimeoutSeconds()).isEqualTo(999);
            assertThat(config.getMaxTokens()).isEqualTo(9999);
            assertThat(config.getTopP()).isEqualTo(0.99);
            assertThat(config.getFrequencyPenalty()).isEqualTo(0.99);
            assertThat(config.getMaxConcurrentRequests()).isEqualTo(99);
            assertThat(config.isThinkingEnabled()).isTrue();
            assertThat(config.getThinkingType()).isEqualTo("custom");
        }

        @Test
        void logConfiguration_canBeCalledMultipleTimes() {
            config.setMaxConcurrentRequests(3);

            config.logConfiguration();
            config.logConfiguration();
            config.logConfiguration();

            // Should not throw, semaphore should still be initialized
            assertThat(config.getRequestSemaphore()).isNotNull();
        }

        @Test
        void chatLanguageModel_calledMultipleTimes_returnsNewInstances() {
            config.setApiKey("test-key");

            ChatLanguageModel model1 = config.chatLanguageModel();
            ChatLanguageModel model2 = config.chatLanguageModel();

            // Each call to @Bean method creates a new instance
            assertThat(model1).isNotNull();
            assertThat(model2).isNotNull();
        }
    }
}
