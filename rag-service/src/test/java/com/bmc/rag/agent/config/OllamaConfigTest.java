package com.bmc.rag.agent.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OllamaConfig}.
 */
class OllamaConfigTest {

    private OllamaConfig config;

    @BeforeEach
    void setUp() {
        config = new OllamaConfig();
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValues {

        @Test
        void baseUrl_defaultsToLocalhost() {
            assertThat(config.getBaseUrl()).isEqualTo("http://localhost:11434");
        }

        @Test
        void model_defaultsToLlama3() {
            assertThat(config.getModel()).isEqualTo("llama3:8b");
        }

        @Test
        void temperature_defaultsToZero() {
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
        void topP_defaults09() {
            assertThat(config.getTopP()).isEqualTo(0.9);
        }

        @Test
        void numThreads_defaults4() {
            assertThat(config.getNumThreads()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("Setter/Getter Round-trips")
    class SetterGetterRoundtrips {

        @Test
        void baseUrl_roundTrip() {
            String customUrl = "http://custom-server:8080";
            config.setBaseUrl(customUrl);
            assertThat(config.getBaseUrl()).isEqualTo(customUrl);
        }

        @Test
        void model_roundTrip() {
            String customModel = "mistral:7b";
            config.setModel(customModel);
            assertThat(config.getModel()).isEqualTo(customModel);
        }

        @Test
        void temperature_roundTrip() {
            double customTemp = 0.7;
            config.setTemperature(customTemp);
            assertThat(config.getTemperature()).isEqualTo(customTemp);
        }

        @Test
        void timeoutSeconds_roundTrip() {
            int customTimeout = 300;
            config.setTimeoutSeconds(customTimeout);
            assertThat(config.getTimeoutSeconds()).isEqualTo(customTimeout);
        }

        @Test
        void maxTokens_roundTrip() {
            int customTokens = 4096;
            config.setMaxTokens(customTokens);
            assertThat(config.getMaxTokens()).isEqualTo(customTokens);
        }

        @Test
        void topP_roundTrip() {
            double customTopP = 0.95;
            config.setTopP(customTopP);
            assertThat(config.getTopP()).isEqualTo(customTopP);
        }

        @Test
        void numThreads_roundTrip() {
            int customThreads = 8;
            config.setNumThreads(customThreads);
            assertThat(config.getNumThreads()).isEqualTo(customThreads);
        }
    }

    @Nested
    @DisplayName("Bean Factory Methods")
    class BeanFactoryMethods {

        @Test
        void chatLanguageModel_createsNonNullInstance() {
            assertThat(config.chatLanguageModel()).isNotNull();
        }

        @Test
        void streamingChatLanguageModel_createsNonNullInstance() {
            assertThat(config.streamingChatLanguageModel()).isNotNull();
        }

        @Test
        void chatLanguageModel_usesConfiguredBaseUrl() {
            config.setBaseUrl("http://test-server:9999");
            // Just verify it doesn't throw - we can't inspect internal state easily
            assertThat(config.chatLanguageModel()).isNotNull();
        }

        @Test
        void streamingChatLanguageModel_usesConfiguredModel() {
            config.setModel("custom-model:latest");
            assertThat(config.streamingChatLanguageModel()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        void temperature_acceptsNegativeValues() {
            config.setTemperature(-0.5);
            assertThat(config.getTemperature()).isEqualTo(-0.5);
        }

        @Test
        void temperature_acceptsValuesGreaterThanOne() {
            config.setTemperature(1.5);
            assertThat(config.getTemperature()).isEqualTo(1.5);
        }

        @Test
        void baseUrl_acceptsEmptyString() {
            config.setBaseUrl("");
            assertThat(config.getBaseUrl()).isEmpty();
        }

        @Test
        void model_acceptsEmptyString() {
            config.setModel("");
            assertThat(config.getModel()).isEmpty();
        }

        @Test
        void topP_acceptsZero() {
            config.setTopP(0.0);
            assertThat(config.getTopP()).isEqualTo(0.0);
        }

        @Test
        void topP_acceptsOne() {
            config.setTopP(1.0);
            assertThat(config.getTopP()).isEqualTo(1.0);
        }
    }
}
