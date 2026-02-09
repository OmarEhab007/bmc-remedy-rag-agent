package com.bmc.rag.api.health;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LlmHealthIndicator}.
 * Tests health check reporting for the LLM chat model.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LlmHealthIndicator Tests")
class LlmHealthIndicatorTest {

    @Mock
    private ChatLanguageModel chatModel;

    @Nested
    @DisplayName("Health Check Tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return UP when chat model is configured")
        void shouldReturnUpWhenChatModelConfigured() {
            LlmHealthIndicator indicator = new LlmHealthIndicator(chatModel);

            Health health = indicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).containsEntry("provider", "configured");
        }

        @Test
        @DisplayName("Should return DOWN when chat model is null")
        void shouldReturnDownWhenChatModelNull() {
            LlmHealthIndicator indicator = new LlmHealthIndicator(null);

            Health health = indicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsKey("reason");
            assertThat(health.getDetails().get("reason")).isEqualTo("ChatLanguageModel bean is null");
        }

        @Test
        @DisplayName("Should return UP when chat model is non-null")
        void shouldReturnUpWhenChatModelNonNull() {
            LlmHealthIndicator indicator = new LlmHealthIndicator(chatModel);

            Health health = indicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).containsKey("provider");
            assertThat(health.getDetails().get("provider")).isEqualTo("configured");
        }

        @Test
        @DisplayName("Should handle multiple health check calls")
        void shouldHandleMultipleHealthCheckCalls() {
            LlmHealthIndicator indicator = new LlmHealthIndicator(chatModel);

            Health health1 = indicator.health();
            Health health2 = indicator.health();

            assertThat(health1.getStatus()).isEqualTo(Status.UP);
            assertThat(health2.getStatus()).isEqualTo(Status.UP);
            assertThat(health1.getDetails()).isEqualTo(health2.getDetails());
        }
    }

    @Nested
    @DisplayName("Health Details Tests")
    class HealthDetailsTests {

        @Test
        @DisplayName("Should include provider detail when UP")
        void shouldIncludeProviderDetailWhenUp() {
            LlmHealthIndicator indicator = new LlmHealthIndicator(chatModel);

            Health health = indicator.health();

            assertThat(health.getDetails()).hasSize(1);
            assertThat(health.getDetails()).containsEntry("provider", "configured");
        }

        @Test
        @DisplayName("Should include reason detail when DOWN due to null model")
        void shouldIncludeReasonDetailWhenDown() {
            LlmHealthIndicator indicator = new LlmHealthIndicator(null);

            Health health = indicator.health();

            assertThat(health.getDetails()).hasSize(1);
            assertThat(health.getDetails()).containsKey("reason");
        }

        @Test
        @DisplayName("Should only include provider detail when chat model configured")
        void shouldOnlyIncludeProviderDetailWhenConfigured() {
            LlmHealthIndicator indicator = new LlmHealthIndicator(chatModel);

            Health health = indicator.health();

            assertThat(health.getDetails()).hasSize(1);
            assertThat(health.getDetails()).containsEntry("provider", "configured");
        }
    }
}
