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
 * Unit tests for {@link GoogleAiConfig}.
 */
class GoogleAiConfigTest {

    private GoogleAiConfig config;

    @BeforeEach
    void setUp() {
        config = new GoogleAiConfig();
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValues {

        @Test
        void enabled_defaultsFalse() {
            assertThat(config.isEnabled()).isFalse();
        }

        @Test
        void apiKey_defaultsEmpty() {
            assertThat(config.getApiKey()).isEmpty();
        }

        @Test
        void model_defaultsGeminiFlash() {
            assertThat(config.getModel()).isEqualTo("gemini-1.5-flash");
        }

        @Test
        void temperature_defaults01() {
            assertThat(config.getTemperature()).isEqualTo(0.1);
        }

        @Test
        void maxTokens_defaults4096() {
            assertThat(config.getMaxTokens()).isEqualTo(4096);
        }

        @Test
        void topP_defaults08() {
            assertThat(config.getTopP()).isEqualTo(0.8);
        }

        @Test
        void maxConcurrentRequests_defaults5() {
            assertThat(config.getMaxConcurrentRequests()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("isConfigured")
    class IsConfigured {

        @Test
        void disabledWithNoKey_returnsFalse() {
            config.setEnabled(false);
            config.setApiKey("");
            assertThat(config.isConfigured()).isFalse();
        }

        @Test
        void enabledWithBlankKey_returnsFalse() {
            config.setEnabled(true);
            config.setApiKey("   ");
            assertThat(config.isConfigured()).isFalse();
        }

        @Test
        void enabledWithNullKey_returnsFalse() {
            config.setEnabled(true);
            config.setApiKey(null);
            assertThat(config.isConfigured()).isFalse();
        }

        @Test
        void enabledWithValidKey_returnsTrue() {
            config.setEnabled(true);
            config.setApiKey("valid-api-key");
            assertThat(config.isConfigured()).isTrue();
        }

        @Test
        void disabledWithValidKey_returnsFalse() {
            config.setEnabled(false);
            config.setApiKey("valid-api-key");
            assertThat(config.isConfigured()).isFalse();
        }
    }

    @Nested
    @DisplayName("isThinkingEnabled")
    class IsThinkingEnabled {

        @Test
        void alwaysReturnsFalse() {
            assertThat(config.isThinkingEnabled()).isFalse();
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
        void semaphoreMatchesMaxConcurrent() {
            config.setMaxConcurrentRequests(10);
            config.logConfiguration();
            assertThat(config.getRequestSemaphore().availablePermits()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Mock ChatLanguageModel")
    class MockChatModel {

        @Test
        void notConfigured_returnsMockModel() {
            config.setEnabled(false);
            ChatLanguageModel model = config.chatLanguageModel();
            assertThat(model).isNotNull();
        }

        @Test
        void mockModel_returnsMockResponse() {
            config.setEnabled(false);
            ChatLanguageModel model = config.chatLanguageModel();
            ChatRequest request = ChatRequest.builder()
                    .messages(List.of(UserMessage.from("test")))
                    .build();
            ChatResponse response = model.chat(request);
            assertThat(response.aiMessage().text()).contains("not configured");
        }
    }

    @Nested
    @DisplayName("Mock StreamingChatLanguageModel")
    class MockStreamingModel {

        @Test
        void notConfigured_returnsMockStreamingModel() {
            config.setEnabled(false);
            ChatLanguageModel chatModel = config.chatLanguageModel();
            StreamingChatLanguageModel streamingModel = config.streamingChatLanguageModel(chatModel);
            assertThat(streamingModel).isNotNull();
        }

        @Test
        void mockStreamingModel_sendsPartialResponses() throws InterruptedException {
            config.setEnabled(false);
            ChatLanguageModel chatModel = config.chatLanguageModel();
            StreamingChatLanguageModel streamingModel = config.streamingChatLanguageModel(chatModel);

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
            assertThat(combined).contains("not configured");
        }
    }

    @Nested
    @DisplayName("Configured Streaming Wrapper (anonymous class)")
    class ConfiguredStreamingWrapper {

        @Test
        void configuredWrapper_chunksResponseAndCallsComplete() throws InterruptedException {
            // Create a custom sync ChatModel that returns a known response
            ChatLanguageModel syncModel = new ChatLanguageModel() {
                @Override
                public ChatResponse doChat(ChatRequest req) {
                    return ChatResponse.builder()
                            .aiMessage(dev.langchain4j.data.message.AiMessage.from(
                                    "This is a test response for streaming simulation"))
                            .build();
                }
            };

            // The streamingChatLanguageModel method wraps the sync model when not configured
            // We simulate a configured state by calling the wrapping code directly
            config.setEnabled(true);
            config.setApiKey("test-key");

            // Since we cannot create a real GoogleAiGeminiChatModel without a valid API key,
            // we use the config's streamingChatLanguageModel method with a custom sync model
            // that will trigger the configured wrapping path
            StreamingChatLanguageModel streamingModel = config.streamingChatLanguageModel(syncModel);
            assertThat(streamingModel).isNotNull();

            List<String> tokens = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);
            java.util.concurrent.atomic.AtomicReference<ChatResponse> completedResponse =
                    new java.util.concurrent.atomic.AtomicReference<>();

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
                    completedResponse.set(response);
                    latch.countDown();
                }

                @Override
                public void onError(Throwable error) {
                    latch.countDown();
                }
            });

            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(tokens).isNotEmpty();
            String combined = String.join("", tokens);
            assertThat(combined).isEqualTo("This is a test response for streaming simulation");
            assertThat(completedResponse.get()).isNotNull();
        }

        @Test
        void configuredWrapper_errorInSyncModel_callsOnError() throws InterruptedException {
            // Create a sync model that throws
            ChatLanguageModel errorModel = new ChatLanguageModel() {
                @Override
                public ChatResponse doChat(ChatRequest req) {
                    throw new RuntimeException("Simulated LLM failure");
                }
            };

            config.setEnabled(true);
            config.setApiKey("test-key");
            StreamingChatLanguageModel streamingModel = config.streamingChatLanguageModel(errorModel);

            CountDownLatch latch = new CountDownLatch(1);
            java.util.concurrent.atomic.AtomicBoolean errorCalled =
                    new java.util.concurrent.atomic.AtomicBoolean(false);
            java.util.concurrent.atomic.AtomicReference<Throwable> capturedError =
                    new java.util.concurrent.atomic.AtomicReference<>();

            ChatRequest request = ChatRequest.builder()
                    .messages(List.of(UserMessage.from("test")))
                    .build();

            streamingModel.chat(request, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String token) {
                }

                @Override
                public void onCompleteResponse(ChatResponse response) {
                    latch.countDown();
                }

                @Override
                public void onError(Throwable error) {
                    errorCalled.set(true);
                    capturedError.set(error);
                    latch.countDown();
                }
            });

            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(errorCalled.get()).isTrue();
            assertThat(capturedError.get().getMessage()).contains("Simulated LLM failure");
        }
    }
}
